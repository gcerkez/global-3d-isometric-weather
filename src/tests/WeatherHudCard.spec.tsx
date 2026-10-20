import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WeatherHudCard } from '../components/WeatherHudCard';
import { WeatherTelemetry } from '../types/weather';

const mockTelemetry: WeatherTelemetry = {
  tempF: 72,
  feelsLikeF: 71,
  humidity: 48,
  windMph: 9,
  windDeg: 120,
  weatherCode: 0,
  isDay: true,
  usAqi: 28,
  uvIndex: 4.5,
  locationName: 'Yorktown, VA',
  condition: 'Sunny',
  hourlyTemps: [70, 72, 73],
  hourly: [],
  sevenDayDetailed: [],
  provider: 'open-meteo',
  lastUpdated: Date.now(),
};

describe('WeatherHudCard Component', () => {
  it('renders loading state when telemetry is null', () => {
    render(
      <WeatherHudCard
        telemetry={null}
        dioramaTheme="yorktown"
        onThemeChange={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        onOpenForecast={vi.fn()}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
        timeMode="realtime"
        onCycleTimeMode={vi.fn()}
      />
    );

    expect(screen.getByTestId('hud-card-loading')).toBeInTheDocument();
  });

  it('renders telemetry values in HUD card', () => {
    render(
      <WeatherHudCard
        telemetry={mockTelemetry}
        dioramaTheme="yorktown"
        onThemeChange={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        onOpenForecast={vi.fn()}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
        timeMode="realtime"
        onCycleTimeMode={vi.fn()}
      />
    );

    expect(screen.getByTestId('hud-condition')).toHaveTextContent('SUNNY');
    expect(screen.getByTestId('hud-temp')).toHaveTextContent('72°F (22°C)');
    expect(screen.getByTestId('hud-humidity')).toHaveTextContent('48% HUMIDITY');
    expect(screen.getByTestId('hud-wind')).toHaveTextContent('9 MPH WIND');
    expect(screen.getByTestId('hud-aqi')).toHaveTextContent('AQI 28 GOOD');
    expect(screen.getByTestId('hud-uv')).toHaveTextContent('UV 4.5');
  });

  it('handles search input submission', () => {
    const handleSearch = vi.fn();
    render(
      <WeatherHudCard
        telemetry={mockTelemetry}
        dioramaTheme="yorktown"
        onThemeChange={vi.fn()}
        onSearchCity={handleSearch}
        onAutoLoc={vi.fn()}
        onOpenForecast={vi.fn()}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
        timeMode="realtime"
        onCycleTimeMode={vi.fn()}
      />
    );

    const input = screen.getByTestId('city-search-input');
    fireEvent.change(input, { target: { value: 'Denver, CO' } });

    const btn = screen.getByTestId('city-search-btn');
    fireEvent.click(btn);

    expect(handleSearch).toHaveBeenCalledWith('Denver, CO');
  });

  it('triggers onOpenForecast when 7-day forecast button is clicked', () => {
    const handleOpen = vi.fn();
    render(
      <WeatherHudCard
        telemetry={mockTelemetry}
        dioramaTheme="yorktown"
        onThemeChange={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        onOpenForecast={handleOpen}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
        timeMode="realtime"
        onCycleTimeMode={vi.fn()}
      />
    );

    const forecastBtn = screen.getByTestId('hud-forecast-btn');
    fireEvent.click(forecastBtn);

    expect(handleOpen).toHaveBeenCalledTimes(1);
  });

  it('triggers onToggleTempUnit when temp text is clicked', () => {
    const handleToggle = vi.fn();
    render(
      <WeatherHudCard
        telemetry={mockTelemetry}
        dioramaTheme="yorktown"
        onThemeChange={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        onOpenForecast={vi.fn()}
        tempUnit="F"
        onToggleTempUnit={handleToggle}
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
        timeMode="realtime"
        onCycleTimeMode={vi.fn()}
      />
    );

    const tempEl = screen.getByTestId('hud-temp');
    fireEvent.click(tempEl);

    expect(handleToggle).toHaveBeenCalledTimes(1);
  });
});
