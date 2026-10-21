import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SettingsModal } from '../components/SettingsModal';
import { AppSettings } from '../types/weather';

const defaultSettings: AppSettings = {
  provider: 'open-meteo',
  apiKeys: {},
  tempUnit: 'F',
  windUnit: 'mph',
  precipUnit: 'in',
  timeFormat: '12h',
  dioramaTheme: 'yorktown',
  timeMode: 'realtime',
  radarColorScheme: 1,
  radarOpacity: 0.75,
  alertsEnabled: true,
  notificationsEnabled: false,
  soundEnabled: false,
};

describe('SettingsModal Component', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <SettingsModal
        isOpen={false}
        onClose={vi.fn()}
        settings={defaultSettings}
        onUpdateSettings={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal and switches across all 6 tabs', () => {
    render(
      <SettingsModal
        isOpen={true}
        onClose={vi.fn()}
        settings={defaultSettings}
        onUpdateSettings={vi.fn()}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
      />
    );

    expect(screen.getByTestId('settings-modal')).toBeInTheDocument();
    expect(screen.getByTestId('pane-location')).toBeInTheDocument();

    // Switch to Providers
    fireEvent.click(screen.getByTestId('tab-btn-providers'));
    expect(screen.getByTestId('pane-providers')).toBeInTheDocument();

    // Switch to Units
    fireEvent.click(screen.getByTestId('tab-btn-units'));
    expect(screen.getByTestId('pane-units')).toBeInTheDocument();

    // Switch to Diorama
    fireEvent.click(screen.getByTestId('tab-btn-diorama'));
    expect(screen.getByTestId('pane-diorama')).toBeInTheDocument();

    // Switch to Radar
    fireEvent.click(screen.getByTestId('tab-btn-radar'));
    expect(screen.getByTestId('pane-radar')).toBeInTheDocument();

    // Switch to Alerts
    fireEvent.click(screen.getByTestId('tab-btn-alerts'));
    expect(screen.getByTestId('pane-alerts')).toBeInTheDocument();
  });

  it('handles city search and GPS autolocate in Location tab', () => {
    const handleSearch = vi.fn();
    const handleAutoLoc = vi.fn();
    const handleClose = vi.fn();

    render(
      <SettingsModal
        isOpen={true}
        onClose={handleClose}
        settings={defaultSettings}
        onUpdateSettings={vi.fn()}
        onSearchCity={handleSearch}
        onAutoLoc={handleAutoLoc}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
      />
    );

    // Search input
    const input = screen.getByTestId('settings-search-input');
    fireEvent.change(input, { target: { value: 'Denver' } });
    fireEvent.click(screen.getByTestId('settings-search-submit'));
    expect(handleSearch).toHaveBeenCalledWith('Denver');
    expect(handleClose).toHaveBeenCalledTimes(1);

    // Auto GPS button
    fireEvent.click(screen.getByTestId('settings-autoloc-btn'));
    expect(handleAutoLoc).toHaveBeenCalledTimes(1);
  });

  it('handles provider selection and API key saving', () => {
    const handleUpdate = vi.fn();

    render(
      <SettingsModal
        isOpen={true}
        onClose={vi.fn()}
        settings={defaultSettings}
        onUpdateSettings={handleUpdate}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
      />
    );

    // Switch to providers tab
    fireEvent.click(screen.getByTestId('tab-btn-providers'));

    // Select WeatherAPI provider
    fireEvent.click(screen.getByTestId('provider-card-weatherapi'));
    expect(handleUpdate).toHaveBeenCalledWith({ provider: 'weatherapi' });

    // Save API key
    const keyInput = screen.getByTestId('provider-api-key-input');
    fireEvent.change(keyInput, { target: { value: 'test_token_123' } });
    fireEvent.click(screen.getByTestId('save-api-key-btn'));
    expect(handleUpdate).toHaveBeenCalledWith({
      apiKeys: { 'open-meteo': 'test_token_123' },
    });
  });

  it('handles unit toggling and diorama selection', () => {
    const handleUpdate = vi.fn();

    render(
      <SettingsModal
        isOpen={true}
        onClose={vi.fn()}
        settings={defaultSettings}
        onUpdateSettings={handleUpdate}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
      />
    );

    // Units Tab
    fireEvent.click(screen.getByTestId('tab-btn-units'));
    fireEvent.click(screen.getByTestId('temp-unit-c'));
    expect(handleUpdate).toHaveBeenCalledWith({ tempUnit: 'C' });

    // Diorama Tab
    fireEvent.click(screen.getByTestId('tab-btn-diorama'));
    const dioramaSelect = screen.getByTestId('settings-diorama-select');
    fireEvent.change(dioramaSelect, { target: { value: 'norfolk' } });
    expect(handleUpdate).toHaveBeenCalledWith({ dioramaTheme: 'norfolk' });
  });

  it('handles alerts settings toggles and request notifications callback', () => {
    const handleUpdate = vi.fn();
    const handleRequestNotifications = vi.fn();

    render(
      <SettingsModal
        isOpen={true}
        onClose={vi.fn()}
        settings={defaultSettings}
        onUpdateSettings={handleUpdate}
        onSearchCity={vi.fn()}
        onAutoLoc={vi.fn()}
        currentLocationName="Yorktown, VA"
        currentCoords={{ lat: 37.2388, lon: -76.5097 }}
        onRequestNotifications={handleRequestNotifications}
        notificationPermission="default"
      />
    );

    // Alerts Tab
    fireEvent.click(screen.getByTestId('tab-btn-alerts'));

    // Toggle Notifications
    const notifCheckbox = screen.getByTestId('settings-notifications-enabled-checkbox');
    fireEvent.click(notifCheckbox);
    expect(handleUpdate).toHaveBeenCalledWith({ notificationsEnabled: true });

    // Request permissions button
    const permBtn = screen.getByTestId('request-notification-permission-btn');
    fireEvent.click(permBtn);
    expect(handleRequestNotifications).toHaveBeenCalledTimes(1);
  });
});
