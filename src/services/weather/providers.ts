import { WeatherProviderId, WeatherProviderInfo } from '../../types/weather';

export const WEATHER_PROVIDERS: Record<WeatherProviderId, WeatherProviderInfo> = {
  'open-meteo': {
    id: 'open-meteo',
    name: 'Open-Meteo',
    description: 'High-resolution global weather models (ECMWF, GFS, ICON). Free, no API key needed.',
    badge: 'FREE & OPEN',
    requiresKey: false,
    freeTierNote: 'Completely free & open access with 15-minute refresh and global hourly accuracy.',
  },
  'nws': {
    id: 'nws',
    name: 'National Weather Service (NOAA)',
    description: 'Official US federal government meteorological service (api.weather.gov).',
    badge: 'OFFICIAL US GOV',
    requiresKey: false,
    freeTierNote: 'Direct official NOAA radar & station gridpoint forecast for all US cities and territories.',
  },
  'met-norway': {
    id: 'met-norway',
    name: 'MET Norway',
    description: 'Norwegian Meteorological Institute & NRK global location forecast (api.met.no).',
    badge: 'EU / GLOBAL FREE',
    requiresKey: false,
    freeTierNote: 'Authoritative European and Arctic global numerical weather model, completely open.',
  },
  'openweathermap': {
    id: 'openweathermap',
    name: 'OpenWeatherMap',
    description: 'Popular worldwide commercial weather API (OneCall / 2.5 data feeds).',
    badge: 'KEY OR DEMO',
    requiresKey: true,
    keyStorageKey: 'owm_api_key',
    freeTierNote: 'Supports custom API key, or automatically uses cached/demo simulation when key is empty.',
  },
  'weatherapi': {
    id: 'weatherapi',
    name: 'WeatherAPI.com',
    description: 'Comprehensive global weather, air quality and sports forecast service.',
    badge: 'KEY OR DEMO',
    requiresKey: true,
    keyStorageKey: 'weatherapi_key',
    freeTierNote: 'Supports custom WeatherAPI token, or automatically uses cached/demo simulation when key is empty.',
  },
};
