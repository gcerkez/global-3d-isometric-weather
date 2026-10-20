import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DayForecastRow } from '../components/DayForecastRow';
import { DailyForecast } from '../types/weather';

const mockDay: DailyForecast = {
  dayName: 'Wed',
  dateStr: '2026-10-21',
  tempMin: 50,
  tempMax: 72,
  precipProbMax: 45,
  precipSum: 0.18,
  windMax: 12,
  uvMax: 4.5,
  sunrise: '7:15 AM',
  sunset: '6:20 PM',
  condition: 'Chance Rain',
  emoji: '🌧️',
  hourly: [],
};

describe('DayForecastRow Component', () => {
  it('renders day row with correct metrics and emoji', () => {
    render(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={false}
        onSelect={vi.fn()}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      />
    );

    expect(screen.getByTestId('day-name-2026-10-21')).toHaveTextContent('Wed');
    expect(screen.getByTestId('day-min-2026-10-21')).toHaveTextContent('50°');
    expect(screen.getByTestId('day-max-2026-10-21')).toHaveTextContent('72°');
    expect(screen.getByTestId('day-rain-badge-2026-10-21')).toHaveTextContent('45%');
  });

  it('renders "TODAY" label when isToday is true', () => {
    render(
      <DayForecastRow
        day={mockDay}
        isToday={true}
        isSelected={false}
        onSelect={vi.fn()}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      />
    );

    expect(screen.getByTestId('day-name-2026-10-21')).toHaveTextContent('TODAY');
    expect(screen.getByTestId('current-temp-dot')).toBeInTheDocument();
  });

  it('invokes onSelect when clicked without reloading or wiping DOM', () => {
    const handleSelect = vi.fn();
    render(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={false}
        onSelect={handleSelect}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      />
    );

    const header = screen.getByTestId('day-header-2026-10-21');
    fireEvent.click(header);
    expect(handleSelect).toHaveBeenCalledTimes(1);
  });

  it('supports keyboard navigation via Enter and Space keys', () => {
    const handleSelect = vi.fn();
    render(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={false}
        onSelect={handleSelect}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      />
    );

    const header = screen.getByTestId('day-header-2026-10-21');
    fireEvent.keyDown(header, { key: 'Enter' });
    expect(handleSelect).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(header, { key: ' ' });
    expect(handleSelect).toHaveBeenCalledTimes(2);
  });

  it('mounts children smoothly when isSelected is true', () => {
    const { rerender } = render(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={false}
        onSelect={vi.fn()}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      >
        <div data-testid="expanded-content">Expanded Details</div>
      </DayForecastRow>
    );

    expect(screen.queryByTestId('expanded-content')).not.toBeInTheDocument();

    rerender(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={true}
        onSelect={vi.fn()}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
      >
        <div data-testid="expanded-content">Expanded Details</div>
      </DayForecastRow>
    );

    expect(screen.getByTestId('expanded-content')).toBeInTheDocument();
    expect(screen.getByTestId('day-row-2026-10-21')).toHaveClass('mat-expanded');
  });

  it('renders temperature correctly in Celsius mode', () => {
    render(
      <DayForecastRow
        day={mockDay}
        isToday={false}
        isSelected={false}
        onSelect={vi.fn()}
        weekMin={45}
        weekMax={75}
        currentTempF={65}
        tempUnit="C"
      />
    );

    // 50°F is 10°C, 72°F is 22°C
    expect(screen.getByTestId('day-min-2026-10-21')).toHaveTextContent('10°');
    expect(screen.getByTestId('day-max-2026-10-21')).toHaveTextContent('22°');
  });
});
