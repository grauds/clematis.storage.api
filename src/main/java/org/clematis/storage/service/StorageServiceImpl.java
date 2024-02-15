package org.clematis.storage.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.repository.StorageEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

    private final StorageEntityRepository storageEntityRepository;

    public StorageServiceImpl(StorageEntityRepository storageEntityRepository) {
        this.storageEntityRepository = storageEntityRepository;
    }

    @Override
    public StorageEntity saveAttachment(MultipartFile file) throws Exception {

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {

            if(fileName.contains("..")) {
                throw  new Exception("Filename contains invalid path sequence " + fileName);
            }
            if (file.getBytes().length > (1024 * 1024)) {
                throw new Exception("File size exceeds maximum limit");
            }
            StorageEntity attachment = new StorageEntity(fileName, file.getContentType(), file.getBytes());
            return storageEntityRepository.save(attachment);

        } catch (MaxUploadSizeExceededException e) {
            throw new MaxUploadSizeExceededException(file.getSize());
        } catch (Exception e) {
            throw new Exception("Could not save File: " + fileName);
        }
    }

    @Override
    public void saveFiles(MultipartFile[] files) {
        Arrays.asList(files).forEach(file -> {
            try {
                saveAttachment(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<StorageEntity> getAllFiles() {
        return storageEntityRepository.findAll();
    }
}
