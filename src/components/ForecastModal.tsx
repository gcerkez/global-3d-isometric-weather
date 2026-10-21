import React, { useState, useEffect } from 'react';
import { WeatherTelemetry } from '../types/weather';
import { DayForecastRow } from './DayForecastRow';
import { HourlyBreakdownChart } from './HourlyBreakdownChart';
import { RadarView } from './RadarView';

interface ForecastModalProps {
  isOpen: boolean;
  onClose: () => void;
  telemetry: WeatherTelemetry | null;
  tempUnit: 'F' | 'C';
  onToggleTempUnit: () => void;
  lat?: number;
  lon?: number;
  onOpenRadar?: () => void;
}

export const ForecastModal: React.FC<ForecastModalProps> = ({
  isOpen,
  onClose,
  telemetry,
  tempUnit,
  onToggleTempUnit,
  lat = 37.2388,
  lon = -76.5097,
  onOpenRadar,
}) => {
  const [activeTab, setActiveTab] = useState<'outlook' | 'radar'>('outlook');
  const [selectedDayIdx, setSelectedDayIdx] = useState<number>(0);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !telemetry) return null;

  const days = telemetry.sevenDayDetailed || [];
  const selDay = days[selectedDayIdx] || days[0];

  const formatTemp = (f: number) => {
    return tempUnit === 'C' ? Math.round(((f - 32) * 5) / 9) : Math.round(f);
  };

  const weekMin = days.length > 0 ? Math.min(...days.map((d) => d.tempMin)) : 40;
  const weekMax = days.length > 0 ? Math.max(...days.map((d) => d.tempMax)) : 80;

  const chipIcon = (selDay?.precipProbMax ?? 0) <= 25 ? '🌦️' : '🌧️';
  const rainChipText =
    (selDay?.precipSum ?? 0) > 0.005
      ? `${chipIcon} ${selDay.precipSum.toFixed(2)} in expected precipitation (${selDay.precipProbMax}% peak chance)`
      : (selDay?.precipProbMax ?? 0) > 0
      ? `${(selDay?.precipProbMax ?? 0) <= 25 ? '🌦️' : '💧'} ${selDay.precipProbMax}% chance of rain`
      : '☀️ Dry conditions • 0% chance of rain';

  return (
    <div
      className="forecast-modal-overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      data-testid="forecast-modal"
    >
      <div className="forecast-modal-card" role="dialog" aria-modal="true" data-testid="forecast-modal-card">
        {/* Header */}
        <div className="modal-header">
          <div>
            <div className="modal-title" data-testid="modal-title">
              📍 {telemetry.locationName}
            </div>
            <div className="modal-subtitle">7-Day Forecast &amp; Live Precipitation Radar</div>
          </div>
          <div className="modal-header-actions">
            <button
              type="button"
              className="btn-unit-toggle"
              onClick={onToggleTempUnit}
              title="Toggle Fahrenheit / Celsius"
              data-testid="unit-toggle-btn"
            >
              °{tempUnit}
            </button>
            <button
              type="button"
              className="btn-close-modal"
              onClick={onClose}
              title="Close modal"
              data-testid="close-modal-btn"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Modal Tab Selector */}
        <div className="forecast-tab-bar" role="tablist">
          <button
            type="button"
            className={`forecast-tab-pill ${activeTab === 'outlook' ? 'active' : ''}`}
            onClick={() => setActiveTab('outlook')}
            data-testid="tab-btn-forecast-outlook"
          >
            📅 7-Day Outlook
          </button>
          <button
            type="button"
            className={`forecast-tab-pill ${activeTab === 'radar' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('radar');
              if (onOpenRadar) onOpenRadar();
            }}
            data-testid="tab-btn-forecast-radar"
          >
            📡 Live Doppler Radar
          </button>
        </div>

        {/* Modal Body */}
        <div className="modal-body">
          {activeTab === 'outlook' ? (
            <>
              {/* Hero Weather Card */}
              <div className="modal-hero-card" data-testid="modal-hero-card">
                <div className="hero-temp-row">
                  <div className="hero-temp-large" data-testid="hero-temp-large">
                    {formatTemp(telemetry.tempF)}°{tempUnit}
                  </div>
                  <div>
                    <div className="hero-cond-title" data-testid="hero-cond-title">
                      {selDay ? `${selDay.emoji} ${selDay.condition}` : telemetry.condition}
                    </div>
                    <div className="hero-cond-sub">
                      {selDay && (
                        <>
                          High {formatTemp(selDay.tempMax)}° • Low {formatTemp(selDay.tempMin)}° • Feels{' '}
                          {formatTemp(telemetry.feelsLikeF)}°
                        </>
                      )}
                    </div>
                  </div>
                </div>
                <div className="hero-chip-row">
                  <div className="hero-chip" data-testid="hero-chip">
                    {rainChipText}
                  </div>
                  <button
                    type="button"
                    className="hero-radar-btn"
                    onClick={() => {
                      setActiveTab('radar');
                      if (onOpenRadar) onOpenRadar();
                    }}
                    data-testid="forecast-open-radar-btn"
                  >
                    📡 View Live Doppler Radar →
                  </button>
                </div>
              </div>

              {/* 7-Day Outlook & Expansion Accordion */}
              <div className="modal-outlook-card" data-testid="modal-outlook-card">
                <div className="outlook-header-row">
                  <div className="outlook-title">📅 7-DAY OUTLOOK &amp; HOURLY BREAKDOWN</div>
                  <div className="outlook-sub">
                    Tap any day to expand its interactive 24-hour graph &amp; key metrics without reloading.
                  </div>
                </div>

                <div className="mat-accordion" data-testid="days-accordion">
                  {days.map((day, idx) => {
                    const isSelected = idx === selectedDayIdx;
                    return (
                      <DayForecastRow
                        key={day.dateStr || idx}
                        day={day}
                        isToday={idx === 0}
                        isSelected={isSelected}
                        onSelect={() => setSelectedDayIdx(idx)}
                        weekMin={weekMin}
                        weekMax={weekMax}
                        currentTempF={telemetry.tempF}
                        tempUnit={tempUnit}
                      >
                        <HourlyBreakdownChart day={day} tempUnit={tempUnit} />
                      </DayForecastRow>
                    );
                  })}
                </div>
              </div>
            </>
          ) : (
            <div className="modal-radar-card" data-testid="modal-radar-card">
              <div className="radar-card-header">
                <div className="radar-card-title">📡 LIVE DOPPLER RADAR COMPOSITE</div>
                <div className="radar-card-sub">
                  Past observations loop &amp; nowcast precipitation forecast for {telemetry.locationName}
                </div>
              </div>
              <RadarView
                lat={lat}
                lon={lon}
                locationName={telemetry.locationName}
                height={380}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
