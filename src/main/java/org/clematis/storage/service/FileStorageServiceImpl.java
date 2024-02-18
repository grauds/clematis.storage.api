package org.clematis.storage.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.clematis.storage.model.StorageEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.java.Log;

/**
 * Service which uses file system to store the uploaded resources with their binary contents.
 */
@Service
@Log
public class FileStorageServiceImpl implements StorageService {

    @Value("${clematis.storage.download.folder}")
    private String downloadFolder;

    @Override
    public StorageEntity saveAttachment(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        File downloadFolderFile = new File(downloadFolder);
        if ((!downloadFolderFile.exists() && downloadFolderFile.mkdirs()) || downloadFolderFile.exists()) {
            file.transferTo(new File(downloadFolder + File.separator + fileName).getAbsoluteFile());
        } else {
            log.log(Level.SEVERE, "Couldn't make directory on the server: "
                + downloadFolderFile.getAbsolutePath());
        }
        return new StorageEntity(file.getName(), file.getContentType(), file.getBytes());
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
        return null;
    }

    @Override
    public byte[] getFile(String id) {
        try {
            return Files.readAllBytes(Path.of(downloadFolder, id));
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public void deleteFile(String id) {

    }
}
