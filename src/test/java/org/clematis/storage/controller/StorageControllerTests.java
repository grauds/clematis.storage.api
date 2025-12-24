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
import java.util.List;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class StorageControllerTests extends ApplicationTests {

    public static final String HELLO_WORLD = "Hello, world!";

    public static Resource mockMultipartFile() throws IOException {
        Path testFile = Files.createTempFile("test", ".txt");
        Files.writeString(testFile, HELLO_WORLD);
        return new FileSystemResource(testFile.toFile());
    }

    /* -------------------------------------------------------
     *                Helper upload methods
     * ------------------------------------------------------- */

    private RequestResponse uploadToDb() throws IOException {
        return given(this.spec)
            .multiPart(mockMultipartFile().getFile())
            .filter(document("db-upload"))
            .when()
            .post("/api/db/upload")
            .andReturn()
            .as(RequestResponse.class);
    }

    private RequestResponse uploadToFs() throws IOException {
        return given(this.spec)
            .multiPart(mockMultipartFile().getFile())
            .filter(document("fs-upload"))
            .when()
            .post("/api/files/upload")
            .andReturn()
            .as(RequestResponse.class);
    }


    /* -------------------------------------------------------
     *                Database storage tests
     * ------------------------------------------------------- */

    @Test
    public void testDbFileUploadAndDownload() throws IOException {
        RequestResponse response = uploadToDb();
        Assertions.assertNotNull(response);

        byte[] file = given(this.spec)
            .filter(document("db-download"))
            .when()
            .get(response.getDownloadUrl())
            .asByteArray();

        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testDbGetAllFiles() throws IOException {
        uploadToDb(); // ensure at least one exists

        List<RequestResponse> list =
            given(this.spec)
                .filter(document("db-getall"))
                .when()
                .get("/api/db/get")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList(".", RequestResponse.class);

        Assertions.assertFalse(list.isEmpty());
    }

    @Test
    public void testDbUploadBatch() throws IOException {
        Resource file1 = mockMultipartFile();
        Resource file2 = mockMultipartFile();

        List<RequestResponse> list = given(this.spec)
            .multiPart("files", file1.getFile())
            .multiPart("files", file2.getFile())
            .filter(document("db-upload-batch"))
            .when()
            .post("/api/db/upload/batch")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath().getList(".", RequestResponse.class);

        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testDbDeleteFile() throws IOException {
        RequestResponse saved = uploadToDb();
        String id = saved.getDownloadUrl().substring(saved.getDownloadUrl().lastIndexOf('/') + 1);

        given(this.spec)
            .filter(document("db-delete"))
            .when()
            .delete("/api/db/" + id)
            .then()
            .statusCode(200);

        given(this.spec)
            .when()
            .get(saved.getDownloadUrl())
            .then()
            .statusCode(404);
    }


    /* -------------------------------------------------------
     *                Filesystem storage tests
     * ------------------------------------------------------- */

    @Test
    public void testFsFileUploadAndDownload() throws IOException {
        RequestResponse response = uploadToFs();
        Assertions.assertNotNull(response);

        byte[] file = given(this.spec)
            .filter(document("fs-download"))
            .when()
            .get(response.getDownloadUrl())
            .asByteArray();

        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testFsGetAllReturns200() {
        given(this.spec)
            .filter(document("fs-getall"))
            .when()
            .get("/api/files/get")
            .then()
            .statusCode(200);
    }

    @Test
    public void testFsUploadToFolder() throws IOException {
        RequestResponse responseEntity = given(this.spec)
            .filter(document("fs-upload-path"))
            .multiPart(mockMultipartFile().getFile())
            .when()
            .post("/api/files/upload?path=test-folder")
            .andReturn().as(RequestResponse.class);

        Assertions.assertNotNull(responseEntity);

        byte[] file = given(this.spec)
            .filter(document("fs-download-from-folder"))
            .when()
            .get(responseEntity.getDownloadUrl())
            .asByteArray();

        Assertions.assertEquals(HELLO_WORLD, new String(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testFsUploadBatch() throws IOException {
        Resource file1 = mockMultipartFile();
        Resource file2 = mockMultipartFile();

        List<RequestResponse> list = given(this.spec)
            .multiPart("files", file1.getFile())
            .multiPart("files", file2.getFile())
            .filter(document("fs-upload-batch"))
            .when()
            .post("/api/files/upload/batch")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath().getList(".", RequestResponse.class);

        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFsDeleteFile() throws IOException {
        RequestResponse saved = uploadToFs();
        String id = saved.getDownloadUrl().substring(saved.getDownloadUrl().lastIndexOf('/') + 1);

        given(this.spec)
            .filter(document("fs-delete"))
            .when()
            .delete("/api/files/" + id)
            .then()
            .statusCode(200);

        given(this.spec)
            .when()
            .get(saved.getDownloadUrl())
            .then()
            .statusCode(404);
    }

    /* -------------------------------------------------------
     *               Negative / Error scenarios
     * ------------------------------------------------------- */

    @Test
    public void testDownloadNotFound() {
        given(this.spec)
            .when()
            .get("/api/files/does-not-exist")
            .then()
            .statusCode(404);
    }

    @Test
    public void testUploadWithoutFileFails() {
        given(this.spec)
            .multiPart("file", new byte[0]) // empty content, still multipart
            .when()
            .post("/api/files/upload")
            .then()
            .statusCode(400); // now Spring sees a multipart request
    }
}
