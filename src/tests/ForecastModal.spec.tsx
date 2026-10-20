import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ForecastModal } from '../components/ForecastModal';
import { WeatherTelemetry } from '../types/weather';

const mockTelemetry: WeatherTelemetry = {
  tempF: 68,
  feelsLikeF: 67,
  humidity: 55,
  windMph: 8,
  windDeg: 180,
  weatherCode: 0,
  isDay: true,
  usAqi: 24,
  uvIndex: 3.2,
  locationName: 'Yorktown, VA',
  condition: 'Clear Sky',
  hourlyTemps: [65, 66, 68, 70],
  hourly: [
    {
      time: '2026-10-21T12:00:00Z',
      hourLabel: '12 PM',
      temp: 68,
      feelsLike: 67,
      humidity: 55,
      precipProb: 10,
      precipInches: 0,
      windSpeed: 8,
      windGust: 10,
      uvIndex: 4,
      weatherCode: 0,
      condition: 'Clear Sky',
      emoji: '☀️',
    },
  ],
  sevenDayDetailed: [
    {
      dayName: 'TODAY',
      dateStr: '2026-10-21',
      tempMin: 52,
      tempMax: 70,
      precipProbMax: 10,
      precipSum: 0,
      windMax: 9,
      uvMax: 3.5,
      sunrise: '7:14 AM',
      sunset: '6:21 PM',
      condition: 'Clear Sky',
      emoji: '☀️',
      hourly: [
        {
          time: '2026-10-21T12:00:00Z',
          hourLabel: '12 PM',
          temp: 68,
          feelsLike: 67,
          humidity: 55,
          precipProb: 10,
          precipInches: 0,
          windSpeed: 8,
          windGust: 10,
          uvIndex: 4,
          weatherCode: 0,
          condition: 'Clear Sky',
          emoji: '☀️',
        },
      ],
    },
    {
      dayName: 'Thu',
      dateStr: '2026-10-22',
      tempMin: 54,
      tempMax: 74,
      precipProbMax: 60,
      precipSum: 0.35,
      windMax: 14,
      uvMax: 2.8,
      sunrise: '7:15 AM',
      sunset: '6:20 PM',
      condition: 'Showers',
      emoji: '🌧️',
      hourly: [],
    },
  ],
  provider: 'open-meteo',
  lastUpdated: Date.now(),
};

describe('ForecastModal Component', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ForecastModal
        isOpen={false}
        onClose={vi.fn()}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal dialog with hero card and 7-day outlook', () => {
    render(
      <ForecastModal
        isOpen={true}
        onClose={vi.fn()}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
      />
    );

    expect(screen.getByTestId('forecast-modal')).toBeInTheDocument();
    expect(screen.getByTestId('modal-title')).toHaveTextContent('📍 Yorktown, VA');
    expect(screen.getByTestId('hero-temp-large')).toHaveTextContent('68°F');
    expect(screen.getByTestId('modal-outlook-card')).toBeInTheDocument();
  });

  it('closes when close button is clicked', () => {
    const handleClose = vi.fn();
    render(
      <ForecastModal
        isOpen={true}
        onClose={handleClose}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
      />
    );

    const closeBtn = screen.getByTestId('close-modal-btn');
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('closes when Escape key is pressed', () => {
    const handleClose = vi.fn();
    render(
      <ForecastModal
        isOpen={true}
        onClose={handleClose}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
      />
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('switches selected day accordion without wiping DOM or flickering', () => {
    render(
      <ForecastModal
        isOpen={true}
        onClose={vi.fn()}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={vi.fn()}
      />
    );

    // Initial state: TODAY is selected
    expect(screen.getByTestId('day-row-2026-10-21')).toHaveClass('mat-expanded');
    expect(screen.getByTestId('day-row-2026-10-22')).not.toHaveClass('mat-expanded');

    // Tap on Thursday row
    const thuHeader = screen.getByTestId('day-header-2026-10-22');
    fireEvent.click(thuHeader);

    // Thursday row is now expanded smoothly, TODAY is collapsed
    expect(screen.getByTestId('day-row-2026-10-22')).toHaveClass('mat-expanded');
    expect(screen.getByTestId('day-row-2026-10-21')).not.toHaveClass('mat-expanded');
  });

  it('invokes onToggleTempUnit when unit toggle button is clicked', () => {
    const handleToggle = vi.fn();
    render(
      <ForecastModal
        isOpen={true}
        onClose={vi.fn()}
        telemetry={mockTelemetry}
        tempUnit="F"
        onToggleTempUnit={handleToggle}
      />
    );

    const unitBtn = screen.getByTestId('unit-toggle-btn');
    fireEvent.click(unitBtn);
    expect(handleToggle).toHaveBeenCalledTimes(1);
  });
});
