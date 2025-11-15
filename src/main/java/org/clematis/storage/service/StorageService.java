package org.clematis.storage.service;

import java.util.List;
import java.util.Optional;

import org.clematis.storage.model.StorageEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Specification of a storage service capable of saving, retrieving,
 * searching, and deleting stored binary resources (files).
 * <p>
 * Implementations may store files in a database, filesystem, cloud storage, etc.
 */
public interface StorageService {

    /**
     * Stores a single uploaded file under the given logical path.
     *
     * @param file the uploaded multipart file; must not be null
     * @param path optional subpath (folder-like prefix) inside the storage;
     *             when null or blank, the file is stored at the root level
     * @return the created {@link StorageEntity} describing the stored binary file
     * @throws Exception if storing fails due to I/O problems, invalid file name,
     *                   storage limits, or other implementation-specific issues
     */
    StorageEntity saveAttachment(MultipartFile file, String path) throws Exception;

    /**
     * Stores multiple uploaded files under the given logical path.
     * Implementations may choose to store them in a batch or serialize the operations.
     *
     * @param files array of multipart files; may be empty but must not be null
     * @param path optional subpath (folder-like prefix)
     * @return list of successfully saved {@link StorageEntity} objects
     */
    List<StorageEntity> saveAttachments(MultipartFile[] files, String path) throws Exception;

    /**
     * Finds stored files whose normalized path starts with the given prefix.
     * Both forward (/) and backward (\) slashes are treated as equivalent separators.
     * <p>
     * When {@code path} is null or blank, all files MUST be returned.
     *
     * @param path prefix of stored paths (case-sensitive), or null/blank for all files
     * @return list of matching stored entities; never null
     */
    List<StorageEntity> findByPath(String path);

    /**
     * Returns all stored entities.
     *
     * @return list of all objects; never null
     */
    List<StorageEntity> getAll();

    /**
     * Looks up a stored entity by its unique identifier.
     *
     * @param id database or storage ID of the file
     * @return optional containing the entity or empty if not found
     */
    Optional<StorageEntity> getStorageEntity(String id);

    /**
     * Deletes a stored file by its identifier.
     *
     * @param id identifier of the file to delete; if the file does not exist,
     *           behavior is implementation-specific (usually no-op)
     */
    void deleteFile(String id);
}
