export type WeatherProviderId = 'open-meteo' | 'nws' | 'met-norway' | 'openweathermap' | 'weatherapi';

export interface WeatherProviderInfo {
  id: WeatherProviderId;
  name: string;
  description: string;
  badge: string;
  requiresKey: boolean;
  keyStorageKey?: string;
  freeTierNote: string;
}

export interface HourlyForecastPoint {
  time: string;           // ISO or formatted
  hourLabel: string;      // e.g. "2 PM", "14:00"
  temp: number;           // °F
  feelsLike: number;      // °F
  humidity: number;       // %
  precipProb: number;     // %
  precipInches: number;   // in
  windSpeed: number;      // MPH
  windGust: number;       // MPH
  uvIndex: number;
  weatherCode: number;    // WMO code normalized
  condition: string;
  emoji: string;
}

export interface DailyForecast {
  dayName: string;        // e.g. "TODAY", "Mon", "Tue"
  dateStr: string;        // e.g. "2026-10-21"
  tempMin: number;        // °F
  tempMax: number;        // °F
  precipProbMax: number;  // %
  precipSum: number;      // in
  windMax: number;        // MPH
  uvMax: number;
  sunrise: string;        // e.g. "7:12 AM"
  sunset: string;         // e.g. "6:24 PM"
  condition: string;
  emoji: string;
  hourly: HourlyForecastPoint[];
}

export interface WeatherTelemetry {
  tempF: number;
  feelsLikeF: number;
  humidity: number;
  windMph: number;
  windDeg: number;
  weatherCode: number;
  isDay: boolean;
  usAqi: number;
  uvIndex: number;
  locationName: string;
  condition: string;
  hourlyTemps: number[];
  hourly: HourlyForecastPoint[];
  sevenDayDetailed: DailyForecast[];
  provider: WeatherProviderId;
  lastUpdated: number;
}

export interface WeatherAlert {
  id: string;
  event: string;
  severity: 'Extreme' | 'Severe' | 'Moderate' | 'Minor';
  urgency?: string;
  headline: string;
  description: string;
  instruction?: string;
  onset?: string;
  expires?: string;
  senderName?: string;
  source: 'NOAA_NWS' | 'WMO_METEOROLOGICAL' | 'SIMULATION';
}

export interface RadarFrame {
  time: number; // Unix timestamp (seconds)
  path: string; // e.g. "/v2/radar/..."
  isNowcast: boolean;
}

export interface RadarMetadata {
  host: string;
  past: RadarFrame[];
  nowcast: RadarFrame[];
  generated: number;
}

export interface AppSettings {
  tempUnit: 'F' | 'C' | 'BOTH';
  windUnit: 'mph' | 'kmh' | 'kn' | 'ms';
  precipUnit: 'in' | 'mm';
  timeFormat: '12h' | '24h';
  provider: WeatherProviderId;
  apiKeys: Partial<Record<WeatherProviderId, string>>;
  dioramaTheme: string;
  timeMode: string; // 'realtime' | 'day' | 'sunset' | 'night'
  radarColorScheme: number; // 1 = original, 2 = universal blue, 4 = the weather channel, 6 = nexrad
  radarOpacity: number; // 0.2 to 1.0
  alertsEnabled: boolean;
  notificationsEnabled: boolean;
  soundEnabled: boolean;
}
