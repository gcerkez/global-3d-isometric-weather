import React from 'react';
import { DailyForecast } from '../types/weather';

interface DayForecastRowProps {
  day: DailyForecast;
  isToday: boolean;
  isSelected: boolean;
  onSelect: () => void;
  weekMin: number;
  weekMax: number;
  currentTempF: number;
  tempUnit?: 'F' | 'C';
  children?: React.ReactNode;
}

export const DayForecastRow: React.FC<DayForecastRowProps> = ({
  day,
  isToday,
  isSelected,
  onSelect,
  weekMin,
  weekMax,
  currentTempF,
  tempUnit = 'F',
  children,
}) => {
  const formatTemp = (f: number) => {
    return tempUnit === 'C' ? Math.round(((f - 32) * 5) / 9) : Math.round(f);
  };

  const totalRange = Math.max(1, weekMax - weekMin);
  const leftPct = Math.max(0, Math.min(100, ((day.tempMin - weekMin) / totalRange) * 100));
  const rightPct = Math.max(0, Math.min(100, ((day.tempMax - weekMin) / totalRange) * 100));
  const widthPct = Math.max(4, rightPct - leftPct);

  const hasRainBadge =
    day.precipProbMax >= 15 &&
    (day.emoji === '🌦️' ||
      day.emoji === '🌧️' ||
      day.emoji === '🌨️' ||
      day.emoji === '⛈️' ||
      day.precipSum > 0.005 ||
      day.precipProbMax >= 20);

  let curDotPct: number | null = null;
  if (isToday) {
    curDotPct = Math.max(0, Math.min(100, ((currentTempF - weekMin) / totalRange) * 100));
  }

  return (
    <div
      className={`mat-expansion-panel ${isSelected ? 'mat-expanded' : ''}`}
      data-testid={`day-row-${day.dateStr}`}
    >
      <div
        className="mat-expansion-panel-header"
        onClick={onSelect}
        role="button"
        tabIndex={0}
        aria-expanded={isSelected}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onSelect();
          }
        }}
        data-testid={`day-header-${day.dateStr}`}
      >
        <div className="mat-panel-title" data-testid={`day-name-${day.dateStr}`}>
          {isToday ? 'TODAY' : day.dayName}
        </div>

        <div className="day-icon-box">
          <span className="day-emoji">{day.emoji}</span>
          {hasRainBadge && (
            <span className="p-badge" data-testid={`day-rain-badge-${day.dateStr}`}>
              {day.precipProbMax}%
            </span>
          )}
        </div>

        <div className="day-min" data-testid={`day-min-${day.dateStr}`}>
          {formatTemp(day.tempMin)}°
        </div>

        <div className="day-bar-box" data-testid="day-range-bar">
          <div
            className="day-bar-pill"
            style={{
              left: `${leftPct}%`,
              width: `${widthPct}%`,
              background: 'linear-gradient(90deg, #38bdf8, #ffd166)',
            }}
          />
          {curDotPct !== null && (
            <div
              className="cur-dot"
              style={{ left: `${curDotPct}%` }}
              title={`Current temp: ${formatTemp(currentTempF)}°${tempUnit}`}
              data-testid="current-temp-dot"
            />
          )}
        </div>

        <div className="day-max" data-testid={`day-max-${day.dateStr}`}>
          {formatTemp(day.tempMax)}°
        </div>

        <div
          className={`mat-expansion-indicator ${isSelected ? 'expanded' : ''}`}
          aria-hidden="true"
        >
          ▼
        </div>
      </div>

      {isSelected && (
        <div className="mat-expansion-panel-body" data-testid={`day-body-${day.dateStr}`}>
          <div className="mat-divider" />
          {children}
        </div>
      )}
    </div>
  );
};
