import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AlertService } from '../services/alerts/AlertService';
import { WeatherTelemetry } from '../types/weather';

describe('AlertService', () => {
  let service: AlertService;

  beforeEach(() => {
    service = new AlertService();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('fetches and normalizes NOAA NWS alerts correctly', async () => {
    const mockNwsResponse = {
      features: [
        {
          id: 'nws-alert-123',
          properties: {
            id: 'nws-alert-123',
            event: 'Tornado Warning',
            severity: 'Extreme',
            urgency: 'Immediate',
            headline: 'Tornado Warning issued for York County',
            description: 'A severe rotation was observed by Doppler radar.',
            instruction: 'TAKE COVER NOW! Move to an interior room on lowest floor.',
            onset: '2026-10-21T03:00:00Z',
            expires: '2026-10-21T04:00:00Z',
            senderName: 'NWS Wakefield VA',
          },
        },
      ],
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockNwsResponse,
    });

    const alerts = await service.fetchAlerts(37.2388, -76.5097);
    expect(alerts.length).toBe(1);
    expect(alerts[0].id).toBe('nws-alert-123');
    expect(alerts[0].event).toBe('Tornado Warning');
    expect(alerts[0].severity).toBe('Extreme');
    expect(alerts[0].urgency).toBe('Immediate');
    expect(alerts[0].source).toBe('NOAA_NWS');
  });

  it('falls back to WMO telemetry analysis when NOAA returns empty', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ features: [] }),
    });

    const severeTelemetry: WeatherTelemetry = {
      tempF: 75,
      feelsLikeF: 75,
      weatherCode: 99, // severe tstorm
      conditionText: 'Severe Thunderstorm',
      isDay: true,
      windMph: 55,
      windDirDeg: 180,
      humidityPct: 90,
      pressureInHg: 29.8,
      uvIndex: 0,
      visibilityMiles: 3,
      cloudCoverPct: 100,
    };

    const alerts = await service.fetchAlerts(51.5074, -0.1278, severeTelemetry);
    expect(alerts.length).toBe(1);
    expect(alerts[0].event).toBe('Severe Thunderstorm Warning');
    expect(alerts[0].severity).toBe('Severe');
    expect(alerts[0].source).toBe('WMO_METEOROLOGICAL');
  });

  it('detects severe meteorological conditions across all hazard categories', () => {
    const base: WeatherTelemetry = {
      tempF: 70,
      feelsLikeF: 70,
      weatherCode: 0,
      conditionText: 'Clear',
      isDay: true,
      windMph: 10,
      windDirDeg: 90,
      humidityPct: 50,
      pressureInHg: 30.0,
      uvIndex: 5,
      visibilityMiles: 10,
      cloudCoverPct: 10,
    };

    // Normal conditions -> null
    expect(service.analyzeTelemetryForAlerts(base)).toBeNull();

    // High wind (>= 45 mph)
    const highWind = service.analyzeTelemetryForAlerts({ ...base, windMph: 50 });
    expect(highWind?.event).toBe('High Wind Warning');
    expect(highWind?.severity).toBe('Severe');

    // Wind advisory (30 - 44 mph)
    const windAdv = service.analyzeTelemetryForAlerts({ ...base, windMph: 35 });
    expect(windAdv?.event).toBe('Wind Advisory');
    expect(windAdv?.severity).toBe('Minor');

    // Heavy Snow (code 75)
    const snowAlert = service.analyzeTelemetryForAlerts({ ...base, weatherCode: 75 });
    expect(snowAlert?.event).toBe('Winter Storm Warning');
    expect(snowAlert?.severity).toBe('Severe');

    // Torrential Rain / Deluge (code 65)
    const floodAlert = service.analyzeTelemetryForAlerts({ ...base, weatherCode: 65 });
    expect(floodAlert?.event).toBe('Flash Flood Advisory');
    expect(floodAlert?.severity).toBe('Moderate');

    // Excessive Heat (>= 105 F)
    const heatAlert = service.analyzeTelemetryForAlerts({ ...base, tempF: 108 });
    expect(heatAlert?.event).toBe('Excessive Heat Warning');
    expect(heatAlert?.severity).toBe('Severe');

    // Hard Freeze (<= 10 F)
    const freezeAlert = service.analyzeTelemetryForAlerts({ ...base, tempF: 5 });
    expect(freezeAlert?.event).toBe('Hard Freeze Warning');
    expect(freezeAlert?.severity).toBe('Moderate');
  });

  it('dispatches browser notification when permission is granted and avoids duplicates', () => {
    const mockNotification = vi.fn();
    (globalThis as unknown as { Notification: unknown }).Notification = Object.assign(
      mockNotification,
      {
        permission: 'granted',
        requestPermission: vi.fn().mockResolvedValue('granted'),
      }
    );

    const alert = {
      id: 'alert-test-1',
      event: 'Severe Thunderstorm Warning',
      severity: 'Severe' as const,
      headline: 'Large hail expected',
      description: 'Take shelter immediately.',
      source: 'NOAA_NWS' as const,
    };

    service.dispatchBrowserNotification(alert);
    expect(mockNotification).toHaveBeenCalledTimes(1);

    // Calling again with same alert ID should deduplicate and not fire again
    service.dispatchBrowserNotification(alert);
    expect(mockNotification).toHaveBeenCalledTimes(1);

    // Different alert ID should trigger
    const alert2 = { ...alert, id: 'alert-test-2' };
    service.dispatchBrowserNotification(alert2);
    expect(mockNotification).toHaveBeenCalledTimes(2);
  });
});
