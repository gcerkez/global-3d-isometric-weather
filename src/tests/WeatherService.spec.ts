import { describe, it, expect, beforeEach, vi } from 'vitest';
import { WeatherService } from '../services/weather/WeatherService';
import { WEATHER_PROVIDERS } from '../services/weather/providers';

describe('WeatherService', () => {
  let service: WeatherService;

  beforeEach(() => {
    localStorage.clear();
    service = WeatherService.getInstance();
    service.setProvider('open-meteo');
    vi.restoreAllMocks();
  });

  it('initializes with default provider open-meteo', () => {
    expect(service.getProvider()).toBe('open-meteo');
  });

  it('switches and persists provider in localStorage', () => {
    service.setProvider('nws');
    expect(service.getProvider()).toBe('nws');
    expect(localStorage.getItem('selected_weather_provider')).toBe('nws');
  });

  it('stores and retrieves provider API keys securely', () => {
    service.setProviderKey('openweathermap', 'sk-owm-999');
    expect(service.getProviderKey('openweathermap')).toBe('sk-owm-999');
    expect(localStorage.getItem('owm_api_key')).toBe('sk-owm-999');
  });

  it('translates WMO weather codes to condition names and emojis', () => {
    const clearDay = service.getConditionInfo(0, true);
    expect(clearDay.name).toBe('Sunny');
    expect(clearDay.emoji).toBe('☀️');

    const clearNight = service.getConditionInfo(0, false);
    expect(clearNight.name).toBe('Clear Sky');
    expect(clearNight.emoji).toBe('🌙');

    const thunder = service.getConditionInfo(95, true);
    expect(thunder.name).toBe('Thunderstorm');
    expect(thunder.emoji).toBe('⛈️');

    const snow = service.getConditionInfo(71, true);
    expect(snow.name).toBe('Slight Snow');
    expect(snow.emoji).toBe('🌨️');
  });

  it('translates text forecast strings to WMO codes', () => {
    expect(service.forecastStringToWmo('Sunny')).toBe(0);
    expect(service.forecastStringToWmo('Partly Sunny')).toBe(2);
    expect(service.forecastStringToWmo('Scattered Showers')).toBe(61);
    expect(service.forecastStringToWmo('Severe Thunderstorms')).toBe(95);
    expect(service.forecastStringToWmo('Heavy Snow')).toBe(75);
    expect(service.forecastStringToWmo('Dense Fog')).toBe(45);
  });

  it('has comprehensive provider definitions for all 5 providers', () => {
    const providerIds = ['open-meteo', 'nws', 'met-norway', 'openweathermap', 'weatherapi'] as const;
    providerIds.forEach((id) => {
      const info = WEATHER_PROVIDERS[id];
      expect(info).toBeDefined();
      expect(info.name).toBeTruthy();
      expect(info.badge).toBeTruthy();
      expect(info.description).toBeTruthy();
    });
  });

  it('falls back seamlessly to Open-Meteo if a provider fails', async () => {
    // Mock fetch for Open-Meteo fallback
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('api.open-meteo.com')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              current: {
                temperature_2m: 68,
                apparent_temperature: 67,
                relative_humidity_2m: 55,
                wind_speed_10m: 7,
                wind_direction_10m: 180,
                is_day: 1,
                weather_code: 0,
              },
              hourly: {
                time: ['2026-10-21T00:00:00Z'],
                temperature_2m: [68],
                apparent_temperature: [67],
                relative_humidity_2m: [55],
                precipitation_probability: [10],
                precipitation: [0],
                wind_speed_10m: [7],
                wind_gusts_10m: [10],
                uv_index: [4],
                weather_code: [0],
              },
              daily: {
                time: ['2026-10-21'],
                temperature_2m_max: [72],
                temperature_2m_min: [52],
                precipitation_probability_max: [10],
                precipitation_sum: [0],
                wind_speed_10m_max: [8],
                uv_index_max: [4],
                sunrise: ['2026-10-21T07:15'],
                sunset: ['2026-10-21T18:20'],
                weather_code: [0],
              },
            }),
        });
      }
      if (url.includes('air-quality-api.open-meteo.com')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ current: { us_aqi: 25 } }),
        });
      }
      // Fail on any other provider URL to test fallback
      return Promise.reject(new Error('Network error on custom provider'));
    });

    const result = await service.fetchWeather(37.2388, -76.5097, 'Yorktown, VA', 'met-norway');
    expect(result).toBeDefined();
    expect(result.tempF).toBe(68);
    expect(result.provider).toBe('open-meteo'); // Fallback was engaged
  });
});
