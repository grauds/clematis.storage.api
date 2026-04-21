package org.clematis.storage.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.repository.StorageEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.java.Log;

/**
 * Storage service implementation that persists uploaded files in a database.
 * <p>
 * File contents are stored directly in the {@link StorageEntity} table as BLOBs,
 * while the logical path (folder + filename) is stored in the entity's path field.
 */
@Service
@Log
public class DbStorageServiceImpl implements StorageService {

    private final StorageEntityRepository storageEntityRepository;

    @Value("${clematis.storage.max_file_size:104857600}")
    private long maxFileSize;

    public DbStorageServiceImpl(StorageEntityRepository storageEntityRepository) {
        this.storageEntityRepository = storageEntityRepository;
    }

    /**
     * Saves a single uploaded file into the database.
     * Validates file name, checks size limits, normalizes path separators,
     * and stores binary contents directly.
     */
    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public StorageEntity saveAttachment(MultipartFile file, String path) throws IOException {

        log.info("Saving file to " + path);
        // Clean the filename to avoid path traversal attempts
        String fileName = StringUtils.cleanPath(
            Objects.requireNonNull(file.getOriginalFilename(), "Original filename is null")
        );

        // Build the logical path: "some/path/file.ext" or just "file.ext"
        String additionalPath = (path != null && !path.isEmpty()) ? path : "";
        log.info("Normalized path: " + additionalPath);

        String destination = additionalPath.isEmpty()
            ? fileName
            : additionalPath + File.separator + fileName;
        log.info("Destination: " + destination);

        // Security: forbid directory change in names
        if (fileName.contains("..")) {
            throw new IOException("Filename contains invalid path sequence: " + fileName);
        }

        // Validate size
        if (file.getSize() > maxFileSize) {
            log.severe("File size exceeds max allowed: " + file.getSize() + " > " + maxFileSize);
            throw new MaxUploadSizeExceededException(file.getSize());
        }

        // Create an entity with the logical path + type + bytes
        StorageEntity attachment = new StorageEntity(
            destination,
            file.getContentType(),
            file.getBytes()
        );
        log.info("Metadata saved: " + attachment);
        // Persist to DB
        return storageEntityRepository.save(attachment);
    }

    /**
     * Saves all uploaded files and returns a list of stored entities.
     * If any file fails, a RuntimeException is thrown.
     */
    @Override
    public List<StorageEntity> saveAttachments(MultipartFile[] files, String path) {
        log.info("Saving " + files.length + " files to " + path);
        return Arrays.stream(files).map(file -> {
            try {
                return saveAttachment(file, path);
            } catch (Exception e) {
                log.log(java.util.logging.Level.SEVERE, e.getMessage());
                throw new RuntimeException("Failed to save attachment: " + file.getOriginalFilename(), e);
            }
        }).toList();
    }

    /**
     * Returns all stored files from the database.
     */
    @Override
    public List<StorageEntity> getAll() {
        return storageEntityRepository.findAll();
    }

    /**
     * Loads a single stored file by ID.
     */
    @Override
    public Optional<StorageEntity> getStorageEntity(String id) {
        return storageEntityRepository.findById(id);
    }

    /**
     * Deletes a stored file by ID.
     */
    @Override
    public void deleteFile(String id) {
        storageEntityRepository.deleteById(id);
    }

    /**
     * Searches stored files by a prefix in their normalized path.
     * <p>
     * Both backslashes '\' and forward slashes '/' are normalized to '/'.
     * Database values are also normalized before comparison.
     *
     * @param path prefix to match; if null/blank, returns all files
     * @return filtered list of storage entities
     */
    @Override
    public List<StorageEntity> findByPath(String path) {
        log.info("Searching for files by path: " + path);
        // If no filter provided, return everything
        if (path == null || path.isBlank()) {
            return storageEntityRepository.findAll();
        }

        // Normalize prefix
        String normalizedPrefix = path.replace('\\', '/');
        log.info("Normalized path: " + normalizedPrefix);

        return storageEntityRepository.findAll().stream()
            .filter(entity -> {
                // Normalize the stored path
                String storedPath = entity.getFileName().replace('\\', '/');
                return storedPath.startsWith(normalizedPrefix);
            })
            .toList();
    }
}
