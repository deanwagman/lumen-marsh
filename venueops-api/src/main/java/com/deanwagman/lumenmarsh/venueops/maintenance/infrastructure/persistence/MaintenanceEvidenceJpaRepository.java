package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceEvidenceJpaRepository extends JpaRepository<MaintenanceEvidenceEntity, String> {
    List<MaintenanceEvidenceEntity> findByWorkOrderIdOrderByAddedAtAsc(String workOrderId);
}
