package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_work_order_sequence")
public class MaintenanceWorkOrderSequenceEntity {

    @Id
    @Column(name = "year")
    private Integer year;

    @Column(name = "last_value", nullable = false)
    private long lastValue;

    protected MaintenanceWorkOrderSequenceEntity() {
    }
}
