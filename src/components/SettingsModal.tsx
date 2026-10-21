import React, { useState, useEffect } from 'react';
import { AppSettings } from '../types/weather';
import { WEATHER_PROVIDERS } from '../services/weather/providers';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: AppSettings;
  onUpdateSettings: (newSettings: Partial<AppSettings>) => void;
  onSearchCity: (query: string) => void;
  onAutoLoc: () => void;
  currentLocationName: string;
  currentCoords: { lat: number; lon: number };
  onRequestNotifications?: () => Promise<void>;
  notificationPermission?: NotificationPermission;
}

const PRESET_CITIES = [
  { name: 'Yorktown, VA', lat: 37.2388, lon: -76.5097, theme: 'yorktown' },
  { name: 'Jamestown, VA', lat: 37.2115, lon: -76.7788, theme: 'jamestown' },
  { name: 'Norfolk, VA', lat: 36.8508, lon: -76.2859, theme: 'norfolk' },
  { name: 'Virginia Beach, VA', lat: 36.8529, lon: -75.978, theme: 'virginia_beach' },
  { name: 'Washington, D.C.', lat: 38.9072, lon: -77.0369, theme: 'washington_dc' },
  { name: 'New York City, NY', lat: 40.7128, lon: -74.006, theme: 'new_york' },
  { name: 'Atlanta, GA', lat: 33.749, lon: -84.388, theme: 'atlanta' },
  { name: 'Orlando, FL', lat: 28.5383, lon: -81.3792, theme: 'orlando' },
  { name: 'Miami, FL', lat: 25.7617, lon: -80.1918, theme: 'miami_caribbean' },
  { name: 'Chicago, IL', lat: 41.8781, lon: -87.6298, theme: 'chicago_great_lakes' },
  { name: 'Denver, CO', lat: 39.7392, lon: -104.9903, theme: 'denver' },
  { name: 'Colorado Springs, CO', lat: 38.8339, lon: -104.8214, theme: 'colorado_springs' },
  { name: 'Seattle, WA', lat: 47.6062, lon: -122.3321, theme: 'seattle_pnw' },
  { name: 'San Francisco, CA', lat: 37.7749, lon: -122.4194, theme: 'san_francisco' },
  { name: 'London, UK', lat: 51.5074, lon: -0.1278, theme: 'london_uk' },
  { name: 'Paris, France', lat: 48.8566, lon: 2.3522, theme: 'paris_europe' },
  { name: 'Tokyo, Japan', lat: 35.6762, lon: 139.6503, theme: 'tokyo_east_asia' },
  { name: 'Cairo, Egypt', lat: 30.0444, lon: 31.2357, theme: 'cairo_giza' },
];

const DIORAMA_OPTIONS = [
  { id: 'yorktown', label: 'Historic Yorktown, VA' },
  { id: 'jamestown', label: 'Jamestown Settlement, VA' },
  { id: 'norfolk', label: 'Norfolk Naval Harbor, VA' },
  { id: 'virginia_beach', label: 'Virginia Beach Oceanfront' },
  { id: 'chesapeake_bay', label: 'Chesapeake Bay Maritime' },
  { id: 'washington_dc', label: 'Washington, D.C. National Mall' },
  { id: 'new_york', label: 'New York City Manhattan' },
  { id: 'myrtle_beach', label: 'Myrtle Beach Coastal' },
  { id: 'atlanta', label: 'Atlanta & Piedmont Park' },
  { id: 'orlando', label: 'Orlando Theme Park Resort' },
  { id: 'miami_caribbean', label: 'Miami Beach & Ocean Drive' },
  { id: 'new_orleans', label: 'New Orleans French Quarter' },
  { id: 'texas', label: 'Texas Hill Country & Austin' },
  { id: 'chicago_great_lakes', label: 'Chicago Loop & Lake Michigan' },
  { id: 'denver', label: 'Denver Mile High City' },
  { id: 'colorado_springs', label: 'Colorado Springs & Pikes Peak' },
  { id: 'arizona', label: 'Arizona Sonoran Desert' },
  { id: 'grand_canyon', label: 'Grand Canyon Red Rock' },
  { id: 'las_vegas', label: 'Las Vegas Strip' },
  { id: 'san_francisco', label: 'San Francisco Golden Gate' },
  { id: 'seattle_pnw', label: 'Seattle & PNW Evergreen' },
  { id: 'coastal', label: 'Coastal Harbor & Lighthouse' },
  { id: 'london_uk', label: 'London & Westminster' },
  { id: 'paris_europe', label: 'Paris & Seine River' },
  { id: 'rome_mediterranean', label: 'Rome & Mediterranean' },
  { id: 'cairo_giza', label: 'Cairo & Giza Plateau' },
  { id: 'tokyo_east_asia', label: 'Tokyo Shinjuku Metropolis' },
  { id: 'alpine', label: 'Alpine Mountain Chalet' },
  { id: 'modern', label: 'Global Metropolis 3D' },
];

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
  onSearchCity,
  onAutoLoc,
  currentLocationName,
  currentCoords,
  onRequestNotifications,
  notificationPermission = 'default',
}) => {
  const [activeTab, setActiveTab] = useState<number>(0);
  const [searchInput, setSearchInput] = useState<string>('');
  const [customLat, setCustomLat] = useState<string>(currentCoords.lat.toString());
  const [customLon, setCustomLon] = useState<string>(currentCoords.lon.toString());
  const [keyInput, setKeyInput] = useState<string>(
    settings.apiKeys[settings.provider] || ''
  );
  const [keySavedNotice, setKeySavedNotice] = useState<boolean>(false);

  useEffect(() => {
    setCustomLat(currentCoords.lat.toFixed(4));
    setCustomLon(currentCoords.lon.toFixed(4));
  }, [currentCoords]);

  useEffect(() => {
    setKeyInput(settings.apiKeys[settings.provider] || '');
  }, [settings.provider, settings.apiKeys]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      onSearchCity(searchInput.trim());
      setSearchInput('');
      onClose();
    }
  };

  const handleApplyCoords = (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(customLat);
    const lon = parseFloat(customLon);
    if (!isNaN(lat) && !isNaN(lon)) {
      onSearchCity(`${lat.toFixed(4)}, ${lon.toFixed(4)}`);
      onClose();
    }
  };

  const handleSaveKey = () => {
    const updatedKeys = { ...settings.apiKeys, [settings.provider]: keyInput.trim() };
    onUpdateSettings({ apiKeys: updatedKeys });
    if (typeof window !== 'undefined') {
      localStorage.setItem(`weather_key_${settings.provider}`, keyInput.trim());
    }
    setKeySavedNotice(true);
    setTimeout(() => setKeySavedNotice(false), 2500);
  };

  return (
    <div className="modal-backdrop" onClick={onClose} data-testid="settings-modal-backdrop">
      <div
        className="modal-window settings-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        data-testid="settings-modal"
      >
        {/* Header */}
        <div className="modal-header">
          <div className="settings-header-title">
            <span className="settings-icon">⚙️</span>
            <h2 className="modal-title" data-testid="settings-modal-title">
              Configuration & Settings
            </h2>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close Settings"
            data-testid="close-settings-modal-btn"
          >
            ✕
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="settings-tab-bar" role="tablist">
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 0 ? 'active' : ''}`}
            onClick={() => setActiveTab(0)}
            role="tab"
            aria-selected={activeTab === 0}
            data-testid="tab-btn-location"
          >
            📍 Location
          </button>
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 1 ? 'active' : ''}`}
            onClick={() => setActiveTab(1)}
            role="tab"
            aria-selected={activeTab === 1}
            data-testid="tab-btn-providers"
          >
            🛰️ Providers
          </button>
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 2 ? 'active' : ''}`}
            onClick={() => setActiveTab(2)}
            role="tab"
            aria-selected={activeTab === 2}
            data-testid="tab-btn-units"
          >
            📏 Units
          </button>
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 3 ? 'active' : ''}`}
            onClick={() => setActiveTab(3)}
            role="tab"
            aria-selected={activeTab === 3}
            data-testid="tab-btn-diorama"
          >
            🏛️ Diorama
          </button>
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 4 ? 'active' : ''}`}
            onClick={() => setActiveTab(4)}
            role="tab"
            aria-selected={activeTab === 4}
            data-testid="tab-btn-radar"
          >
            📡 Radar
          </button>
          <button
            type="button"
            className={`settings-tab-btn ${activeTab === 5 ? 'active' : ''}`}
            onClick={() => setActiveTab(5)}
            role="tab"
            aria-selected={activeTab === 5}
            data-testid="tab-btn-alerts"
          >
            🚨 Alerts
          </button>
        </div>

        {/* Tab Body */}
        <div className="settings-body">
          {/* TAB 0: Location & Presets */}
          {activeTab === 0 && (
            <div className="settings-tab-pane" data-testid="pane-location">
              <div className="setting-section">
                <div className="setting-section-title">ACTIVE WEATHER LOCATION</div>
                <div className="current-location-card">
                  <div className="location-name-large">📍 {currentLocationName}</div>
                  <div className="location-coords-small">
                    Coordinates: {currentCoords.lat.toFixed(4)}°, {currentCoords.lon.toFixed(4)}°
                  </div>
                  <button
                    type="button"
                    className="btn-gps"
                    onClick={() => {
                      onAutoLoc();
                      onClose();
                    }}
                    data-testid="settings-autoloc-btn"
                  >
                    🎯 Use Current Device GPS
                  </button>
                </div>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">SEARCH CITY OR ZIP</div>
                <form onSubmit={handleSearchSubmit} className="search-form">
                  <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    placeholder="e.g. Yorktown, Virginia Beach, London, Tokyo..."
                    className="settings-text-input"
                    data-testid="settings-search-input"
                  />
                  <button type="submit" className="btn-primary" data-testid="settings-search-submit">
                    Search
                  </button>
                </form>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">MANUAL COORDINATES</div>
                <form onSubmit={handleApplyCoords} className="coords-form">
                  <div className="coord-input-pair">
                    <label>Latitude:</label>
                    <input
                      type="text"
                      value={customLat}
                      onChange={(e) => setCustomLat(e.target.value)}
                      className="settings-text-input small"
                    />
                  </div>
                  <div className="coord-input-pair">
                    <label>Longitude:</label>
                    <input
                      type="text"
                      value={customLon}
                      onChange={(e) => setCustomLon(e.target.value)}
                      className="settings-text-input small"
                    />
                  </div>
                  <button type="submit" className="btn-secondary" data-testid="settings-coords-submit">
                    Apply Coords
                  </button>
                </form>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">FAVORITE PRESET CITIES</div>
                <div className="preset-cities-grid">
                  {PRESET_CITIES.map((city) => (
                    <button
                      key={city.name}
                      type="button"
                      className="preset-city-chip"
                      onClick={() => {
                        onSearchCity(city.name);
                        onUpdateSettings({ dioramaTheme: city.theme });
                        onClose();
                      }}
                    >
                      {city.name}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 1: Weather Providers */}
          {activeTab === 1 && (
            <div className="settings-tab-pane" data-testid="pane-providers">
              <div className="setting-section">
                <div className="setting-section-title">METEOROLOGICAL PROVIDER ENGINE</div>
                <div className="providers-list">
                  {Object.values(WEATHER_PROVIDERS).map((p) => {
                    const isSelected = settings.provider === p.id;
                    return (
                      <div
                        key={p.id}
                        className={`provider-card-selectable ${isSelected ? 'selected' : ''}`}
                        onClick={() => onUpdateSettings({ provider: p.id })}
                        data-testid={`provider-card-${p.id}`}
                      >
                        <div className="provider-header-row">
                          <span className="provider-name">{p.name}</span>
                          <span className="provider-badge-chip">{p.badge}</span>
                        </div>
                        <div className="provider-desc">{p.description}</div>
                        <div className="provider-tier-note">{p.freeTierNote}</div>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">
                  CUSTOM API TOKEN ({WEATHER_PROVIDERS[settings.provider]?.name || settings.provider})
                </div>
                <div className="api-key-box">
                  <input
                    type="password"
                    value={keyInput}
                    onChange={(e) => setKeyInput(e.target.value)}
                    placeholder="Enter your personal API key / token (Optional for free providers)"
                    className="settings-text-input"
                    data-testid="provider-api-key-input"
                  />
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={handleSaveKey}
                    data-testid="save-api-key-btn"
                  >
                    Save Key
                  </button>
                </div>
                {keySavedNotice && (
                  <div className="saved-success-msg">✓ API Key saved to local storage!</div>
                )}
              </div>
            </div>
          )}

          {/* TAB 2: Units & Display */}
          {activeTab === 2 && (
            <div className="settings-tab-pane" data-testid="pane-units">
              <div className="setting-section">
                <div className="setting-section-title">TEMPERATURE UNIT</div>
                <div className="button-group-segment">
                  <button
                    type="button"
                    className={`segment-btn ${settings.tempUnit === 'F' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ tempUnit: 'F' })}
                    data-testid="temp-unit-f"
                  >
                    Fahrenheit (°F)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.tempUnit === 'C' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ tempUnit: 'C' })}
                    data-testid="temp-unit-c"
                  >
                    Celsius (°C)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.tempUnit === 'BOTH' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ tempUnit: 'BOTH' })}
                    data-testid="temp-unit-both"
                  >
                    Dual (°F + °C)
                  </button>
                </div>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">WIND SPEED UNIT</div>
                <div className="button-group-segment">
                  <button
                    type="button"
                    className={`segment-btn ${settings.windUnit === 'mph' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ windUnit: 'mph' })}
                  >
                    MPH (Miles/hr)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.windUnit === 'kmh' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ windUnit: 'kmh' })}
                  >
                    KM/H (Kilometers/hr)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.windUnit === 'kn' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ windUnit: 'kn' })}
                  >
                    Knots (kn)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.windUnit === 'ms' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ windUnit: 'ms' })}
                  >
                    M/S (Meters/sec)
                  </button>
                </div>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">PRECIPITATION UNIT</div>
                <div className="button-group-segment">
                  <button
                    type="button"
                    className={`segment-btn ${settings.precipUnit === 'in' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ precipUnit: 'in' })}
                  >
                    Inches (in)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.precipUnit === 'mm' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ precipUnit: 'mm' })}
                  >
                    Millimeters (mm)
                  </button>
                </div>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">CLOCK FORMAT</div>
                <div className="button-group-segment">
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeFormat === '12h' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeFormat: '12h' })}
                  >
                    12-Hour (1:30 PM)
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeFormat === '24h' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeFormat: '24h' })}
                  >
                    24-Hour (13:30)
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: 3D Diorama & Simulation */}
          {activeTab === 3 && (
            <div className="settings-tab-pane" data-testid="pane-diorama">
              <div className="setting-section">
                <div className="setting-section-title">3D ISOMETRIC DIORAMA MODEL (28 PRESETS)</div>
                <select
                  value={settings.dioramaTheme}
                  onChange={(e) => onUpdateSettings({ dioramaTheme: e.target.value })}
                  className="settings-select-full"
                  data-testid="settings-diorama-select"
                >
                  {DIORAMA_OPTIONS.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">SOLAR CYCLE & LIGHTING OVERRIDE</div>
                <div className="button-group-segment">
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeMode === 'realtime' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeMode: 'realtime' })}
                  >
                    ☀️ Realtime Solar
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeMode === 'day' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeMode: 'day' })}
                  >
                    🌞 Daylight Noon
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeMode === 'sunset' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeMode: 'sunset' })}
                  >
                    🌅 Golden Sunset
                  </button>
                  <button
                    type="button"
                    className={`segment-btn ${settings.timeMode === 'night' ? 'active' : ''}`}
                    onClick={() => onUpdateSettings({ timeMode: 'night' })}
                  >
                    🌙 Night Sky
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: Live Radar Display */}
          {activeTab === 4 && (
            <div className="settings-tab-pane" data-testid="pane-radar">
              <div className="setting-section">
                <div className="setting-section-title">RADAR COLOR PALETTE</div>
                <select
                  value={settings.radarColorScheme}
                  onChange={(e) => onUpdateSettings({ radarColorScheme: Number(e.target.value) })}
                  className="settings-select-full"
                  data-testid="settings-radar-palette-select"
                >
                  <option value={1}>Original (Universal Green/Yellow/Red)</option>
                  <option value={2}>Universal Blue</option>
                  <option value={4}>The Weather Channel</option>
                  <option value={6}>NEXRAD Level-III</option>
                </select>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">
                  RADAR TILE OPACITY: {Math.round(settings.radarOpacity * 100)}%
                </div>
                <input
                  type="range"
                  min={0.2}
                  max={1.0}
                  step={0.05}
                  value={settings.radarOpacity}
                  onChange={(e) => onUpdateSettings({ radarOpacity: Number(e.target.value) })}
                  className="settings-slider"
                  data-testid="settings-radar-opacity-slider"
                />
              </div>
            </div>
          )}

          {/* TAB 5: Alerts & Notifications */}
          {activeTab === 5 && (
            <div className="settings-tab-pane" data-testid="pane-alerts">
              <div className="setting-section">
                <div className="setting-section-title">SEVERE WEATHER MONITORING</div>
                <label className="checkbox-setting-row">
                  <input
                    type="checkbox"
                    checked={settings.alertsEnabled}
                    onChange={(e) => onUpdateSettings({ alertsEnabled: e.target.checked })}
                    data-testid="settings-alerts-enabled-checkbox"
                  />
                  <div className="checkbox-label-content">
                    <span className="checkbox-title">Enable Active Weather Event Warnings</span>
                    <span className="checkbox-subtitle">
                      Monitor NOAA NWS active alerts and display urgent warning banners for thunderstorms, tornadoes, floods, and blizzards.
                    </span>
                  </div>
                </label>
              </div>

              <div className="setting-section">
                <div className="setting-section-title">DESKTOP PUSH NOTIFICATIONS</div>
                <label className="checkbox-setting-row">
                  <input
                    type="checkbox"
                    checked={settings.notificationsEnabled}
                    onChange={(e) => onUpdateSettings({ notificationsEnabled: e.target.checked })}
                    data-testid="settings-notifications-enabled-checkbox"
                  />
                  <div className="checkbox-label-content">
                    <span className="checkbox-title">Send Browser Desktop Notifications</span>
                    <span className="checkbox-subtitle">
                      Push an immediate desktop notification when an Extreme or Severe alert is detected.
                    </span>
                  </div>
                </label>

                {onRequestNotifications && (
                  <div className="notification-permission-box">
                    <span>Permission Status: <strong>{notificationPermission.toUpperCase()}</strong></span>
                    {notificationPermission !== 'granted' && (
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={onRequestNotifications}
                        data-testid="request-notification-permission-btn"
                      >
                        Request Permission
                      </button>
                    )}
                  </div>
                )}
              </div>

              <div className="setting-section">
                <div className="setting-section-title">AUDITORY CHIMES</div>
                <label className="checkbox-setting-row">
                  <input
                    type="checkbox"
                    checked={settings.soundEnabled}
                    onChange={(e) => onUpdateSettings({ soundEnabled: e.target.checked })}
                  />
                  <div className="checkbox-label-content">
                    <span className="checkbox-title">Play Warning Chime</span>
                    <span className="checkbox-subtitle">
                      Play a subtle synthesized chime when severe weather strikes.
                    </span>
                  </div>
                </label>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="modal-footer">
          <button
            type="button"
            className="btn-primary"
            onClick={onClose}
            data-testid="settings-done-btn"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
