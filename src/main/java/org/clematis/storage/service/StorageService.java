package org.clematis.storage.service;

import java.util.List;

import org.clematis.storage.model.StorageEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StorageEntity saveAttachment(MultipartFile file) throws Exception;

    void saveFiles(MultipartFile[] files) throws Exception;

    List<StorageEntity> getAllFiles();
}
