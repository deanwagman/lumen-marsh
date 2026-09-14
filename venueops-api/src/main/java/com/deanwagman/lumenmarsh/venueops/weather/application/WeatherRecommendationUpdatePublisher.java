package com.deanwagman.lumenmarsh.venueops.weather.application;

public interface WeatherRecommendationUpdatePublisher {

    void publish(WeatherRecommendationOperationalUpdate update);
}
