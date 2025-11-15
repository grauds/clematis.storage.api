package org.clematis.storage.controller;

import java.util.ArrayList;
import java.util.Collections;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.java.Log;
/**
 * Abstract base controller that provides REST endpoints for uploading,
 * downloading, listing, and deleting files stored by any {@link StorageService}
 * implementation (e.g. DB storage, filesystem storage, cloud storage).
 * <p>
 * Subclasses must supply:
 * - a concrete {@link StorageService} implementation via {@link #getStorageService()}
 * - the base download path used for generating public download URLs via {@link #getDownloadPath()}
 */
@Log
public abstract class AbstractStorageController {

    private static final String FILE_NOT_FOUND_MSG = "File not found, id=";

    /**
     * Builds a standard API response object describing a stored file.
     * Includes:
     * - original or logical filename
     * - public download URL
     * - content type
     * - file size
     */
    private RequestResponse createResponse(StorageEntity attachment, String contentType, long size) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getDownloadPath())
            .path(attachment.getId())
            .toUriString();
        return new RequestResponse(
            attachment.getFileName(),
            downloadUrl,
            contentType,
            size
        );
    }

    // ---------------------------------------------------------------------
    // Upload single file
    // ---------------------------------------------------------------------
    @Operation(summary = "Upload a single file",
        description = "Uploads a file and optionally stores it under the specified path.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File successfully uploaded",
            content = @Content(schema = @Schema(implementation = RequestResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
        @Parameter(description = "File to upload", required = true, in = ParameterIn.QUERY)
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "Optional storage path", in = ParameterIn.QUERY)
        @RequestParam(name = "path", required = false) String path) {
        try {
            RequestResponse response = saveAttachment(file, path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ---------------------------------------------------------------------
    // Upload batch
    // ---------------------------------------------------------------------
    @Operation(summary = "Upload multiple files",
        description = "Uploads multiple files and optionally stores them under the specified path.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Files successfully uploaded",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/upload/batch")
    public ResponseEntity<?> uploadBatch(
        @Parameter(description = "List of files to upload", in = ParameterIn.QUERY)
        @RequestParam("files") MultipartFile[] files,
        @Parameter(description = "Optional storage path", in = ParameterIn.QUERY)
        @RequestParam(name = "path", required = false) String path) {
        try {
            List<RequestResponse> responseList = new ArrayList<>();
            for (MultipartFile file : files) {
                responseList.add(saveAttachment(file, path));
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ---------------------------------------------------------------------
    // List all files
    // ---------------------------------------------------------------------
    @Operation(summary = "List all stored files", description = "Returns metadata for all files currently stored.")
    @ApiResponse(responseCode = "200", description = "List of stored files",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestResponse.class))))
    @GetMapping("/get")
    public ResponseEntity<List<RequestResponse>> getAll() {
        List<StorageEntity> storageEntities = getStorageService().getAll();

        List<RequestResponse> responses = storageEntities.stream()
            .map(storageEntity -> createResponse(storageEntity,
                    storageEntity.getContentType(),
                    storageEntity.getData() == null ? 0 : storageEntity.getData().length
                )
            ).collect(Collectors.toList());

        return ResponseEntity.ok().body(responses);
    }

    // ---------------------------------------------------------------------
    // Find by path prefix
    // ---------------------------------------------------------------------
    @Operation(summary = "Find files by path prefix",
        description = "Returns stored file metadata whose logical path starts with the provided prefix. "
            + "If path is blank or missing, returns all files.")
    @ApiResponse(responseCode = "200", description = "List of matched files",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestResponse.class))))
    @GetMapping("/find")
    public ResponseEntity<List<RequestResponse>> findByPath(
        @Parameter(description = "Path prefix to search", in = ParameterIn.QUERY)
        @RequestParam(name = "path", required = false) String path
    ) {

        List<StorageEntity> found = getStorageService().findByPath(path);

        List<RequestResponse> responses = found.stream()
            .map(se -> createResponse(se, se.getContentType(), se.getData() == null ? 0 : se.getData().length))
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ---------------------------------------------------------------------
    // Download by id
    // ---------------------------------------------------------------------
    @Operation(summary = "Download file by ID", description = "Downloads file binary content.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File found and returned"),
        @ApiResponse(responseCode = "404", description = "File not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SuppressWarnings("checkstyle:ReturnCount")
    @GetMapping(value = "/download/{id}")
    @ResponseBody
    public ResponseEntity<?> getFile(
        @Parameter(description = "ID of the file to download", required = true, in = ParameterIn.PATH)
        @PathVariable("id") String id) {
        try {
            Optional<StorageEntity> storageEntity = getStorageService().getStorageEntity(id);
            if (storageEntity.isPresent()) {
                StorageEntity se = storageEntity.get();
                return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(se.getContentType()))
                    .body(se.getData());
            } else {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(FILE_NOT_FOUND_MSG + id, HttpStatus.NOT_FOUND));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ---------------------------------------------------------------------
    // Get metadata only
    // ---------------------------------------------------------------------
    @Operation(summary = "Get metadata only",
        description = "Returns only metadata for the stored file (no binary content)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metadata returned",
                content = @Content(schema = @Schema(implementation = RequestResponse.class))
            ),
        @ApiResponse(responseCode = "404", description = "Not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/metadata/{id}")
    public ResponseEntity<?> getMetadata(
        @Parameter(description = "ID of the file", required = true, in = ParameterIn.PATH)
        @PathVariable("id") String id) {

        Optional<StorageEntity> se = getStorageService().getStorageEntity(id);
        if (se.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(FILE_NOT_FOUND_MSG + id, HttpStatus.NOT_FOUND));
        }
        StorageEntity entity = se.get();
        RequestResponse resp = createResponse(
            entity, entity.getContentType(),
            entity.getData() == null ? 0 : entity.getData().length
        );
        return ResponseEntity.ok(resp);
    }

    // ---------------------------------------------------------------------
    // Delete by id
    // ---------------------------------------------------------------------
    @Operation(summary = "Delete file by ID", description = "Deletes a file from the storage by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(
        @Parameter(description = "ID of the file to delete", required = true, in = ParameterIn.PATH)
        @PathVariable("id") String id) {
        try {
            getStorageService().deleteFile(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ---------------------------------------------------------------------
    // Delete all files
    // ---------------------------------------------------------------------
    @Operation(summary = "Delete all files", description = "Deletes all files and metadata from the storage.")
    @ApiResponse(responseCode = "204", description = "All files deleted")
    @DeleteMapping("/delete/all")
    public ResponseEntity<?> deleteAllFiles() {
        List<StorageEntity> all = getStorageService().getAll();
        for (StorageEntity se : all) {
            try {
                getStorageService().deleteFile(se.getId());
            } catch (Exception e) {
                log.warning("Failed to delete file id=" + se.getId());
            }
        }
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Internal helper
    // ---------------------------------------------------------------------
    /**
     * Saves an uploaded file through the storage service and builds the response DTO.
     */
    protected RequestResponse saveAttachment(MultipartFile file, String path) throws Exception {
        StorageEntity attachment = getStorageService().saveAttachment(file, path);
        return createResponse(attachment, file.getContentType(), file.getSize());
    }

    /**
     * Must return the storage service implementation to use.
     * Example: return fileStorageService or dbStorageService.
     */
    @Operation(hidden = true)
    public abstract StorageService getStorageService();

    /**
     * Must return the REST base path for downloading files.
     * Example: "/api/storage/download/"
     */
    @Operation(hidden = true)
    public abstract String getDownloadPath();

    /**
     * Returns a list of storage entities whose stored file names start with the given path prefix.
     * If the prefix is null or empty, all stored files are returned.
     *
     * @param path optional prefix path to filter stored files by their names
     * @return list of RequestResponse objects with download URLs
     */
    @Operation(
        summary = "Get files by path prefix",
        description = "Returns all files whose stored names start with the given prefix. "
            + "If prefix is empty or null, returns all files."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of files returned successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/getByPath")
    public ResponseEntity<List<RequestResponse>> getByPath(
        @RequestParam(name = "path", required = false) String path) {
        try {
            List<StorageEntity> storageEntities = getStorageService().findByPath(path);
            List<RequestResponse> responses = storageEntities.stream()
                .map(entity -> createResponse(entity, entity.getContentType(), entity.getData().length))
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.emptyList());
        }
    }
}
