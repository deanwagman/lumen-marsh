package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsAnswer;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsQuestion;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityDetail;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Evidence;

import java.util.List;

public interface AnswerProvider {

    AnalyticsAnswer answer(AnalyticsQuestion question, AvailabilitySummary curatedAvailability);

    List<AnalyticsAnswer> curated(AvailabilitySummary summary, List<AvailabilityDetail> details, List<Evidence> evidence);
}
