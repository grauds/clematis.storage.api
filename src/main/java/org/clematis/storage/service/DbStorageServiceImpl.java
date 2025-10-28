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

/**
 * Service which uses a database to store the uploaded resources with their binary contents.
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
    public StorageEntity saveAttachment(MultipartFile file, String path) throws IOException {

        // store a file with a trimmed path
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // path relative to download folder some/path/file_name
        String additionalPath = (path != null && !path.isEmpty()) ? path : "";
        // destination additional path plus file
        String destination = !additionalPath.isEmpty()
            ? additionalPath + File.separator + fileName
            : fileName;

        if (fileName.contains("..")) {
            throw new IOException("Filename contains invalid path sequence " + fileName);
        }
        if (file.getBytes().length > maxFileSize) {
            throw new MaxUploadSizeExceededException(file.getSize());
        }
        StorageEntity attachment = new StorageEntity(
            destination, file.getContentType(), file.getBytes()
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
        return storageEntityRepository.findAll();
    }

    @Override
    public Optional<StorageEntity> getStorageEntity(String id) {
        return storageEntityRepository.findById(id);
    }

    @Override
    public void deleteFile(String id) {
        storageEntityRepository.deleteById(id);
    }
}
