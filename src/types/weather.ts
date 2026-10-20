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
