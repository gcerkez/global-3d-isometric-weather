import React, { useState } from 'react';
import { WeatherProviderId } from '../types/weather';
import { WEATHER_PROVIDERS } from '../services/weather/providers';
import { WeatherService } from '../services/weather/WeatherService';

interface WeatherProviderSelectorProps {
  currentProvider: WeatherProviderId;
  onProviderChange: (providerId: WeatherProviderId) => void;
}

export const WeatherProviderSelector: React.FC<WeatherProviderSelectorProps> = ({
  currentProvider,
  onProviderChange,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [apiKeyInput, setApiKeyInput] = useState('');
  const [keySavedMessage, setKeySavedMessage] = useState(false);

  const weatherService = WeatherService.getInstance();
  const currentInfo = WEATHER_PROVIDERS[currentProvider];

  const handleSelectProvider = (id: WeatherProviderId) => {
    onProviderChange(id);
    weatherService.setProvider(id);
    const existingKey = weatherService.getProviderKey(id);
    setApiKeyInput(existingKey);
    setKeySavedMessage(false);
  };

  const handleSaveKey = () => {
    weatherService.setProviderKey(currentProvider, apiKeyInput);
    setKeySavedMessage(true);
    setTimeout(() => setKeySavedMessage(false), 2500);
  };

  return (
    <div className="provider-selector-root">
      <div className="provider-selector-header" onClick={() => setIsOpen(!isOpen)} role="button" aria-expanded={isOpen} data-testid="provider-toggle">
        <span className="provider-icon">⚡</span>
        <span className="provider-current-name" data-testid="current-provider-name">{currentInfo.name}</span>
        <span className="provider-badge" data-testid="current-provider-badge">{currentInfo.badge}</span>
        <span className="provider-arrow">{isOpen ? '▲' : '▼'}</span>
      </div>

      {isOpen && (
        <div className="provider-dropdown-panel" data-testid="provider-dropdown">
          <div className="provider-dropdown-title">Choose Weather Forecast Provider</div>
          <div className="provider-list">
            {(Object.keys(WEATHER_PROVIDERS) as WeatherProviderId[]).map((id) => {
              const info = WEATHER_PROVIDERS[id];
              const isSelected = id === currentProvider;
              return (
                <div
                  key={id}
                  className={`provider-item ${isSelected ? 'active' : ''}`}
                  onClick={() => handleSelectProvider(id)}
                  data-testid={`provider-option-${id}`}
                >
                  <div className="provider-item-top">
                    <span className="provider-item-name">{info.name}</span>
                    <span className={`provider-item-badge ${info.requiresKey ? 'badge-key' : 'badge-free'}`}>
                      {info.badge}
                    </span>
                  </div>
                  <div className="provider-item-desc">{info.description}</div>
                </div>
              );
            })}
          </div>

          {currentInfo.requiresKey && (
            <div className="provider-key-box" data-testid="provider-key-box">
              <label htmlFor="apiKeyInput" className="provider-key-label">
                Custom API Key ({currentInfo.name})
              </label>
              <div className="provider-key-row">
                <input
                  id="apiKeyInput"
                  type="password"
                  placeholder="Enter API Key (or leave blank for demo/cached simulation)"
                  value={apiKeyInput}
                  onChange={(e) => setApiKeyInput(e.target.value)}
                  className="provider-key-input"
                  data-testid="api-key-input"
                />
                <button
                  type="button"
                  onClick={handleSaveKey}
                  className="provider-key-save-btn"
                  data-testid="save-key-btn"
                >
                  Save
                </button>
              </div>
              {keySavedMessage && (
                <div className="provider-key-saved-notice" data-testid="key-saved-notice">
                  ✓ Key saved securely
                </div>
              )}
              <div className="provider-key-hint">{currentInfo.freeTierNote}</div>
            </div>
          )}

          <div className="provider-footer">
            <button
              type="button"
              className="provider-close-btn"
              onClick={() => setIsOpen(false)}
              data-testid="close-provider-dropdown"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
