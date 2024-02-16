package org.clematis.storage.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.clematis.storage.ApplicationTests;
import org.clematis.storage.web.RequestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class StorageControllerTests extends ApplicationTests {

    public static final String HELLO_WORLD = "Hello, world!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    public static Resource mockMultipartFile() throws IOException {
        Path testFile = Files.createTempFile("test", ".txt");
        Files.write(testFile, HELLO_WORLD.getBytes());
        return new FileSystemResource(testFile.toFile());
    }

    @Test
    public void testFileDownloadDatabase() throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", mockMultipartFile());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestResponse> responseEntity = testRestTemplate
            .postForEntity("/api/files/single/base", requestEntity, RequestResponse.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(responseEntity.getBody());

        ResponseEntity<byte[]> file
            = testRestTemplate.getForEntity(responseEntity.getBody().getDownloadUrl(), byte[].class);
        Assertions.assertNotNull(file.getBody());
        Assertions.assertEquals(HELLO_WORLD, new String(file.getBody()));
    }

    @Test
    public void testFileDownloadFilesystem() throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", mockMultipartFile());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestResponse> responseEntity = testRestTemplate
            .postForEntity("/api/files/single/file", requestEntity, RequestResponse.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(responseEntity.getBody());

        ResponseEntity<byte[]> file
            = testRestTemplate.getForEntity(responseEntity.getBody().getDownloadUrl(), byte[].class);
        Assertions.assertNotNull(file.getBody());
        Assertions.assertEquals(HELLO_WORLD, new String(file.getBody()));
    }

}
