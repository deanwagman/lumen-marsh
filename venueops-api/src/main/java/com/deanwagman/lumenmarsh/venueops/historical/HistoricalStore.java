package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ReplayBundle;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Snapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoricalStore {

    Optional<Dataset> findDataset(String datasetId);

    List<Dataset> listDatasets();

    void replace(ReplayBundle bundle);

    List<OperatingDay> days(String datasetId, LocalDate from, LocalDate to);

    List<AttractionPlan> plans(String datasetId, LocalDate from, LocalDate to);

    List<Snapshot> snapshots(String datasetId);

    List<Activity> activities(String datasetId, String attractionId);
}
