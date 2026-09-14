package com.deanwagman.lumenmarsh.venueops.weather.domain;

public class InvalidWeatherRecommendationTransitionException extends RuntimeException {

    private final WeatherRecommendationOperatorStatus currentStatus;
    private final WeatherRecommendationCommand command;

    public InvalidWeatherRecommendationTransitionException(
            WeatherRecommendationOperatorStatus currentStatus,
            WeatherRecommendationCommand command
    ) {
        super("Cannot apply " + command + " when recommendation handling is " + currentStatus);
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public WeatherRecommendationOperatorStatus currentStatus() {
        return currentStatus;
    }

    public WeatherRecommendationCommand command() {
        return command;
    }
}
