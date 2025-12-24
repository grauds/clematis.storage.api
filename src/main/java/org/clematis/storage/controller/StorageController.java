package org.clematis.storage.controller;

import org.clematis.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Spring Web MVC controller to expose endpoints to upload the file to database storage
 */
@Getter
@RestController
@RequestMapping("/api/db")
@SuppressFBWarnings("EI_EXPOSE_REP")
@Log
public class StorageController extends AbstractStorageController {

    private final StorageService storageService;

    public StorageController(@Qualifier("dbStorageServiceImpl") StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public StorageService getStorageService() {
        return storageService;
    }

    @Override
    public String getDownloadPath() {
        return "/api/db/";
    }
}
