package org.clematis.storage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.service.StorageService;
import org.clematis.storage.web.ErrorResponse;
import org.clematis.storage.web.RequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.extern.java.Log;

/**
 * Abstract storage controller is
 */
@Log
public abstract class AbstractStorageController {

    private RequestResponse createResponse(StorageEntity attachment, String file, long size) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getDownloadPath())
            .path(attachment.getId())
            .toUriString();
        return new RequestResponse(
            attachment.getFileName(),
            downloadUrl,
            file,
            size
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(name = "path", required = false) String path) {
        try {
            RequestResponse response = saveAttachment(file, path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<?> upload(@RequestParam("files") MultipartFile[] files,
                                          @RequestParam(name = "path", required = false) String path) {
        try {
            List<RequestResponse> responseList = new ArrayList<>();
            for (MultipartFile file : files) {
                RequestResponse response = saveAttachment(file, path);
                responseList.add(response);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/get")
    public ResponseEntity<List<RequestResponse>> getAllFiles() {
        List<StorageEntity> storageEntities = getStorageService().getAllFiles();

        List<RequestResponse> responses = storageEntities.stream()
            .map(storageEntity -> createResponse(storageEntity,
                    storageEntity.getContentType(),
                    storageEntity.getData().length
                )
            ).collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    @GetMapping(value = "/download/{id}")
    @ResponseBody
    public ResponseEntity<?> getFile(@PathVariable("id") String id) {
        try {
            Optional<StorageEntity> storageEntity = getStorageService().getStorageEntity(id);
            if (storageEntity.isPresent()) {
                return ResponseEntity
                    .ok()
                    .contentType(MediaType.valueOf(storageEntity.get().getContentType()))
                    .body(storageEntity.get().getData());
            } else {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("File not found, id=" + id, HttpStatus.NOT_FOUND));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @DeleteMapping(value = "/{id}")
    public void deleteFile(@PathVariable("id") String id) throws IllegalArgumentException {
        getStorageService().deleteFile(id);
    }

    protected RequestResponse saveAttachment(MultipartFile file, String path) throws Exception {
        StorageEntity attachment = getStorageService().saveAttachment(file, path);
        return createResponse(attachment, file.getContentType(), file.getSize());
    }

    public abstract StorageService getStorageService();

    public abstract String getDownloadPath();
}
