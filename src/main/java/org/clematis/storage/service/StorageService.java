package org.clematis.storage.service;

import java.util.List;

import org.clematis.storage.model.StorageEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Specification of storage service to be extended with any other storage system interface.
 */
public interface StorageService {

    StorageEntity saveAttachment(MultipartFile file, String path) throws Exception;

    List<StorageEntity> saveAttachments(MultipartFile[] files, String path) throws Exception;

    List<StorageEntity> getAllFiles();

    StorageEntity getStorageEntity(String id);

    void deleteFile(String id);
}
