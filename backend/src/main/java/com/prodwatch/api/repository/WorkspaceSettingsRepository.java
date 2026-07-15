package com.prodwatch.api.repository;

import java.util.Optional;
import java.util.UUID;

import com.prodwatch.api.entity.WorkspaceSettings;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettings, UUID> {

    default Optional<WorkspaceSettings> findSingleton() {
        return findById(WorkspaceSettings.SINGLETON_ID);
    }
}
