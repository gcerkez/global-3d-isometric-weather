import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  WeatherTelemetry,
  WeatherProviderId,
  AppSettings,
  WeatherAlert,
} from './types/weather';
import { WeatherService } from './services/weather/WeatherService';
import { alertService } from './services/alerts/AlertService';
import { IsometricEngine } from './engine/IsometricEngine';
import { WeatherCanvas } from './components/WeatherCanvas';
import { WeatherHudCard } from './components/WeatherHudCard';
import { ForecastModal } from './components/ForecastModal';
import { RadarModal } from './components/RadarModal';
import { AlertDetailsModal } from './components/AlertDetailsModal';
import { WeatherAlertsBanner } from './components/WeatherAlertsBanner';
import { SettingsModal } from './components/SettingsModal';

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
  const [activeAlerts, setActiveAlerts] = useState<WeatherAlert[]>([]);
  const [selectedAlert, setSelectedAlert] = useState<WeatherAlert | null>(null);

  // Modals state
  const [isForecastOpen, setIsForecastOpen] = useState<boolean>(false);
  const [isRadarOpen, setIsRadarOpen] = useState<boolean>(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);

  // Geolocation & Coordinates
  const [locationCoords, setLocationCoords] = useState<{ lat: number; lon: number; name: string }>({
    lat: 37.2388,
    lon: -76.5097,
    name: 'Yorktown, VA',
  });

  const [notificationPermission, setNotificationPermission] = useState<NotificationPermission>(() => {
    return typeof window !== 'undefined' && 'Notification' in window
      ? Notification.permission
      : 'default';
  });

  // App Settings state (persisted across sessions via localStorage)
  const [settings, setSettings] = useState<AppSettings>(() => {
    const savedTheme = (typeof window !== 'undefined' && localStorage.getItem('diorama_theme')) || 'yorktown';
    const savedProvider = ((typeof window !== 'undefined' && localStorage.getItem('weather_provider')) as WeatherProviderId) || 'open-meteo';
    const savedTempUnit = ((typeof window !== 'undefined' && localStorage.getItem('temp_unit')) as 'F' | 'C' | 'BOTH') || 'BOTH';
    const savedWindUnit = ((typeof window !== 'undefined' && localStorage.getItem('wind_unit')) as 'mph' | 'kmh' | 'kn' | 'ms') || 'mph';
    const savedPrecipUnit = ((typeof window !== 'undefined' && localStorage.getItem('precip_unit')) as 'in' | 'mm') || 'in';
    const savedTimeFormat = ((typeof window !== 'undefined' && localStorage.getItem('time_format')) as '12h' | '24h') || '12h';
    const savedTimeMode = (typeof window !== 'undefined' && localStorage.getItem('time_mode')) || 'realtime';
    const savedRadarScheme = Number((typeof window !== 'undefined' && localStorage.getItem('radar_scheme')) || '1');
    const savedRadarOpacity = Number((typeof window !== 'undefined' && localStorage.getItem('radar_opacity')) || '0.75');
    const savedAlertsEnabled = (typeof window !== 'undefined' && localStorage.getItem('alerts_enabled')) !== 'false';
    const savedNotifs = (typeof window !== 'undefined' && localStorage.getItem('notifications_enabled')) === 'true';
    const savedSound = (typeof window !== 'undefined' && localStorage.getItem('sound_enabled')) === 'true';

    return {
      tempUnit: savedTempUnit,
      windUnit: savedWindUnit,
      precipUnit: savedPrecipUnit,
      timeFormat: savedTimeFormat,
      provider: savedProvider,
      apiKeys: {},
      dioramaTheme: savedTheme,
      timeMode: savedTimeMode,
      radarColorScheme: savedRadarScheme,
      radarOpacity: savedRadarOpacity,
      alertsEnabled: savedAlertsEnabled,
      notificationsEnabled: savedNotifs,
      soundEnabled: savedSound,
    };
  });

  const weatherService = WeatherService.getInstance();
  const engineRef = useRef<IsometricEngine | null>(null);

  const refreshWeather = useCallback(
    async (
      lat: number = locationCoords.lat,
      lon: number = locationCoords.lon,
      name: string = locationCoords.name,
      provider: WeatherProviderId = settings.provider
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

        // Fetch active severe weather alerts
        const alerts = await alertService.fetchAlerts(lat, lon, data);
        setActiveAlerts(alerts);

        if (settings.notificationsEnabled && alerts.length > 0) {
          const topAlert = alerts[0];
          if (topAlert.severity === 'Extreme' || topAlert.severity === 'Severe') {
            alertService.dispatchBrowserNotification(topAlert);
          }
        }
      } catch (err) {
        console.error('[App] Failed to load weather:', err);
      }
    },
    [locationCoords, settings.provider, settings.notificationsEnabled, weatherService]
  );

  useEffect(() => {
    refreshWeather();
  }, [refreshWeather]);

  const handleEngineReady = (engine: IsometricEngine) => {
    engineRef.current = engine;
    engine.setDioramaTheme(settings.dioramaTheme);
    (engine as unknown as { setWeatherProvider?: (p: WeatherProviderId) => void }).setWeatherProvider = (p: WeatherProviderId) => {
      handleUpdateSettings({ provider: p });
    };
    refreshWeather();
  };

  const handleUpdateSettings = (partial: Partial<AppSettings>) => {
    setSettings((prev) => {
      const next = { ...prev, ...partial };

      if (partial.dioramaTheme !== undefined) {
        localStorage.setItem('diorama_theme', partial.dioramaTheme);
        if (engineRef.current) engineRef.current.setDioramaTheme(partial.dioramaTheme);
      }
      if (partial.timeMode !== undefined) {
        localStorage.setItem('time_mode', partial.timeMode);
        if (engineRef.current) {
          engineRef.current.state.timeMode = partial.timeMode;
          engineRef.current.refreshAll();
        }
      }
      if (partial.provider !== undefined && partial.provider !== prev.provider) {
        localStorage.setItem('weather_provider', partial.provider);
        weatherService.setProvider(partial.provider);
        refreshWeather(locationCoords.lat, locationCoords.lon, locationCoords.name, partial.provider);
      }
      if (partial.tempUnit !== undefined) {
        localStorage.setItem('temp_unit', partial.tempUnit);
      }
      if (partial.windUnit !== undefined) {
        localStorage.setItem('wind_unit', partial.windUnit);
      }
      if (partial.precipUnit !== undefined) {
        localStorage.setItem('precip_unit', partial.precipUnit);
      }
      if (partial.timeFormat !== undefined) {
        localStorage.setItem('time_format', partial.timeFormat);
      }
      if (partial.radarColorScheme !== undefined) {
        localStorage.setItem('radar_scheme', String(partial.radarColorScheme));
      }
      if (partial.radarOpacity !== undefined) {
        localStorage.setItem('radar_opacity', String(partial.radarOpacity));
      }
      if (partial.alertsEnabled !== undefined) {
        localStorage.setItem('alerts_enabled', String(partial.alertsEnabled));
      }
      if (partial.notificationsEnabled !== undefined) {
        localStorage.setItem('notifications_enabled', String(partial.notificationsEnabled));
      }
      if (partial.soundEnabled !== undefined) {
        localStorage.setItem('sound_enabled', String(partial.soundEnabled));
      }

      return next;
    });
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
          handleUpdateSettings({ dioramaTheme: matchedTheme });
        }

        refreshWeather(newCoords.lat, newCoords.lon, newCoords.name, settings.provider);
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
              headers: { 'User-Agent': 'Global3DIsometricWeather/0.0.6' },
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
            refreshWeather(lat, lon, fullName, settings.provider);
          } catch {
            const newCoords = { lat, lon, name: 'Current Location' };
            setLocationCoords(newCoords);
            refreshWeather(lat, lon, 'Current Location', settings.provider);
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
    const curIdx = modes.indexOf(settings.timeMode);
    const nextMode = modes[(curIdx + 1) % modes.length];
    handleUpdateSettings({ timeMode: nextMode });
  };

  const toggleTempUnit = () => {
    const next = settings.tempUnit === 'F' ? 'C' : settings.tempUnit === 'C' ? 'BOTH' : 'F';
    handleUpdateSettings({ tempUnit: next });
  };

  const handleRequestNotifications = async () => {
    const perm = await alertService.requestNotificationPermission();
    setNotificationPermission(perm);
    if (perm === 'granted') {
      handleUpdateSettings({ notificationsEnabled: true });
    }
  };

  return (
    <div className="widget-frame" data-testid="app-root">
      <WeatherCanvas
        locationName={telemetry ? telemetry.locationName : locationCoords.name}
        onEngineReady={handleEngineReady}
        onRefreshClick={() => refreshWeather()}
      />

      <div className="hud-column">
        <WeatherHudCard
          telemetry={telemetry}
          dioramaTheme={settings.dioramaTheme}
          onThemeChange={(th) => handleUpdateSettings({ dioramaTheme: th })}
          onSearchCity={handleSearchCity}
          onAutoLoc={handleAutoLoc}
          onOpenForecast={() => setIsForecastOpen(true)}
          onOpenRadar={() => setIsRadarOpen(true)}
          onOpenSettings={() => setIsSettingsOpen(true)}
          tempUnit={settings.tempUnit}
          onToggleTempUnit={toggleTempUnit}
          currentProvider={settings.provider}
          onProviderChange={(p) => handleUpdateSettings({ provider: p })}
          timeMode={settings.timeMode}
          onCycleTimeMode={handleCycleTimeMode}
        />

        {settings.alertsEnabled && (
          <WeatherAlertsBanner
            alerts={activeAlerts}
            onSelectAlert={(al) => setSelectedAlert(al)}
          />
        )}
      </div>

      {/* 7-Day Outlook Modal */}
      <ForecastModal
        isOpen={isForecastOpen}
        onClose={() => setIsForecastOpen(false)}
        telemetry={telemetry}
        tempUnit={settings.tempUnit === 'C' ? 'C' : 'F'}
        onToggleTempUnit={toggleTempUnit}
      />

      {/* Interactive Doppler Weather Radar Modal */}
      <RadarModal
        isOpen={isRadarOpen}
        onClose={() => setIsRadarOpen(false)}
        lat={locationCoords.lat}
        lon={locationCoords.lon}
        locationName={locationCoords.name}
        colorScheme={settings.radarColorScheme}
        opacity={settings.radarOpacity}
        onChangeColorScheme={(sc) => handleUpdateSettings({ radarColorScheme: sc })}
        onChangeOpacity={(op) => handleUpdateSettings({ radarOpacity: op })}
      />

      {/* Weather Alert Details Modal */}
      <AlertDetailsModal
        isOpen={!!selectedAlert}
        onClose={() => setSelectedAlert(null)}
        alert={selectedAlert}
      />

      {/* Full Configuration & Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        settings={settings}
        onUpdateSettings={handleUpdateSettings}
        onSearchCity={handleSearchCity}
        onAutoLoc={handleAutoLoc}
        currentLocationName={locationCoords.name}
        currentCoords={{ lat: locationCoords.lat, lon: locationCoords.lon }}
        onRequestNotifications={handleRequestNotifications}
        notificationPermission={notificationPermission}
      />
    </div>
  );
};
