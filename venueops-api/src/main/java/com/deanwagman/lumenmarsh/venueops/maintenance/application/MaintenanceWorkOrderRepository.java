package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;

import java.util.List;
import java.util.Optional;

public interface MaintenanceWorkOrderRepository {

    Optional<MaintenanceWorkOrder> findById(MaintenanceWorkOrderId id);

    List<MaintenanceWorkOrder> findAll();

    List<MaintenanceWorkOrder> findByAssetId(MaintenanceAssetId assetId);

    void save(MaintenanceWorkOrder workOrder);
}
