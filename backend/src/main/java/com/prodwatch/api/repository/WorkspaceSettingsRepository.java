package com.prodwatch.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.WorkspaceSettings;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettings, UUID> {

    Optional<WorkspaceSettings> findByEcosystem_Id(UUID ecosystemId);

    default Optional<WorkspaceSettings> findSingleton() {
        return findById(WorkspaceSettings.SINGLETON_ID);
    }
}
