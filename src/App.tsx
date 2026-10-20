import React, { useState, useEffect, useRef, useCallback } from 'react';
import { WeatherTelemetry, WeatherProviderId } from './types/weather';
import { WeatherService } from './services/weather/WeatherService';
import { IsometricEngine } from './engine/IsometricEngine';
import { WeatherCanvas } from './components/WeatherCanvas';
import { WeatherHudCard } from './components/WeatherHudCard';
import { ForecastModal } from './components/ForecastModal';

const CITY_THEME_MAP: Record<string, string> = {
  yorktown: 'yorktown',
  jamestown: 'jamestown',
  'virginia beach': 'virginia_beach',
  norfolk: 'norfolk',
  chesapeake: 'chesapeake_bay',
  'washington dc': 'washington_dc',
  'washington, d.c.': 'washington_dc',
  washington: 'washington_dc',
  'new york': 'new_york',
  boston: 'coastal',
  'myrtle beach': 'myrtle_beach',
  atlanta: 'atlanta',
  orlando: 'orlando',
  miami: 'miami_caribbean',
  'new orleans': 'new_orleans',
  austin: 'texas',
  texas: 'texas',
  dallas: 'texas',
  houston: 'texas',
  chicago: 'chicago_great_lakes',
  denver: 'denver',
  'colorado springs': 'colorado_springs',
  phoenix: 'arizona',
  sedona: 'arizona',
  'grand canyon': 'grand_canyon',
  'las vegas': 'las_vegas',
  'san francisco': 'san_francisco',
  seattle: 'seattle_pnw',
  london: 'london_uk',
  paris: 'paris_europe',
  rome: 'rome_mediterranean',
  cairo: 'cairo_giza',
  tokyo: 'tokyo_east_asia',
  sydney: 'modern',
  zermatt: 'alpine',
  aspen: 'alpine',
};

export const App: React.FC = () => {
  const [telemetry, setTelemetry] = useState<WeatherTelemetry | null>(null);
  const [dioramaTheme, setDioramaTheme] = useState<string>('yorktown');
  const [isForecastOpen, setIsForecastOpen] = useState<boolean>(false);
  const [tempUnit, setTempUnit] = useState<'F' | 'C'>('F');
  const [timeMode, setTimeMode] = useState<string>('realtime');
  const [locationCoords, setLocationCoords] = useState<{ lat: number; lon: number; name: string }>({
    lat: 37.2388,
    lon: -76.5097,
    name: 'Yorktown, VA',
  });

  const weatherService = WeatherService.getInstance();
  const [currentProvider, setCurrentProvider] = useState<WeatherProviderId>(
    weatherService.getProvider()
  );

  const engineRef = useRef<IsometricEngine | null>(null);

  const refreshWeather = useCallback(
    async (
      lat: number = locationCoords.lat,
      lon: number = locationCoords.lon,
      name: string = locationCoords.name,
      provider: WeatherProviderId = currentProvider
    ) => {
      try {
        const data = await weatherService.fetchWeather(lat, lon, name, provider);
        setTelemetry(data);

        if (engineRef.current) {
          engineRef.current.updateTelemetry({
            locationName: data.locationName,
            latitude: lat,
            longitude: lon,
            tempF: data.tempF,
            humidity: data.humidity,
            windMph: data.windMph,
            windDeg: data.windDeg,
            weatherCode: data.weatherCode,
            isDay: data.isDay,
            sevenDayDetailed: data.sevenDayDetailed,
          });
        }
      } catch (err) {
        console.error('[App] Failed to load weather:', err);
      }
    },
    [locationCoords, currentProvider, weatherService]
  );

  useEffect(() => {
    refreshWeather();
  }, [refreshWeather]);

  const handleEngineReady = (engine: IsometricEngine) => {
    engineRef.current = engine;
    engine.setDioramaTheme(dioramaTheme);
    refreshWeather();
  };

  const handleThemeChange = (newTheme: string) => {
    setDioramaTheme(newTheme);
    if (engineRef.current) {
      engineRef.current.setDioramaTheme(newTheme);
    }
  };

  const handleProviderChange = (newProvider: WeatherProviderId) => {
    setCurrentProvider(newProvider);
    weatherService.setProvider(newProvider);
    refreshWeather(locationCoords.lat, locationCoords.lon, locationCoords.name, newProvider);
  };

  const handleSearchCity = async (query: string) => {
    try {
      const geoUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(
        query
      )}&count=1&language=en&format=json`;
      const res = await fetch(geoUrl);
      const data = await res.json();

      if (data && data.results && data.results.length > 0) {
        const place = data.results[0];
        const fullName = `${place.name}${place.admin1 ? ', ' + place.admin1 : ''}${
          place.country_code ? ' (' + place.country_code.toUpperCase() + ')' : ''
        }`;
        const newCoords = { lat: place.latitude, lon: place.longitude, name: fullName };
        setLocationCoords(newCoords);

        // Auto diorama detection
        const queryLower = query.toLowerCase();
        let matchedTheme: string | null = null;
        for (const [key, theme] of Object.entries(CITY_THEME_MAP)) {
          if (queryLower.includes(key)) {
            matchedTheme = theme;
            break;
          }
        }
        if (matchedTheme) {
          handleThemeChange(matchedTheme);
        }

        refreshWeather(newCoords.lat, newCoords.lon, newCoords.name, currentProvider);
      }
    } catch (err) {
      console.error('[App] Geocoding search failed:', err);
    }
  };

  const handleAutoLoc = () => {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          const lat = pos.coords.latitude;
          const lon = pos.coords.longitude;
          try {
            const revUrl = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}`;
            const res = await fetch(revUrl, {
              headers: { 'User-Agent': 'Global3DIsometricWeather/0.0.5' },
            });
            const data = await res.json();
            const cityName =
              data?.address?.city ||
              data?.address?.town ||
              data?.address?.village ||
              data?.address?.county ||
              'Current Location';
            const state = data?.address?.state ? `, ${data.address.state}` : '';
            const fullName = `${cityName}${state}`;
            const newCoords = { lat, lon, name: fullName };
            setLocationCoords(newCoords);
            refreshWeather(lat, lon, fullName, currentProvider);
          } catch {
            const newCoords = { lat, lon, name: 'Current Location' };
            setLocationCoords(newCoords);
            refreshWeather(lat, lon, 'Current Location', currentProvider);
          }
        },
        (err) => {
          console.warn('[App] Geolocation denied or unavailable:', err);
        }
      );
    }
  };

  const handleCycleTimeMode = () => {
    const modes = ['realtime', 'day', 'sunset', 'night'];
    const curIdx = modes.indexOf(timeMode);
    const nextMode = modes[(curIdx + 1) % modes.length];
    setTimeMode(nextMode);
    if (engineRef.current) {
      engineRef.current.state.timeMode = nextMode;
      engineRef.current.refreshAll();
    }
  };

  const toggleTempUnit = () => {
    setTempUnit((prev) => (prev === 'F' ? 'C' : 'F'));
  };

  return (
    <div className="widget-frame" data-testid="app-root">
      <WeatherCanvas
        locationName={telemetry ? telemetry.locationName : locationCoords.name}
        onEngineReady={handleEngineReady}
        onRefreshClick={() => refreshWeather()}
      />

      <WeatherHudCard
        telemetry={telemetry}
        dioramaTheme={dioramaTheme}
        onThemeChange={handleThemeChange}
        onSearchCity={handleSearchCity}
        onAutoLoc={handleAutoLoc}
        onOpenForecast={() => setIsForecastOpen(true)}
        tempUnit={tempUnit}
        onToggleTempUnit={toggleTempUnit}
        currentProvider={currentProvider}
        onProviderChange={handleProviderChange}
        timeMode={timeMode}
        onCycleTimeMode={handleCycleTimeMode}
      />

      <ForecastModal
        isOpen={isForecastOpen}
        onClose={() => setIsForecastOpen(false)}
        telemetry={telemetry}
        tempUnit={tempUnit}
        onToggleTempUnit={toggleTempUnit}
      />
    </div>
  );
};
