package org.clematis.storage.repository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.clematis.storage.ApplicationTests;
import org.clematis.storage.model.StorageEntity;
import org.clematis.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockMultipartFile;

public class DBStorageEntityRepositoryTests extends ApplicationTests {

    @Autowired
    private StorageEntityRepository storageEntityRepository;

    @Qualifier("dbStorageServiceImpl")
    @Autowired
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        storageEntityRepository.deleteAll();
    }

    @Test
    public void testSaveAttachment() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.txt", "text/plain",
            "Hello, world!".getBytes(StandardCharsets.UTF_8));
        StorageEntity storageEntity = storageService.saveAttachment(mockFile);
        assertNotNull(storageEntity.getId());
        assertEquals("test.txt", storageEntity.getFileName());
        assertEquals("text/plain", storageEntity.getFileType());
    }

    @Test
    public void testSaveFiles() throws Exception {
        MockMultipartFile mockFile1 = new MockMultipartFile(
            "file", "test1.pdf",
            "text/plain", "Hello, world!".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile mockFile2 = new MockMultipartFile(
            "file", "test2.txt", "text/plain",
            "Goodbye, world!".getBytes(StandardCharsets.UTF_8));
        storageService.saveFiles(new MockMultipartFile[]{mockFile1, mockFile2});
        List<StorageEntity> storageEntities = storageService.getAllFiles();
        System.out.println("Saved files:");
        for (StorageEntity product : storageEntities) {
            System.out.println(product.getFileName());
        }
        assertEquals(2, storageEntities.size());
        assertEquals("test1.pdf", storageEntities.get(0).getFileName());
        assertEquals("test2.txt", storageEntities.get(1).getFileName());
    }

    @Test
    public void testSaveAttachmentInvalidName() {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "../test.txt", "text/plain",
            "Hello, world!".getBytes(StandardCharsets.UTF_8));
        assertThrows(Exception.class, () -> storageService.saveAttachment(mockFile));
    }

    @Test
    public void testSaveAttachmentTooLarge() {
        byte[] bytes = new byte[1024 * 1024 * 10];
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", bytes);
        assertThrows(Exception.class, () -> storageService.saveAttachment(mockFile));
    }

}
