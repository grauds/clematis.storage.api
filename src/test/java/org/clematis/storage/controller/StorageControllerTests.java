package org.clematis.storage.controller;

import static io.restassured.RestAssured.given;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.clematis.storage.ApplicationTests;
import org.clematis.storage.web.RequestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class StorageControllerTests extends ApplicationTests {

    public static final String HELLO_WORLD = "Hello, world!";

    public static Resource mockMultipartFile() throws IOException {
        Path testFile = Files.createTempFile("test", ".txt");
        Files.writeString(testFile, HELLO_WORLD);
        return new FileSystemResource(testFile.toFile());
    }

    @Test
    public void testFileDownloadDatabase() throws IOException {

        RequestResponse response =
            given(this.spec).
                multiPart(mockMultipartFile().getFile()).
                filter(document("dbupload")).
            when().
                post("/api/db/upload").
            andReturn().
                body().
                as(RequestResponse.class);

        Assertions.assertNotNull(response);

        byte[] file
            = given(this.spec)
                .filter(document("index"))
            .when()
                .get(response.getDownloadUrl())
            .asByteArray();

        Assertions.assertNotNull(file);
        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testFileDownloadFilesystem() throws IOException {

        RequestResponse responseEntity =
            given(this.spec)
                .multiPart(mockMultipartFile().getFile())
                .filter(document("upload"))
            .when()
                .post("/api/files/upload")
            .andReturn().as(RequestResponse.class);

        Assertions.assertNotNull(responseEntity);
        Assertions.assertNotNull(responseEntity.getDownloadUrl());

        byte[] file
            = given(this.spec)
                .filter(document("index"))
            .when()
                .get(responseEntity.getDownloadUrl())
            .asByteArray();
        Assertions.assertNotNull(file);
        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }


    @Test
    public void testFileDownloadInFilesystemFolder() throws IOException {
        RequestResponse responseEntity = given(this.spec)
                .filter(document("upload"))
                .multiPart(mockMultipartFile().getFile())
            .when()
                .post("/api/files/upload?path=test")
           .andReturn().as(RequestResponse.class);

        // todo Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertNotNull(responseEntity);

        byte[] file
            = given(this.spec)
                .filter(document("index"))
            .when()
                .get(responseEntity.getDownloadUrl())
            .asByteArray();

        Assertions.assertNotNull(file);
        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }


}
