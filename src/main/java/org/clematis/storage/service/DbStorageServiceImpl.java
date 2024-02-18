package org.clematis.storage.service;

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

/**
 * Service which uses database to store the uploaded resources with their binary contents.
 */
@Service
public class DbStorageServiceImpl implements StorageService {

    private final StorageEntityRepository storageEntityRepository;

    @Value("${clematis.storage.max_file_size}")
    private final int maxFileSize = 1024 * 1024;

    public DbStorageServiceImpl(StorageEntityRepository storageEntityRepository) {
        this.storageEntityRepository = storageEntityRepository;
    }

    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public StorageEntity saveAttachment(MultipartFile file) throws Exception {

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {

            if (fileName.contains("..")) {
                throw new Exception("Filename contains invalid path sequence " + fileName);
            }
            if (file.getBytes().length > maxFileSize) {
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

    @Override
    public byte[] getFile(String id) {
        Optional<StorageEntity> storageEntity = storageEntityRepository.findById(id);
        return storageEntity.isPresent() ? storageEntity.get().getData() : new byte[0];
    }

    @Override
    public void deleteFile(String id) {
        storageEntityRepository.deleteById(id);
    }
}
