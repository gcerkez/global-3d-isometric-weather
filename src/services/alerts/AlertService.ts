import { WeatherAlert, WeatherTelemetry } from '../../types/weather';

export class AlertService {
  private lastDispatchedAlertId: string | null = null;

  /**
   * Fetches active severe weather alerts for a geographic point.
   * Leverages NOAA National Weather Service (api.weather.gov) for US locations,
   * with automatic meteorological condition analysis fallback for worldwide locations.
   */
  public async fetchAlerts(
    lat: number,
    lon: number,
    telemetry?: WeatherTelemetry | null
  ): Promise<WeatherAlert[]> {
    // 1. Attempt NOAA NWS API query (works for all US territories & borders)
    try {
      const url = `https://api.weather.gov/alerts/active?point=${lat.toFixed(4)},${lon.toFixed(4)}`;
      const res = await fetch(url, {
        headers: {
          Accept: 'application/geo+json',
          'User-Agent': 'Global3DIsometricWeather/0.0.6 (contact@yorktownweather.internal)',
        },
      });

      if (res.ok) {
        const data = await res.json();
        const features = data.features || [];
        if (features.length > 0) {
          return features.map((f: {
            id?: string;
            properties?: {
              id?: string;
              event?: string;
              severity?: string;
              urgency?: string;
              headline?: string;
              description?: string;
              instruction?: string;
              onset?: string;
              expires?: string;
              senderName?: string;
            };
          }) => {
            const p = f.properties || {};
            const rawSev = p.severity || 'Moderate';
            const normSev: WeatherAlert['severity'] =
              rawSev === 'Extreme' ? 'Extreme' :
              rawSev === 'Severe' ? 'Severe' :
              rawSev === 'Moderate' ? 'Moderate' : 'Minor';

            return {
              id: p.id || f.id || `nws_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
              event: p.event || 'Weather Advisory',
              severity: normSev,
              urgency: p.urgency,
              headline: p.headline || p.event || 'Severe Weather Notification',
              description: p.description || 'Active alert issued by meteorological authorities.',
              instruction: p.instruction || 'Monitor local media and follow instructions from emergency management.',
              onset: p.onset,
              expires: p.expires,
              senderName: p.senderName || 'National Weather Service',
              source: 'NOAA_NWS',
            };
          });
        }
      }
    } catch (err) {
      console.warn('[AlertService] NOAA NWS alerts query skipped or offline:', err);
    }

    // 2. Worldwide fallback: Synthesize localized alerts if telemetry shows extreme conditions
    if (telemetry) {
      const synAlert = this.analyzeTelemetryForAlerts(telemetry);
      if (synAlert) {
        return [synAlert];
      }
    }

    return [];
  }

  /**
   * Evaluates active weather parameters and WMO codes to detect hazardous meteorological events
   */
  public analyzeTelemetryForAlerts(telemetry: WeatherTelemetry): WeatherAlert | null {
    const code = telemetry.weatherCode;
    const wind = telemetry.windMph;
    const temp = telemetry.tempF;

    if (code === 99 || code === 96) {
      return {
        id: `wmo_severe_tstorm_${Date.now()}`,
        event: 'Severe Thunderstorm Warning',
        severity: 'Severe',
        urgency: 'Immediate',
        headline: 'Severe Thunderstorm with Hail in Area',
        description: 'Atmospheric convection producing frequent lightning, damaging wind gusts, and possible hail.',
        instruction: 'Move to an interior room on the lowest floor of a sturdy building. Avoid windows.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (code === 95) {
      return {
        id: `wmo_tstorm_${Date.now()}`,
        event: 'Thunderstorm Watch',
        severity: 'Moderate',
        urgency: 'Expected',
        headline: 'Thunderstorms Detected in Region',
        description: 'Scattered thunderstorms capable of gusty winds and heavy localized downpours.',
        instruction: 'Be prepared to seek shelter if thunder is heard. Stay indoors.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (wind >= 45) {
      return {
        id: `wmo_high_wind_${Date.now()}`,
        event: 'High Wind Warning',
        severity: 'Severe',
        urgency: 'Immediate',
        headline: `Damaging Winds of ${Math.round(wind)} MPH Detected`,
        description: 'Sustained strong winds and gusts capable of downing tree limbs and causing power outages.',
        instruction: 'Secure loose outdoor items. Exercise caution if driving high-profile vehicles.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (wind >= 30) {
      return {
        id: `wmo_wind_adv_${Date.now()}`,
        event: 'Wind Advisory',
        severity: 'Minor',
        urgency: 'Expected',
        headline: `Brisk Winds of ${Math.round(wind)} MPH in Effect`,
        description: 'Elevated sustained winds and choppy waters. Potential localized debris.',
        instruction: 'Use caution when traveling on bridges and open roadways.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (code === 75 || code === 86) {
      return {
        id: `wmo_heavy_snow_${Date.now()}`,
        event: 'Winter Storm Warning',
        severity: 'Severe',
        urgency: 'Expected',
        headline: 'Heavy Snowfall & Reduced Visibility',
        description: 'Significant snow accumulation and freezing conditions impacting travel and infrastructure.',
        instruction: 'Avoid non-essential travel. Keep an emergency supply kit in your vehicle.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (code === 65 || code === 82) {
      return {
        id: `wmo_heavy_rain_${Date.now()}`,
        event: 'Flash Flood Advisory',
        severity: 'Moderate',
        urgency: 'Expected',
        headline: 'Heavy Precipitation & Deluge',
        description: 'Torrential rainfall capable of causing localized ponding and flash flooding in low-lying areas.',
        instruction: 'Never drive through flooded roadways. Turn around, don\'t drown.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (temp >= 105) {
      return {
        id: `wmo_heat_${Date.now()}`,
        event: 'Excessive Heat Warning',
        severity: 'Severe',
        urgency: 'Expected',
        headline: `Extreme Heat of ${Math.round(temp)}°F Detected`,
        description: 'Dangerously hot conditions with high heat index values posing significant risk of heat exhaustion.',
        instruction: 'Drink plenty of fluids, stay in an air-conditioned room, and stay out of direct sun.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    if (temp <= 10) {
      return {
        id: `wmo_freeze_${Date.now()}`,
        event: 'Hard Freeze Warning',
        severity: 'Moderate',
        urgency: 'Expected',
        headline: `Frigid Arctic Temperatures (${Math.round(temp)}°F)`,
        description: 'Sub-freezing temperatures capable of damaging exposed pipes and vegetation.',
        instruction: 'Wrap exposed outdoor pipes and ensure pets have warm shelter.',
        source: 'WMO_METEOROLOGICAL',
      };
    }

    return null;
  }

  /**
   * Requests browser notification permission if available
   */
  public async requestNotificationPermission(): Promise<NotificationPermission> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'denied';
    }
    return Notification.requestPermission();
  }

  /**
   * Dispatches a native browser push notification for an urgent alert
   */
  public dispatchBrowserNotification(alert: WeatherAlert): void {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (Notification.permission !== 'granted') return;
    if (this.lastDispatchedAlertId === alert.id) return; // Prevent duplicate notifications

    this.lastDispatchedAlertId = alert.id;
    try {
      new Notification(`⚠️ ${alert.event}`, {
        body: alert.headline || alert.description,
        icon: '/favicon.ico',
        tag: alert.id,
      });
    } catch (err) {
      console.warn('[AlertService] Failed to trigger Notification:', err);
    }
  }
}

export const alertService = new AlertService();
