import React, { useState, useEffect } from 'react';
import { WeatherTelemetry, WeatherProviderId } from '../types/weather';
import { WeatherProviderSelector } from './WeatherProviderSelector';

interface WeatherHudCardProps {
  telemetry: WeatherTelemetry | null;
  dioramaTheme: string;
  onThemeChange: (theme: string) => void;
  onSearchCity: (query: string) => void;
  onAutoLoc: () => void;
  onOpenForecast: () => void;
  tempUnit: 'F' | 'C';
  onToggleTempUnit: () => void;
  currentProvider: WeatherProviderId;
  onProviderChange: (provider: WeatherProviderId) => void;
  timeMode: string;
  onCycleTimeMode: () => void;
}

export const WeatherHudCard: React.FC<WeatherHudCardProps> = ({
  telemetry,
  dioramaTheme,
  onThemeChange,
  onSearchCity,
  onAutoLoc,
  onOpenForecast,
  tempUnit,
  onToggleTempUnit,
  currentProvider,
  onProviderChange,
  timeMode,
  onCycleTimeMode,
}) => {
  const [searchInput, setSearchInput] = useState('');
  const [clockStr, setClockStr] = useState('');

  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      setClockStr(
        now.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', second: '2-digit' })
      );
    };
    updateClock();
    const timer = setInterval(updateClock, 1000);
    return () => clearInterval(timer);
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      onSearchCity(searchInput.trim());
      setSearchInput('');
    }
  };

  if (!telemetry) {
    return (
      <div className="hud-card loading" data-testid="hud-card-loading">
        <div className="hud-condition">FETCHING METEOROLOGICAL TELEMETRY...</div>
      </div>
    );
  }

  const tempF = Math.round(telemetry.tempF);
  const tempC = Math.round(((telemetry.tempF - 32) * 5) / 9);
  const feelsF = Math.round(telemetry.feelsLikeF);
  const feelsC = Math.round(((telemetry.feelsLikeF - 32) * 5) / 9);

  return (
    <div
      className="hud-card"
      onClick={onOpenForecast}
      title="Tap to view 7-day forecast & precipitation graph"
      data-testid="hud-card"
    >
      <div className="hud-top-row">
        <div className="hud-condition" data-testid="hud-condition">
          {telemetry.condition.toUpperCase()}
        </div>
        <div className="hud-clock" data-testid="hud-clock">
          {clockStr}
        </div>
      </div>

      <div
        className="hud-temp"
        onClick={(e) => {
          e.stopPropagation();
          onToggleTempUnit();
        }}
        title="Tap to toggle °F / °C"
        data-testid="hud-temp"
      >
        {tempUnit === 'C' ? `${tempC}°C (${tempF}°F)` : `${tempF}°F (${tempC}°C)`}
      </div>

      <div className="hud-meta" data-testid="hud-humidity">
        {telemetry.humidity}% HUMIDITY
      </div>

      <div className="hud-meta" data-testid="hud-wind">
        {Math.round(telemetry.windMph)} MPH WIND
      </div>

      <div className="hud-divider" />

      <div className="hud-meta" data-testid="hud-loc">
        📍 {telemetry.locationName.toUpperCase()} • FEELS{' '}
        {tempUnit === 'C' ? `${feelsC}°C` : `${feelsF}°F`}
      </div>

      <div className="hud-badges">
        <span
          className="pill"
          style={{ color: telemetry.usAqi <= 50 ? '#4cd137' : '#fbc531' }}
          data-testid="hud-aqi"
        >
          AQI {telemetry.usAqi} {telemetry.usAqi <= 50 ? 'GOOD' : 'MOD'}
        </span>
        <span className="pill" style={{ color: '#fbc531' }} data-testid="hud-uv">
          UV {telemetry.uvIndex.toFixed(1)}
        </span>
        <button
          type="button"
          className="pill pill-interactive"
          onClick={(e) => {
            e.stopPropagation();
            onOpenForecast();
          }}
          data-testid="hud-forecast-btn"
        >
          📅 7-Day Forecast
        </button>
      </div>

      {/* Weather Provider Selector Bar */}
      <div className="hud-provider-container" onClick={(e) => e.stopPropagation()}>
        <WeatherProviderSelector
          currentProvider={currentProvider}
          onProviderChange={onProviderChange}
        />
      </div>

      {/* Controls: Search, Location, Diorama Theme, Time mode */}
      <form
        className="hud-controls"
        onSubmit={handleSearchSubmit}
        onClick={(e) => e.stopPropagation()}
        data-testid="hud-controls-form"
      >
        <input
          type="text"
          placeholder="Search any city..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="hud-input"
          data-testid="city-search-input"
        />
        <button type="submit" className="hud-btn" data-testid="city-search-btn">
          Go
        </button>
        <button
          type="button"
          onClick={onAutoLoc}
          className="hud-btn"
          title="Detect Current Computer Location"
          data-testid="auto-loc-btn"
        >
          📍 My Loc
        </button>
        <select
          value={dioramaTheme}
          onChange={(e) => onThemeChange(e.target.value)}
          className="hud-select"
          title="3D Isometric Diorama Scene (Independent of Weather Location)"
          data-testid="diorama-theme-select"
        >
          <optgroup label="Historic Virginia &amp; Mid-Atlantic (NPS)">
            <option value="yorktown">🏛️ Historic Yorktown, VA</option>
            <option value="jamestown">⛵ Historic Jamestown, VA</option>
            <option value="virginia_beach">🏖️ Virginia Beach, VA</option>
            <option value="norfolk">⚓ Norfolk &amp; Hampton Roads, VA</option>
            <option value="chesapeake_bay">🦀 Chesapeake Bay &amp; Bridge-Tunnel</option>
            <option value="washington_dc">🏛️ Washington, D.C.</option>
          </optgroup>
          <optgroup label="East Coast, South &amp; Florida">
            <option value="new_york">🗽 New York City</option>
            <option value="coastal">⚓ New England Maritime Harbor</option>
            <option value="myrtle_beach">🎡 Myrtle Beach, SC</option>
            <option value="atlanta">🏙️ Atlanta &amp; Georgia Piedmont</option>
            <option value="orlando">🏰 Orlando Theme Parks, FL</option>
            <option value="miami_caribbean">🌴 Miami Beach &amp; Tropics</option>
            <option value="new_orleans">🎷 New Orleans &amp; Gulf</option>
          </optgroup>
          <optgroup label="Heartland, Mountains &amp; American West">
            <option value="texas">🤠 Texas Hill Country &amp; Austin</option>
            <option value="chicago_great_lakes">🏙️ Chicago &amp; Great Lakes</option>
            <option value="denver">🏔️ Denver Mile High Skyline, CO</option>
            <option value="colorado_springs">⛰️ Colorado Springs &amp; Pikes Peak, CO</option>
            <option value="arizona">🌵 Arizona Sonoran Desert</option>
            <option value="grand_canyon">🏜️ Grand Canyon &amp; Southwest</option>
            <option value="las_vegas">🎰 Las Vegas Strip, NV</option>
            <option value="san_francisco">🌉 San Francisco Bay</option>
            <option value="seattle_pnw">🌲 Seattle &amp; Pacific Northwest</option>
          </optgroup>
          <optgroup label="Global Capitals &amp; Landmarks">
            <option value="london_uk">🕰️ London &amp; UK</option>
            <option value="paris_europe">🏰 Paris &amp; Western Europe</option>
            <option value="rome_mediterranean">🏛️ Rome &amp; Mediterranean</option>
            <option value="cairo_giza">🏺 Cairo &amp; Giza</option>
            <option value="tokyo_east_asia">⛩️ Tokyo &amp; East Asia</option>
            <option value="modern">🌆 Global Modern Metropolis</option>
            <option value="alpine">🏔️ Alpine Mountain Village</option>
          </optgroup>
        </select>
        <button
          type="button"
          onClick={onCycleTimeMode}
          className="hud-btn"
          title={`Cycle Time: currently ${timeMode}`}
          data-testid="cycle-time-btn"
        >
          ☀️ {timeMode === 'realtime' ? 'Time' : timeMode}
        </button>
      </form>
    </div>
  );
};
