package org.clematis.storage.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.repository.StorageEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.java.Log;
/**
 * Implementation of {@link StorageService} that stores uploaded files directly
 * on the server's filesystem. Only metadata (logical path, MIME type) is stored
 * in the database through {@link StorageEntityRepository}. Actual file bytes
 * are stored under the configured download folder.
 * <p>
 * On retrieval, files are re-read from the filesystem and returned as
 * {@link StorageEntity} objects with full binary content.
 */
@Service
@Log
public class FileStorageServiceImpl implements StorageService {

    private static final String MAKE_DIR_ERROR_MESSAGE = "Couldn't make directory on the server: ";

    private final StorageEntityRepository storageEntityRepository;

    @Value("${clematis.storage.download.folder}")
    private String downloadFolder;

    public FileStorageServiceImpl(StorageEntityRepository storageEntityRepository) {
        this.storageEntityRepository = storageEntityRepository;
    }

    /**
     * Saves an uploaded file to the filesystem. The stored file path is composed of:
     * <ul>
     *   <li>the configured download directory</li>
     *   <li>optional subpath (normalized)</li>
     *   <li>the original filename or a generated fallback</li>
     * </ul>
     *
     * Only metadata (file path, MIME type) is persisted to the database.
     */
    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public StorageEntity saveAttachment(MultipartFile file, String path) throws IOException {
        log.info("Saving file to " + path);
        if (file == null) {
            throw new IOException("Multipart file is null");
        }

        // If no original filename -> generate the random UUID name
        String fileName = file.getOriginalFilename() != null
            ? file.getOriginalFilename()
            : UUID.randomUUID().toString();
        log.info("Generated filename: " + fileName);

        // Normalize the additional path, prevent path traversal
        String additionalPath = (path != null && !path.isEmpty())
            ? StringUtils.cleanPath(path)
            : "";
        log.info("Normalized path: " + additionalPath);

        if (additionalPath.contains("..")) {
            throw new IOException("Filename contains invalid path sequence " + additionalPath);
        }

        // Build the folder where the file will be stored
        File destinationFolder = additionalPath.isEmpty()
            ? new File(this.downloadFolder)
            : new File(this.downloadFolder, additionalPath);
        log.info("Destination folder: " + destinationFolder.getAbsolutePath());

        // Build absolute destination file
        File destination = new File(destinationFolder, fileName).getAbsoluteFile();

        // Extract the content type before moving the file
        String contentType = file.getContentType();

        // Ensure folder exists
        if ((!destinationFolder.exists() && destinationFolder.mkdirs()) || destinationFolder.exists()) {
            // Moves the uploaded file to destination (MultipartFile loses its buffer afterward)
            file.transferTo(destination);
        } else {
            log.info("Couldn't create directory: " + destinationFolder.getAbsolutePath());
            log.log(Level.SEVERE, MAKE_DIR_ERROR_MESSAGE + destinationFolder.getAbsolutePath());
            throw new IOException(MAKE_DIR_ERROR_MESSAGE + destinationFolder.getAbsolutePath());
        }

        // Store metadata only; contents stored on filesystem
        log.info("File saved to: " + destination.getAbsolutePath());
        StorageEntity attachment = new StorageEntity(
            Paths.get(additionalPath, fileName).toString(),
            contentType,
            new byte[0] // binary content is never stored here
        );
        attachment.setSize(destination.length());
        log.info("Metadata saved: " + attachment);
        return storageEntityRepository.save(attachment);
    }

    /**
     * Saves multiple uploaded files to the filesystem.
     * If any file fails, a RuntimeException is thrown.
     */
    @Override
    public List<StorageEntity> saveAttachments(MultipartFile[] files, String path) {
        log.info("Saving " + files.length + " files to " + path);
        return Arrays.stream(files).map(file -> {
            try {
                return saveAttachment(file, path);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage());
                throw new RuntimeException(e);
            }
        }).toList();
    }

    /**
     * Returns metadata for all stored files.
     * (Does not load binary content.)
     */
    @Override
    public List<StorageEntity> getAll() {
        return storageEntityRepository.findAll();
    }

    /**
     * Loads a stored file by ID. The metadata is read from the database,
     * and the binary content is loaded from disk.
     * <p>
     * MIME type is validated via:
     * <ol>
     *   <li>database-stored content type</li>
     *   <li>filesystem probing using {@link Files#probeContentType(Path)}</li>
     *   <li>fallback to application/octet-stream</li>
     * </ol>
     */
    @SuppressWarnings("checkstyle:ReturnCount")
    @Override
    public Optional<StorageEntity> getStorageEntity(String id) {
        log.info("Loading file with ID: " + id);
        try {
            Optional<StorageEntity> storageEntity = storageEntityRepository.findById(id);
            if (storageEntity.isPresent()) {

                // Build filesystem path
                Path path = Path.of(downloadFolder, storageEntity.orElseThrow().getFileName());
                log.info("Loading file from disk: " + path);
                // Determine media type via stored metadata or probe
                MediaType contentType = ensureMediaType(storageEntity, path);
                // Load file bytes from disk
                byte[] bytes = Files.readAllBytes(path);
                log.info("Loaded file with size: " + bytes.length);
                return Optional.of(new StorageEntity(id, contentType.toString(), bytes));
            }
            log.info("File not found");
            return Optional.empty();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load file from disk: " + id);
            log.log(Level.SEVERE, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Attempts to determine the correct MIME type based on:
     * 1. Database metadata
     * 2. Filesystem sniffing
     * 3. Fallback to octet-stream
     */
    private MediaType ensureMediaType(Optional<StorageEntity> storageEntity, Path path) {

        MediaType mediaType = null;
        log.info("Determining media type for " + path);
        // Try stored content type
        try {
            mediaType = MediaType.valueOf(storageEntity.orElseThrow().getContentType());
        } catch (Exception e) {
            log.warning("Media type can't parse the " + storageEntity.orElseThrow().getContentType());
        }

        // Try probing from the disk
        if (mediaType == null) {
            try {
                String probed = Files.probeContentType(path);
                if (probed != null) {
                    mediaType = MediaType.valueOf(probed);
                }
            } catch (Exception e) {
                log.warning("Failed to probe media type of " + path);
            }
        }
        log.info("Determined media type: " + mediaType + " for " + path);
        // Fallback
        return mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * Deletes a file from both the filesystem and metadata from the database.
     */
    @Override
    public void deleteFile(String id) {
        log.info("Deleting file with ID: " + id);
        storageEntityRepository.findById(id).ifPresent(entity -> {
            Path path = Path.of(downloadFolder, entity.getFileName());
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warning("Unable to delete file from filesystem: " + path);
            }
            storageEntityRepository.deleteById(id);
        });
    }

    /**
     * Finds stored files whose normalized path starts with the given prefix.
     * Normalization converts '\' to '/' for both stored and input paths.
     *
     * @param path prefix to search for; null/blank returns all
     */
    @Override
    public List<StorageEntity> findByPath(String path) {
        log.info("Searching for files by path: " + path);
        if (path == null || path.isBlank()) {
            log.info("Loading all files...");
            return storageEntityRepository.findAll();
        }

        String normalizedPrefix = path.replace('\\', '/');
        log.info("Loading files from path: " + normalizedPrefix);

        return storageEntityRepository.findAll().stream()
            .filter(entity -> {
                String storedPath = entity.getFileName().replace('\\', '/');
                return storedPath.startsWith(normalizedPrefix);
            })
            .toList();
    }
}
