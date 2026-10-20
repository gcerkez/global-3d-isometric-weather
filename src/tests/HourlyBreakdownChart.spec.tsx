import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HourlyBreakdownChart } from '../components/HourlyBreakdownChart';
import { DailyForecast } from '../types/weather';

const mockDayWithHours: DailyForecast = {
  dayName: 'TODAY',
  dateStr: '2026-10-21',
  tempMin: 52,
  tempMax: 70,
  precipProbMax: 35,
  precipSum: 0.12,
  windMax: 11,
  uvMax: 4.2,
  sunrise: '7:14 AM',
  sunset: '6:21 PM',
  condition: 'Partly Cloudy',
  emoji: '⛅',
  hourly: [
    {
      time: '2026-10-21T00:00:00Z',
      hourLabel: '12 AM',
      temp: 54,
      feelsLike: 53,
      humidity: 70,
      precipProb: 5,
      precipInches: 0,
      windSpeed: 4,
      windGust: 6,
      uvIndex: 0,
      weatherCode: 1,
      condition: 'Mainly Clear',
      emoji: '🌙',
    },
    {
      time: '2026-10-21T12:00:00Z',
      hourLabel: '12 PM',
      temp: 68,
      feelsLike: 67,
      humidity: 50,
      precipProb: 35,
      precipInches: 0.08,
      windSpeed: 10,
      windGust: 14,
      uvIndex: 4,
      weatherCode: 2,
      condition: 'Partly Cloudy',
      emoji: '⛅',
    },
  ],
};

describe('HourlyBreakdownChart Component', () => {
  it('renders mode switcher buttons, canvas, and metric tiles', () => {
    render(<HourlyBreakdownChart day={mockDayWithHours} tempUnit="F" />);

    expect(screen.getByTestId('graph-title')).toHaveTextContent('Hourly Breakdown • TODAY');
    expect(screen.getByTestId('mode-precip-btn')).toHaveClass('active');
    expect(screen.getByTestId('mode-temp-btn')).toBeInTheDocument();
    expect(screen.getByTestId('mode-wind-btn')).toBeInTheDocument();
    expect(screen.getByTestId('hourly-canvas')).toBeInTheDocument();

    // 4 metric tiles
    expect(screen.getByTestId('metric-tile-precip')).toBeInTheDocument();
    expect(screen.getByTestId('metric-tile-sun')).toBeInTheDocument();
    expect(screen.getByTestId('metric-tile-wind')).toBeInTheDocument();
    expect(screen.getByTestId('metric-tile-uv')).toBeInTheDocument();
  });

  it('switches graph mode between precip, temp, and wind', () => {
    render(<HourlyBreakdownChart day={mockDayWithHours} tempUnit="F" />);

    const tempBtn = screen.getByTestId('mode-temp-btn');
    fireEvent.click(tempBtn);
    expect(tempBtn).toHaveClass('active');
    expect(screen.getByTestId('mode-precip-btn')).not.toHaveClass('active');

    const windBtn = screen.getByTestId('mode-wind-btn');
    fireEvent.click(windBtn);
    expect(windBtn).toHaveClass('active');
    expect(tempBtn).not.toHaveClass('active');
  });

  it('displays correct sun cycle times and precipitation sum in metrics grid', () => {
    render(<HourlyBreakdownChart day={mockDayWithHours} tempUnit="F" />);

    expect(screen.getByTestId('metric-tile-precip')).toHaveTextContent('0.12 in');
    expect(screen.getByTestId('metric-tile-sun')).toHaveTextContent('7:14 AM');
    expect(screen.getByTestId('metric-tile-sun')).toHaveTextContent('Sunset: 6:21 PM');
    expect(screen.getByTestId('metric-tile-wind')).toHaveTextContent('11 MPH');
  });
});
