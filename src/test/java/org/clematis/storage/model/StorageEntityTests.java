package org.clematis.storage.model;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StorageEntityTests {

    public static final String APPLICATION_IMAGE = "application/image";
    public static final String TEST_FOLDER_FILE_JPG = "test/folder/file.jpg";
    public static final byte[] DATA = new byte[0];

    @Test
    public void testEquals() {
        StorageEntity storageEntity = new StorageEntity();
        storageEntity.setId(String.valueOf(UUID.randomUUID()));
        storageEntity.setData(DATA);
        storageEntity.setContentType(APPLICATION_IMAGE);
        storageEntity.setFileName(TEST_FOLDER_FILE_JPG);

        StorageEntity storageEntity2 = new StorageEntity();
        storageEntity2.setId(String.valueOf(UUID.randomUUID()));
        storageEntity2.setData(DATA);
        storageEntity2.setContentType(APPLICATION_IMAGE);
        storageEntity2.setFileName(TEST_FOLDER_FILE_JPG);

        Assertions.assertNotEquals(storageEntity, storageEntity2);

        storageEntity2.setId(storageEntity.getId());
        Assertions.assertEquals(storageEntity, storageEntity2);

        StorageEntity storageEntity3 = StorageEntity.builder()
            .id(storageEntity.getId())
            .contentType(APPLICATION_IMAGE)
            .data(DATA)
            .fileName(TEST_FOLDER_FILE_JPG)
            .build();
        Assertions.assertEquals(storageEntity, storageEntity3);
    }
}
