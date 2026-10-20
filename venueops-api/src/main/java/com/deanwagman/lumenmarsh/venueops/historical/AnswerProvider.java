package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsAnswer;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsQuestion;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;

public interface AnswerProvider {

    AnalyticsAnswer answer(AnalyticsQuestion question, AvailabilitySummary curatedAvailability);
}
