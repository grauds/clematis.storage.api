package org.clematis.storage.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class StorageControllerTests extends ApplicationTests {

    public static final String HELLO_WORLD = "Hello, world!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    public static Resource mockMultipartFile() throws IOException {
        Path testFile = Files.createTempFile("test", ".txt");
        Files.writeString(testFile, HELLO_WORLD);
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
            .postForEntity("/api/db/upload", requestEntity, RequestResponse.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(responseEntity.getBody());

        ResponseEntity<byte[]> file
            = testRestTemplate.getForEntity(responseEntity.getBody().getDownloadUrl(), byte[].class);
        Assertions.assertNotNull(file.getBody());
        Assertions.assertEquals(HELLO_WORLD, new String(file.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFileDownloadFilesystem() throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", mockMultipartFile());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestResponse> responseEntity = testRestTemplate
            .postForEntity("/api/files/upload", requestEntity, RequestResponse.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(responseEntity.getBody());

        ResponseEntity<byte[]> file
            = testRestTemplate.getForEntity(responseEntity.getBody().getDownloadUrl(), byte[].class);
        Assertions.assertNotNull(file.getBody());
        Assertions.assertEquals(HELLO_WORLD, new String(file.getBody(), StandardCharsets.UTF_8));
    }

}
