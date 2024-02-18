package org.clematis.storage.repository;

import org.clematis.storage.model.StorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Default {@link JpaRepository} for storage of uploaded files metadata
 */
public interface StorageEntityRepository extends JpaRepository<StorageEntity, String> {
}
