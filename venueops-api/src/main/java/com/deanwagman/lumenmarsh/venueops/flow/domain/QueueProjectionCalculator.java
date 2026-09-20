package com.deanwagman.lumenmarsh.venueops.flow.domain;

import java.util.List;

public final class QueueProjectionCalculator {

    public static final double MIN_THROUGHPUT_PER_MINUTE = 0.5;
    public static final double NEWEST_WEIGHT = 0.50;
    public static final double PREVIOUS_WEIGHT = 0.30;
    public static final double OLDER_WEIGHT = 0.20;

    private QueueProjectionCalculator() {
    }

    public static QueueProjection fromObservation(
            QueueObservation newest,
            List<QueueObservation> previousNewestFirst,
            QueueProjection current,
            int postedWaitMinutes,
            java.time.Instant now
    ) {
        if (current != null && !newest.observedAt().isAfter(current.observedAt())) {
            return current;
        }
        double newestArrivals = newest.arrivalsPerMinute();
        double newestThroughput = newest.throughputPerMinute();
        Double previousArrivals = previousNewestFirst.size() > 0 ? previousNewestFirst.get(0).arrivalsPerMinute() : null;
        Double olderArrivals = previousNewestFirst.size() > 1 ? previousNewestFirst.get(1).arrivalsPerMinute() : null;
        Double previousThroughput = previousNewestFirst.size() > 0 ? previousNewestFirst.get(0).throughputPerMinute() : null;
        Double olderThroughput = previousNewestFirst.size() > 1 ? previousNewestFirst.get(1).throughputPerMinute() : null;
        double arrivals = ewma(newestArrivals, previousArrivals, olderArrivals);
        double throughput = ewma(newestThroughput, previousThroughput, olderThroughput);
        int calculatedWait = calculatedWaitMinutes(newest.queueLength(), throughput);
        QueueTrend trend = trend(current, calculatedWait);
        int capacity = newest.configuredUnits() == 0
                ? 0
                : (int) Math.round(newest.operatingUnits() * 100.0 / newest.configuredUnits());
        long version = current == null ? 1L : current.version() + 1L;
        return new QueueProjection(
                newest.attractionId(),
                newest.observationId(),
                newest.queueLength(),
                roundRate(arrivals),
                roundRate(throughput),
                calculatedWait,
                Math.max(postedWaitMinutes, 0),
                capacity,
                trend,
                newest.observedAt(),
                newest.receivedAt(),
                now,
                version,
                newest.simulated()
        );
    }

    public static int calculatedWaitMinutes(int queueLength, double throughputPerMinute) {
        double rate = Math.max(throughputPerMinute, MIN_THROUGHPUT_PER_MINUTE);
        return (int) Math.ceil(Math.max(0, queueLength) / rate);
    }

    public static double ewma(double newest, Double previous, Double older) {
        if (previous == null) {
            return newest;
        }
        if (older == null) {
            double total = NEWEST_WEIGHT + PREVIOUS_WEIGHT;
            return (NEWEST_WEIGHT / total) * newest + (PREVIOUS_WEIGHT / total) * previous;
        }
        return NEWEST_WEIGHT * newest + PREVIOUS_WEIGHT * previous + OLDER_WEIGHT * older;
    }

    public static int predictedQueueLength(
            int currentQueue,
            double arrivalRate,
            double throughputRate,
            int horizonMinutes,
            int disruptionTransfer
    ) {
        double predicted = currentQueue
                + arrivalRate * horizonMinutes
                - throughputRate * horizonMinutes
                + disruptionTransfer;
        return (int) Math.max(0, Math.round(predicted));
    }

    public static int predictedWaitMinutes(int predictedQueue, double throughputRate) {
        return calculatedWaitMinutes(predictedQueue, throughputRate);
    }

    private static QueueTrend trend(QueueProjection current, int calculatedWait) {
        if (current == null) {
            return QueueTrend.STABLE;
        }
        int delta = calculatedWait - current.calculatedWaitMinutes();
        if (delta >= 2) {
            return QueueTrend.RISING;
        }
        if (delta <= -2) {
            return QueueTrend.FALLING;
        }
        return QueueTrend.STABLE;
    }

    private static double roundRate(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
