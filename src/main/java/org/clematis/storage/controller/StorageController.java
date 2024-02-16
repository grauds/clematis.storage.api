package org.clematis.storage.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.service.StorageService;
import org.clematis.storage.web.RequestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.extern.java.Log;

@RestController
@RequestMapping("/api/files")
@Log
public class StorageController {

    public static final String DOWNLOAD_PATH = "/api/files/download/";

    @Value("${clematis.storage.download.folder}")
    private String downloadFolder;

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/single/base")
    public ResponseEntity<RequestResponse> uploadFile(@RequestParam("file") MultipartFile file) {

        try {
            StorageEntity attachment = storageService.saveAttachment(file);
            String downloadURl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DOWNLOAD_PATH)
                .path(attachment.getId())
                .toUriString();

            return ResponseEntity.ok(new RequestResponse(attachment.getFileName(),
                downloadURl,
                file.getContentType(),
                file.getSize()));

        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/multiple/base")
    public ResponseEntity<List<RequestResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {

        try {
            List<RequestResponse> responseList = new ArrayList<>();
            for (MultipartFile file : files) {
                StorageEntity attachment = storageService.saveAttachment(file);
                String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(DOWNLOAD_PATH)
                    .path(attachment.getId())
                    .toUriString();
                RequestResponse response = new RequestResponse(attachment.getFileName(),
                    downloadUrl,
                    file.getContentType(),
                    file.getSize());
                responseList.add(response);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<RequestResponse>> getAllFiles() {
        List<StorageEntity> storageEntities = storageService.getAllFiles();

        List<RequestResponse> responses = storageEntities.stream().map(storageEntity -> {
            String downloadURL = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DOWNLOAD_PATH)
                .path(storageEntity.getId())
                .toUriString();
            return new RequestResponse(storageEntity.getFileName(),
                downloadURL,
                storageEntity.getFileType(),
                storageEntity.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    @GetMapping(value = "/download/{id}")
    @ResponseBody
    public byte[] getFile(@PathVariable("id") String id) throws IOException {
        try {
            return storageService.getFile(UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            return Files.readAllBytes(Path.of(downloadFolder, id));
        }
    }

    @PostMapping("/single/file")
    public ResponseEntity<RequestResponse> handleFileUpload(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            File downloadFolderFile = new File(downloadFolder);
            if (!downloadFolderFile.exists()) {
                downloadFolderFile.mkdirs();
            }
            file.transferTo(new File(downloadFolder + File.separator + fileName).getAbsoluteFile());
            assert fileName != null;
            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DOWNLOAD_PATH)
                .path(fileName)
                .toUriString();
            RequestResponse response = new RequestResponse(fileName,
                downloadUrl,
                file.getContentType(),
                file.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/multiple/file")
    public ResponseEntity<List<RequestResponse>> handleMultipleFilesUpload(
        @RequestParam("files") MultipartFile[] files
    ) {
        List<RequestResponse> responseList = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            try {
                file.transferTo(new File(downloadFolder + File.separator + fileName).getAbsoluteFile());
                assert fileName != null;
                String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(DOWNLOAD_PATH)
                    .path(fileName)
                    .toUriString();
                RequestResponse response = new RequestResponse(fileName,
                    downloadUrl,
                    file.getContentType(),
                    file.getSize());
                responseList.add(response);
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.ok(responseList);
    }
}
