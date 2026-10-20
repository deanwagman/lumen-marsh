package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Evidence;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ReplayBundle;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Snapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryHistoricalStore implements HistoricalStore {

    private final Map<String, ReplayBundle> datasets = new LinkedHashMap<>();

    @Override
    public synchronized Optional<Dataset> findDataset(String datasetId) {
        return Optional.ofNullable(datasets.get(datasetId)).map(ReplayBundle::dataset);
    }

    @Override
    public synchronized List<Dataset> listDatasets() {
        return datasets.values().stream().map(ReplayBundle::dataset)
                .sorted(Comparator.comparing(Dataset::createdAt)).toList();
    }

    @Override
    public synchronized void replace(ReplayBundle bundle) {
        datasets.put(bundle.dataset().id(), copy(bundle));
    }

    @Override
    public synchronized List<OperatingDay> days(String datasetId, LocalDate from, LocalDate to) {
        return bundle(datasetId).days().stream()
                .filter(day -> !day.parkDate().isBefore(from) && !day.parkDate().isAfter(to))
                .sorted(Comparator.comparing(OperatingDay::parkDate)).toList();
    }

    @Override
    public synchronized List<AttractionPlan> plans(String datasetId, LocalDate from, LocalDate to) {
        return bundle(datasetId).plans().stream()
                .filter(plan -> !plan.parkDate().isBefore(from) && !plan.parkDate().isAfter(to))
                .sorted(Comparator.comparing(AttractionPlan::parkDate).thenComparing(AttractionPlan::attractionId))
                .toList();
    }

    @Override
    public synchronized List<Snapshot> snapshots(String datasetId) {
        return List.copyOf(bundle(datasetId).snapshots());
    }

    @Override
    public synchronized List<Activity> activities(String datasetId, String attractionId) {
        return bundle(datasetId).activities().stream()
                .filter(activity -> activity.value().attractionId().value().equals(attractionId))
                .sorted(Comparator.comparing((Activity activity) -> activity.value().occurredAt())
                        .thenComparing(activity -> activity.value().resultingVersion()))
                .toList();
    }

    @Override
    public synchronized List<Evidence> evidence(String datasetId) {
        return bundle(datasetId).evidence().stream()
                .sorted(Comparator.comparing(Evidence::occurredAt).thenComparing(Evidence::id))
                .toList();
    }

    private ReplayBundle bundle(String datasetId) {
        ReplayBundle bundle = datasets.get(datasetId);
        if (bundle == null) {
            throw new HistoricalDatasetNotFoundException(datasetId);
        }
        return bundle;
    }

    private static ReplayBundle copy(ReplayBundle source) {
        return new ReplayBundle(source.dataset(), List.copyOf(source.days()), List.copyOf(source.plans()),
                List.copyOf(source.snapshots()), List.copyOf(source.activities()), List.copyOf(source.commands()),
                List.copyOf(source.evidence()));
    }
}
