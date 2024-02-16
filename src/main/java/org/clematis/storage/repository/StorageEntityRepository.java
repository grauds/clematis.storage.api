package org.clematis.storage.repository;

import org.clematis.storage.model.StorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageEntityRepository extends JpaRepository<StorageEntity, String> {
}
