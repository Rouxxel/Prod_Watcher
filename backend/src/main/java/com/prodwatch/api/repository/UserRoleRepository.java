package com.prodwatch.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    Optional<UserRole> findByUser_Id(UUID userId);

    boolean existsByRole(AppRole role);

    long countByRole(AppRole role);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role = :role AND ur.user.active = true")
    long countActiveByRole(@Param("role") AppRole role);
}
