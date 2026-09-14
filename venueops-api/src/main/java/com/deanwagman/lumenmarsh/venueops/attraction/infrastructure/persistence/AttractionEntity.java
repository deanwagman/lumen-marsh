package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attraction")
public class AttractionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AttractionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AttractionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_mode", nullable = false, length = 64)
    private CapacityMode capacityMode;

    @Column(name = "wait_minutes")
    private Integer waitMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long version;

    protected AttractionEntity() {
    }

    public AttractionEntity(
            String id,
            String name,
            String area,
            AttractionType type,
            AttractionStatus status,
            CapacityMode capacityMode,
            Integer waitMinutes,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.type = type;
        this.status = status;
        this.capacityMode = capacityMode;
        this.waitMinutes = waitMinutes;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public AttractionType getType() {
        return type;
    }

    public AttractionStatus getStatus() {
        return status;
    }

    public CapacityMode getCapacityMode() {
        return capacityMode;
    }

    public Integer getWaitMinutes() {
        return waitMinutes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
