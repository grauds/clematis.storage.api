package org.clematis.storage.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.service.StorageService;
import org.clematis.storage.web.RequestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/files")
public class StorageController {

    @Value("${clematis.storage.download.folder}")
    private String downloadFolder;

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/single/base")
    public RequestResponse uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        StorageEntity attachment = storageService.saveAttachment(file);
        String downloadURl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/download/")
            .path(attachment.getId())
            .toUriString();

        return new RequestResponse(attachment.getFileName(),
            downloadURl,
            file.getContentType(),
            file.getSize());
    }

    @PostMapping("/multiple/base")
    public List<RequestResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) throws Exception {
        List<RequestResponse> responseList = new ArrayList<>();
        for (MultipartFile file : files) {
            StorageEntity attachment = storageService.saveAttachment(file);
            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(attachment.getId())
                .toUriString();
            RequestResponse response = new RequestResponse(attachment.getFileName(),
                downloadUrl,
                file.getContentType(),
                file.getSize());
            responseList.add(response);
        }
        return responseList;
    }

    @GetMapping("/all")
    public ResponseEntity<List<RequestResponse>> getAllFiles() {
        List<StorageEntity> storageEntities = storageService.getAllFiles();

        List<RequestResponse> responses = storageEntities.stream().map(storageEntity -> {
            String downloadURL = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(storageEntity.getId())
                .toUriString();
            return new RequestResponse(storageEntity.getFileName(),
                downloadURL,
                storageEntity.getFileType(),
                storageEntity.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    @PostMapping("/single/file")
    public ResponseEntity<RequestResponse> handleFileUpload(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            file.transferTo(new File(downloadFolder + fileName));
            assert fileName != null;
            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(fileName)
                .toUriString();
            RequestResponse response = new RequestResponse(fileName,
                downloadUrl,
                file.getContentType(),
                file.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
                file.transferTo(new File(downloadFolder + fileName));
                assert fileName != null;
                String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/download/")
                    .path(fileName)
                    .toUriString();
                RequestResponse response = new RequestResponse(fileName,
                    downloadUrl,
                    file.getContentType(),
                    file.getSize());
                responseList.add(response);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.ok(responseList);
    }
}
