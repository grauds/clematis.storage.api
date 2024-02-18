package org.clematis.storage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.service.StorageService;
import org.clematis.storage.web.RequestResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.java.Log;

/**
 * Spring Web MVC controller to expose endpoints to upload file to filesystem storage
 */
@RestController
@RequestMapping("/api/files")
@SuppressFBWarnings("EI_EXPOSE_REP")
@Log
public class FileStorageController {

    public static final String DOWNLOAD_PATH = "/api/files/download/";

    private final StorageService storageService;

    public FileStorageController(@Qualifier("fileStorageServiceImpl") StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<RequestResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            RequestResponse response = saveAttachment(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<List<RequestResponse>> upload(@RequestParam("files") MultipartFile[] files) {
        try {
            List<RequestResponse> responseList = new ArrayList<>();
            for (MultipartFile file : files) {
                RequestResponse response = saveAttachment(file);
                responseList.add(response);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/get")
    public ResponseEntity<List<RequestResponse>> getAllFiles() {
        List<StorageEntity> storageEntities = storageService.getAllFiles();

        List<RequestResponse> responses = storageEntities.stream()
            .map(storageEntity -> createResponse(storageEntity,
                    storageEntity.getFileType(),
                    storageEntity.getData().length
                )
            ).collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    @GetMapping(value = "/download/{id}")
    @ResponseBody
    public byte[] getFile(@PathVariable("id") String id) {
        return storageService.getFile(id);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteFile(@PathVariable("id") String id) throws IllegalArgumentException {
        storageService.deleteFile(id);
    }

    private RequestResponse saveAttachment(MultipartFile file) throws Exception {
        StorageEntity attachment = storageService.saveAttachment(file);
        return createResponse(attachment, file.getContentType(), file.getSize());
    }

    private static RequestResponse createResponse(StorageEntity attachment, String file, long size) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(DOWNLOAD_PATH)
            .path(attachment.getId())
            .toUriString();
        return new RequestResponse(attachment.getFileName(), downloadUrl, file, size);
    }
}
