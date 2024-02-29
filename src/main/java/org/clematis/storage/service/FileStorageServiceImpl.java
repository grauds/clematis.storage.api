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
 * Service which uses file system to store the uploaded resources with their binary contents.
 */
@Service
@Log
public class FileStorageServiceImpl implements StorageService {

    private static final String COULDN_T_MAKE_DIRECTORY_ON_THE_SERVER = "Couldn't make directory on the server: ";

    private final StorageEntityRepository storageEntityRepository;

    @Value("${clematis.storage.download.folder}")
    private String downloadFolder;

    public FileStorageServiceImpl(StorageEntityRepository storageEntityRepository) {
        this.storageEntityRepository = storageEntityRepository;
    }

    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public StorageEntity saveAttachment(MultipartFile file, String path) throws IOException {

        if (file == null) {
            throw new IOException("Multipath file is null");
        }

        String fileName;
        if (file.getOriginalFilename() != null) {
            fileName = file.getOriginalFilename();
        } else {
            fileName = UUID.randomUUID().toString();
        }

        // path relative to download folder some/path/file_name
        String additionalPath = (path != null && !path.isEmpty()) ? StringUtils.cleanPath(path) : "";
        if (additionalPath.contains("..")) {
            throw new IOException("Filename contains invalid path sequence " + additionalPath);
        }

        // absolute file name for the download folder plus additional path
        File destinationFolder = !additionalPath.isEmpty()
            ? new File(this.downloadFolder, additionalPath)
            : new File(this.downloadFolder);
        // absolute folder plus destination file
        File destination = new File(destinationFolder, fileName).getAbsoluteFile();
        // get temp files data before it is moved and pointer to multipart is lost
        String contentType = file.getContentType();

        if ((!destinationFolder.exists() && destinationFolder.mkdirs()) || destinationFolder.exists()) {
            // moving the file, losing multipart meta information here
            file.transferTo(destination);
        } else {
            log.log(Level.SEVERE, COULDN_T_MAKE_DIRECTORY_ON_THE_SERVER
                + destinationFolder.getAbsolutePath());
            throw new IOException(COULDN_T_MAKE_DIRECTORY_ON_THE_SERVER
                + destinationFolder.getAbsolutePath());
        }

        StorageEntity attachment = new StorageEntity(
            Paths.get(additionalPath, fileName).toString(), contentType, new byte[0]
        );
        return storageEntityRepository.save(attachment);
    }

    @Override
    public List<StorageEntity> saveAttachments(MultipartFile[] files, String path) {
        return Arrays.stream(files).map(file -> {
            try {
                return saveAttachment(file, path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @Override
    public List<StorageEntity> getAllFiles() {
        return null;
    }

    /**
     * This method relies on file type probes to get MIME content types for uploaded files.
     *
     * @param id of the storage entity to load
     * @return storage entity with file bytes, content type and id from the initial arguments for the method
     */
    @Override
    public StorageEntity getStorageEntity(String id) {
        try {
            Optional<StorageEntity> storageEntity = storageEntityRepository.findById(id);
            Path path = Path.of(downloadFolder, storageEntity.orElseThrow().getFileName());
            MediaType contentType = ensureMediaType(storageEntity, path);
            return new StorageEntity(id, contentType.toString(), Files.readAllBytes(path));
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
            return new StorageEntity(new byte[0]);
        }
    }

    private MediaType ensureMediaType(Optional<StorageEntity> storageEntity, Path path) {

        MediaType mediaType = null;

        try {
            mediaType = MediaType.valueOf(storageEntity.orElseThrow().getContentType());
        } catch (Exception e) {
            log.warning("Media type can't parse the " + storageEntity.orElseThrow().getContentType());
        }

        if (mediaType == null) {
            try {
                String contentType = Files.probeContentType(path);
                if (contentType != null) {
                    mediaType = MediaType.valueOf(contentType);
                }
            } catch (Exception e) {
                log.warning("Fail to probe media type of " + path);
            }
        }

        return mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    }

    @Override
    public void deleteFile(String id) {

    }
}
