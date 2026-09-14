from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class QuantitativeValue(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    unit_code: str | None = Field(default=None, alias="unitCode")
    value: float | None = None


class NwsPointProperties(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    cwa: str | None = None
    forecast_office: str = Field(alias="forecastOffice")
    grid_id: str = Field(alias="gridId")
    grid_x: int = Field(alias="gridX")
    grid_y: int = Field(alias="gridY")
    forecast_hourly: str = Field(alias="forecastHourly")
    observation_stations: str = Field(alias="observationStations")
    forecast_zone: str = Field(alias="forecastZone")
    county: str


class NwsPointResponse(BaseModel):
    model_config = ConfigDict(extra="allow")

    properties: NwsPointProperties


class NwsStationProperties(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    station_identifier: str = Field(alias="stationIdentifier")
    name: str = "Unknown station"


class NwsStationFeature(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str | None = None
    geometry: dict[str, Any] | None = None
    properties: NwsStationProperties


class NwsStationCollection(BaseModel):
    model_config = ConfigDict(extra="allow")

    features: list[NwsStationFeature] = Field(default_factory=list)


class NwsObservationProperties(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    station: str | None = None
    timestamp: str
    text_description: str | None = Field(default=None, alias="textDescription")
    present_weather: list[Any] | None = Field(default=None, alias="presentWeather")
    temperature: QuantitativeValue | None = None
    relative_humidity: QuantitativeValue | None = Field(default=None, alias="relativeHumidity")
    wind_speed: QuantitativeValue | None = Field(default=None, alias="windSpeed")
    wind_gust: QuantitativeValue | None = Field(default=None, alias="windGust")
    wind_direction: QuantitativeValue | None = Field(default=None, alias="windDirection")
    precipitation_last_hour: QuantitativeValue | None = Field(
        default=None, alias="precipitationLastHour"
    )
    visibility: QuantitativeValue | None = None


class NwsObservationResponse(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str | None = None
    properties: NwsObservationProperties


class NwsForecastPeriod(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    start_time: str = Field(alias="startTime")
    end_time: str = Field(alias="endTime")
    temperature: float | None = None
    temperature_unit: str | None = Field(default=None, alias="temperatureUnit")
    wind_speed: str | QuantitativeValue | None = Field(default=None, alias="windSpeed")
    wind_gust: str | QuantitativeValue | None = Field(default=None, alias="windGust")
    probability_of_precipitation: QuantitativeValue | None = Field(
        default=None, alias="probabilityOfPrecipitation"
    )
    short_forecast: str = Field(default="", alias="shortForecast")


class NwsForecastProperties(BaseModel):
    model_config = ConfigDict(extra="allow")

    periods: list[NwsForecastPeriod] = Field(default_factory=list)


class NwsForecastResponse(BaseModel):
    model_config = ConfigDict(extra="allow")

    properties: NwsForecastProperties


class NwsAlertProperties(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str | None = None
    event: str = "Unknown"
    severity: str = "Unknown"
    urgency: str = "Unknown"
    certainty: str = "Unknown"
    headline: str | None = None
    description: str | None = None
    instruction: str | None = None
    onset: str | None = None
    expires: str | None = None


class NwsAlertFeature(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str | None = None
    properties: NwsAlertProperties


class NwsAlertCollection(BaseModel):
    model_config = ConfigDict(extra="allow")

    features: list[NwsAlertFeature] = Field(default_factory=list)
