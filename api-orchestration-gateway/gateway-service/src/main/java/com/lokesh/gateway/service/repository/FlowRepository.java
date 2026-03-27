package com.lokesh.gateway.service.repository;

import com.lokesh.gateway.common.enums.FlowStatus;
import com.lokesh.gateway.service.entity.FlowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowRepository extends JpaRepository<FlowEntity, UUID> {

    List<FlowEntity> findAllByStatus(FlowStatus status);

    Page<FlowEntity> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Page<FlowEntity> findByStatusAndNameContainingIgnoreCase(FlowStatus status, String search, Pageable pageable);

    Page<FlowEntity> findByStatus(FlowStatus status, Pageable pageable);

    long countByStatus(FlowStatus status);

    Optional<FlowEntity> findByName(String name);
}
