import {
  DailyForecast,
  HourlyForecastPoint,
  WeatherProviderId,
  WeatherTelemetry,
} from '../../types/weather';
import { WEATHER_PROVIDERS } from './providers';

export class WeatherService {
  private static instance: WeatherService;
  private currentProvider: WeatherProviderId = 'open-meteo';

  private constructor() {
    if (typeof window !== 'undefined' && window.localStorage) {
      const saved = localStorage.getItem('selected_weather_provider') as WeatherProviderId;
      if (saved && WEATHER_PROVIDERS[saved]) {
        this.currentProvider = saved;
      }
    }
  }

  public static getInstance(): WeatherService {
    if (!WeatherService.instance) {
      WeatherService.instance = new WeatherService();
    }
    return WeatherService.instance;
  }

  public getProvider(): WeatherProviderId {
    return this.currentProvider;
  }

  public setProvider(provider: WeatherProviderId): void {
    this.currentProvider = provider;
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('selected_weather_provider', provider);
    }
  }

  public getProviderKey(provider: WeatherProviderId): string {
    const info = WEATHER_PROVIDERS[provider];
    if (!info?.keyStorageKey || typeof window === 'undefined') return '';
    return localStorage.getItem(info.keyStorageKey) || '';
  }

  public setProviderKey(provider: WeatherProviderId, key: string): void {
    const info = WEATHER_PROVIDERS[provider];
    if (info?.keyStorageKey && typeof window !== 'undefined') {
      localStorage.setItem(info.keyStorageKey, key.trim());
    }
  }

  /**
   * Fetches weather telemetry using the active provider with automatic fallback to Open-Meteo.
   */
  public async fetchWeather(
    lat: number,
    lon: number,
    locationName: string,
    forcedProvider?: WeatherProviderId
  ): Promise<WeatherTelemetry> {
    const provider = forcedProvider || this.currentProvider;

    try {
      switch (provider) {
        case 'nws':
          return await this.fetchNwsWeather(lat, lon, locationName);
        case 'met-norway':
          return await this.fetchMetNorwayWeather(lat, lon, locationName);
        case 'openweathermap':
          return await this.fetchOpenWeatherMap(lat, lon, locationName);
        case 'weatherapi':
          return await this.fetchWeatherApi(lat, lon, locationName);
        case 'open-meteo':
        default:
          return await this.fetchOpenMeteoWeather(lat, lon, locationName);
      }
    } catch (err) {
      console.warn(`[WeatherService] Provider ${provider} failed, falling back to Open-Meteo:`, err);
      // Fallback seamlessly to Open-Meteo
      const fallback = await this.fetchOpenMeteoWeather(lat, lon, locationName);
      return {
        ...fallback,
        provider: 'open-meteo',
      };
    }
  }

  // --- Provider 1: Open-Meteo ---
  public async fetchOpenMeteoWeather(
    lat: number,
    lon: number,
    locationName: string
  ): Promise<WeatherTelemetry> {
    const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_gusts_10m,uv_index&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,uv_index_max&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=auto`;
    const aqiUrl = `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${lat}&longitude=${lon}&current=us_aqi`;

    const [wxRes, aqiRes] = await Promise.all([
      fetch(weatherUrl),
      fetch(aqiUrl).catch(() => null),
    ]);

    if (!wxRes.ok) throw new Error(`Open-Meteo HTTP error: ${wxRes.status}`);
    const wxData = await wxRes.json();
    let usAqi = 28;
    if (aqiRes && aqiRes.ok) {
      const aqiData = await aqiRes.json();
      usAqi = aqiData.current?.us_aqi ?? 28;
    }

    const cur = wxData.current || {};
    const daily = wxData.daily || {};
    const hourly = wxData.hourly || {};

    const code = cur.weather_code ?? 0;
    const isDay = cur.is_day === 1;
    const condInfo = this.getConditionInfo(code, isDay);

    // Build 7-day detailed forecast
    const sevenDayDetailed: DailyForecast[] = [];
    const dailyDates: string[] = daily.time || [];

    dailyDates.forEach((dateStr, dIdx) => {
      const dayDate = new Date(dateStr + "T00:00:00");
      const dayName = dIdx === 0 ? "TODAY" : dayDate.toLocaleDateString("en-US", { weekday: "short" });
      const dCode = daily.weather_code?.[dIdx] ?? code;
      const dCond = this.getConditionInfo(dCode, true);

      // Hourly breakdown for this specific day
      const dayHours: HourlyForecastPoint[] = [];
      const hourlyTimes: string[] = hourly.time || [];

      for (let h = 0; h < hourlyTimes.length; h++) {
        if (hourlyTimes[h].startsWith(dateStr)) {
          const hTime = hourlyTimes[h];
          const hDate = new Date(hTime);
          const hourLabel = hDate.toLocaleTimeString("en-US", { hour: "numeric", hour12: true });
          const hCode = hourly.weather_code?.[h] ?? dCode;
          const hCond = this.getConditionInfo(hCode, true);

          dayHours.push({
            time: hTime,
            hourLabel,
            temp: hourly.temperature_2m?.[h] ?? 65,
            feelsLike: hourly.apparent_temperature?.[h] ?? 64,
            humidity: hourly.relative_humidity_2m?.[h] ?? 55,
            precipProb: hourly.precipitation_probability?.[h] ?? 0,
            precipInches: hourly.precipitation?.[h] ?? 0,
            windSpeed: hourly.wind_speed_10m?.[h] ?? 5,
            windGust: hourly.wind_gusts_10m?.[h] ?? 8,
            uvIndex: hourly.uv_index?.[h] ?? 0,
            weatherCode: hCode,
            condition: hCond.name,
            emoji: hCond.emoji,
          });
        }
      }

      sevenDayDetailed.push({
        dayName,
        dateStr,
        tempMin: daily.temperature_2m_min?.[dIdx] ?? 50,
        tempMax: daily.temperature_2m_max?.[dIdx] ?? 70,
        precipProbMax: daily.precipitation_probability_max?.[dIdx] ?? 0,
        precipSum: daily.precipitation_sum?.[dIdx] ?? 0,
        windMax: daily.wind_speed_10m_max?.[dIdx] ?? 10,
        uvMax: daily.uv_index_max?.[dIdx] ?? 4.0,
        sunrise: this.formatTime(daily.sunrise?.[dIdx]),
        sunset: this.formatTime(daily.sunset?.[dIdx]),
        condition: dCond.name,
        emoji: dCond.emoji,
        hourly: dayHours,
      });
    });

    const hourlyTemps = (hourly.temperature_2m || []).slice(0, 24);
    const hourlyPoints = sevenDayDetailed[0]?.hourly || [];

    return {
      tempF: cur.temperature_2m ?? 65,
      feelsLikeF: cur.apparent_temperature ?? 64,
      humidity: cur.relative_humidity_2m ?? 50,
      windMph: cur.wind_speed_10m ?? 7,
      windDeg: cur.wind_direction_10m ?? 180,
      weatherCode: code,
      isDay,
      usAqi,
      uvIndex: hourly.uv_index?.[12] ?? 3.5,
      locationName,
      condition: condInfo.name,
      hourlyTemps,
      hourly: hourlyPoints,
      sevenDayDetailed,
      provider: 'open-meteo',
      lastUpdated: Date.now(),
    };
  }

  // --- Provider 2: US National Weather Service (api.weather.gov) ---
  public async fetchNwsWeather(
    lat: number,
    lon: number,
    locationName: string
  ): Promise<WeatherTelemetry> {
    // 1. Get NWS Grid metadata
    const pointUrl = `https://api.weather.gov/points/${lat.toFixed(4)},${lon.toFixed(4)}`;
    const pointRes = await fetch(pointUrl, {
      headers: { 'User-Agent': 'Global3DIsometricWeatherApp/0.0.5 (github.com/gcerkez)' },
    });
    if (!pointRes.ok) throw new Error(`NWS point lookup failed (status ${pointRes.status}). NWS is only available in US territories.`);

    const pointData = await pointRes.json();
    const forecastHourlyUrl = pointData.properties?.forecastHourly;
    const forecastDailyUrl = pointData.properties?.forecast;

    if (!forecastHourlyUrl || !forecastDailyUrl) throw new Error('NWS grid forecast URLs unavailable');

    const hourlyRes = await fetch(forecastHourlyUrl, { headers: { 'User-Agent': 'Global3DIsometricWeatherApp/0.0.5' } });
    if (!hourlyRes.ok) throw new Error(`NWS hourly error: ${hourlyRes.status}`);
    const hourlyData = await hourlyRes.json();

    const periods: any[] = hourlyData.properties?.periods || [];
    const curPeriod = periods[0] || {};

    const curTemp = curPeriod.temperature ?? 68;
    const curWindStr = curPeriod.windSpeed || "7 mph";
    const curWindNum = parseFloat(curWindStr.replace(/[^\d.]/g, '')) || 7;
    const shortDesc = curPeriod.shortForecast || "Clear";
    const isDay = curPeriod.isDaytime ?? true;
    const code = this.forecastStringToWmo(shortDesc);
    const condInfo = this.getConditionInfo(code, isDay);

    // Group into 7 daily buckets
    const daysMap = new Map<string, HourlyForecastPoint[]>();
    periods.slice(0, 168).forEach(p => {
      const d = new Date(p.startTime);
      const dateKey = d.toISOString().split('T')[0];
      if (!daysMap.has(dateKey)) daysMap.set(dateKey, []);
      const pCode = this.forecastStringToWmo(p.shortForecast || "");
      const pCond = this.getConditionInfo(pCode, p.isDaytime);

      daysMap.get(dateKey)!.push({
        time: p.startTime,
        hourLabel: d.toLocaleTimeString("en-US", { hour: "numeric", hour12: true }),
        temp: p.temperature,
        feelsLike: p.temperature,
        humidity: p.relativeHumidity?.value ?? 55,
        precipProb: p.probabilityOfPrecipitation?.value ?? 0,
        precipInches: (p.probabilityOfPrecipitation?.value ?? 0) > 30 ? 0.05 : 0,
        windSpeed: parseFloat((p.windSpeed || "0").replace(/[^\d.]/g, '')) || 5,
        windGust: parseFloat((p.windSpeed || "0").replace(/[^\d.]/g, '')) * 1.3,
        uvIndex: 4,
        weatherCode: pCode,
        condition: p.shortForecast || pCond.name,
        emoji: pCond.emoji,
      });
    });

    const sevenDayDetailed: DailyForecast[] = [];
    let dIndex = 0;
    for (const [dateStr, hList] of daysMap.entries()) {
      if (dIndex >= 7) break;
      const temps = hList.map(h => h.temp);
      const maxT = Math.max(...temps);
      const minT = Math.min(...temps);
      const dayDate = new Date(dateStr + "T00:00:00");
      const dayName = dIndex === 0 ? "TODAY" : dayDate.toLocaleDateString("en-US", { weekday: "short" });
      const maxPrecip = Math.max(...hList.map(h => h.precipProb));
      const leadCond = hList[0] || {};

      sevenDayDetailed.push({
        dayName,
        dateStr,
        tempMin: minT,
        tempMax: maxT,
        precipProbMax: maxPrecip,
        precipSum: maxPrecip > 30 ? 0.12 : 0,
        windMax: Math.max(...hList.map(h => h.windSpeed)),
        uvMax: 5.0,
        sunrise: "6:45 AM",
        sunset: "6:30 PM",
        condition: leadCond.condition || condInfo.name,
        emoji: leadCond.emoji || condInfo.emoji,
        hourly: hList,
      });
      dIndex++;
    }

    return {
      tempF: curTemp,
      feelsLikeF: curTemp,
      humidity: curPeriod.relativeHumidity?.value ?? 55,
      windMph: curWindNum,
      windDeg: 180,
      weatherCode: code,
      isDay,
      usAqi: 25,
      uvIndex: 4.2,
      locationName: `${locationName} (NWS NOAA)`,
      condition: shortDesc,
      hourlyTemps: periods.slice(0, 24).map(p => p.temperature),
      hourly: sevenDayDetailed[0]?.hourly || [],
      sevenDayDetailed,
      provider: 'nws',
      lastUpdated: Date.now(),
    };
  }

  // --- Provider 3: MET Norway (api.met.no) ---
  public async fetchMetNorwayWeather(
    lat: number,
    lon: number,
    locationName: string
  ): Promise<WeatherTelemetry> {
    const url = `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${lat.toFixed(4)}&lon=${lon.toFixed(4)}`;
    const res = await fetch(url, {
      headers: { 'User-Agent': 'Global3DIsometricWeatherApp/0.0.5 (github.com/gcerkez)' },
    });
    if (!res.ok) throw new Error(`MET Norway HTTP error: ${res.status}`);
    const data = await res.json();

    const timeseries: any[] = data.properties?.timeseries || [];
    if (!timeseries.length) throw new Error('MET Norway returned empty timeseries');

    const cur = timeseries[0]?.data?.instant?.details || {};
    const curSymbol = timeseries[0]?.data?.next_1_hours?.summary?.symbol_code ||
                      timeseries[0]?.data?.next_6_hours?.summary?.symbol_code || 'clearsky_day';

    const tempC = cur.air_temperature ?? 15;
    const tempF = Math.round((tempC * 9 / 5) + 32);
    const windMps = cur.wind_speed ?? 3.5;
    const windMph = Math.round(windMps * 2.23694);
    const humidity = Math.round(cur.relative_humidity ?? 60);
    const windDeg = Math.round(cur.wind_from_direction ?? 180);
    const isDay = curSymbol.includes('day');
    const code = this.metSymbolToWmo(curSymbol);
    const condInfo = this.getConditionInfo(code, isDay);

    // Group into 7 days
    const daysMap = new Map<string, HourlyForecastPoint[]>();
    timeseries.slice(0, 120).forEach(ts => {
      const d = new Date(ts.time);
      const dateKey = d.toISOString().split('T')[0];
      if (!daysMap.has(dateKey)) daysMap.set(dateKey, []);

      const hC = ts.data?.instant?.details?.air_temperature ?? 15;
      const hF = Math.round((hC * 9 / 5) + 32);
      const sym = ts.data?.next_1_hours?.summary?.symbol_code || ts.data?.next_6_hours?.summary?.symbol_code || 'fair_day';
      const pProb = ts.data?.next_1_hours?.details?.probability_of_precipitation ??
                    ts.data?.next_6_hours?.details?.probability_of_precipitation ?? 0;
      const pInches = (ts.data?.next_1_hours?.details?.precipitation_amount ?? 0) / 25.4;
      const hCode = this.metSymbolToWmo(sym);
      const hCond = this.getConditionInfo(hCode, sym.includes('day'));

      daysMap.get(dateKey)!.push({
        time: ts.time,
        hourLabel: d.toLocaleTimeString("en-US", { hour: "numeric", hour12: true }),
        temp: hF,
        feelsLike: hF,
        humidity: Math.round(ts.data?.instant?.details?.relative_humidity ?? 55),
        precipProb: Math.round(pProb),
        precipInches: pInches,
        windSpeed: Math.round((ts.data?.instant?.details?.wind_speed ?? 3) * 2.23694),
        windGust: Math.round((ts.data?.instant?.details?.wind_speed_of_gust ?? 4) * 2.23694),
        uvIndex: Math.round(ts.data?.instant?.details?.ultraviolet_index_clear_sky ?? 3),
        weatherCode: hCode,
        condition: hCond.name,
        emoji: hCond.emoji,
      });
    });

    const sevenDayDetailed: DailyForecast[] = [];
    let dIndex = 0;
    for (const [dateStr, hList] of daysMap.entries()) {
      if (dIndex >= 7) break;
      const temps = hList.map(h => h.temp);
      const maxT = Math.max(...temps);
      const minT = Math.min(...temps);
      const dayDate = new Date(dateStr + "T00:00:00");
      const dayName = dIndex === 0 ? "TODAY" : dayDate.toLocaleDateString("en-US", { weekday: "short" });
      const maxPrecip = Math.max(...hList.map(h => h.precipProb));
      const totalPrecip = hList.reduce((acc, h) => acc + h.precipInches, 0);

      sevenDayDetailed.push({
        dayName,
        dateStr,
        tempMin: minT,
        tempMax: maxT,
        precipProbMax: maxPrecip,
        precipSum: totalPrecip,
        windMax: Math.max(...hList.map(h => h.windSpeed)),
        uvMax: 4.5,
        sunrise: "6:50 AM",
        sunset: "6:40 PM",
        condition: hList[0]?.condition || condInfo.name,
        emoji: hList[0]?.emoji || condInfo.emoji,
        hourly: hList,
      });
      dIndex++;
    }

    return {
      tempF,
      feelsLikeF: tempF,
      humidity,
      windMph,
      windDeg,
      weatherCode: code,
      isDay,
      usAqi: 22,
      uvIndex: 3.8,
      locationName: `${locationName} (MET Norway)`,
      condition: condInfo.name,
      hourlyTemps: sevenDayDetailed[0]?.hourly.map(h => h.temp) || [tempF],
      hourly: sevenDayDetailed[0]?.hourly || [],
      sevenDayDetailed,
      provider: 'met-norway',
      lastUpdated: Date.now(),
    };
  }

  // --- Provider 4: OpenWeatherMap (Key or Demo Simulator) ---
  public async fetchOpenWeatherMap(
    lat: number,
    lon: number,
    locationName: string
  ): Promise<WeatherTelemetry> {
    const apiKey = this.getProviderKey('openweathermap');
    if (!apiKey) {
      // Demo Mode: Fetch base structure from Open-Meteo and brand as OpenWeatherMap preview
      const base = await this.fetchOpenMeteoWeather(lat, lon, locationName);
      return {
        ...base,
        locationName: `${locationName} (OpenWeatherMap Demo)`,
        provider: 'openweathermap',
      };
    }

    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&units=imperial&appid=${apiKey}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`OpenWeatherMap error ${res.status}: ${res.statusText}`);
    const data = await res.json();

    const tempF = Math.round(data.main?.temp ?? 68);
    const feelsLikeF = Math.round(data.main?.feels_like ?? 67);
    const humidity = data.main?.humidity ?? 50;
    const windMph = Math.round(data.wind?.speed ?? 8);
    const windDeg = data.wind?.deg ?? 180;
    const weatherMain = data.weather?.[0]?.main || 'Clear';
    const weatherDesc = data.weather?.[0]?.description || 'Clear sky';
    const isDay = (data.weather?.[0]?.icon || '').includes('d');
    const code = this.owmConditionToWmo(weatherMain);
    const condInfo = this.getConditionInfo(code, isDay);

    // Fetch 5 day / 3 hour forecast if available
    const fUrl = `https://api.openweathermap.org/data/2.5/forecast?lat=${lat}&lon=${lon}&units=imperial&appid=${apiKey}`;
    const fRes = await fetch(fUrl).catch(() => null);
    let sevenDayDetailed: DailyForecast[] = [];

    if (fRes && fRes.ok) {
      const fData = await fRes.json();
      const list: any[] = fData.list || [];
      const daysMap = new Map<string, HourlyForecastPoint[]>();

      list.forEach(item => {
        const d = new Date(item.dt * 1000);
        const dateKey = d.toISOString().split('T')[0];
        if (!daysMap.has(dateKey)) daysMap.set(dateKey, []);
        const iCode = this.owmConditionToWmo(item.weather?.[0]?.main || '');
        const iCond = this.getConditionInfo(iCode, (item.weather?.[0]?.icon || '').includes('d'));

        daysMap.get(dateKey)!.push({
          time: d.toISOString(),
          hourLabel: d.toLocaleTimeString("en-US", { hour: "numeric", hour12: true }),
          temp: Math.round(item.main?.temp ?? tempF),
          feelsLike: Math.round(item.main?.feels_like ?? feelsLikeF),
          humidity: item.main?.humidity ?? 50,
          precipProb: Math.round((item.pop ?? 0) * 100),
          precipInches: (item.rain?.['3h'] ?? 0) / 25.4,
          windSpeed: Math.round(item.wind?.speed ?? 5),
          windGust: Math.round((item.wind?.gust ?? item.wind?.speed ?? 5)),
          uvIndex: 4,
          weatherCode: iCode,
          condition: item.weather?.[0]?.description || iCond.name,
          emoji: iCond.emoji,
        });
      });

      let dIndex = 0;
      for (const [dateStr, hList] of daysMap.entries()) {
        const temps = hList.map(h => h.temp);
        const dayDate = new Date(dateStr + "T00:00:00");
        sevenDayDetailed.push({
          dayName: dIndex === 0 ? "TODAY" : dayDate.toLocaleDateString("en-US", { weekday: "short" }),
          dateStr,
          tempMin: Math.min(...temps),
          tempMax: Math.max(...temps),
          precipProbMax: Math.max(...hList.map(h => h.precipProb)),
          precipSum: hList.reduce((acc, h) => acc + h.precipInches, 0),
          windMax: Math.max(...hList.map(h => h.windSpeed)),
          uvMax: 4.5,
          sunrise: "6:40 AM",
          sunset: "6:35 PM",
          condition: hList[0]?.condition || condInfo.name,
          emoji: hList[0]?.emoji || condInfo.emoji,
          hourly: hList,
        });
        dIndex++;
      }
    }

    if (!sevenDayDetailed.length) {
      const fallback = await this.fetchOpenMeteoWeather(lat, lon, locationName);
      sevenDayDetailed = fallback.sevenDayDetailed;
    }

    return {
      tempF,
      feelsLikeF,
      humidity,
      windMph,
      windDeg,
      weatherCode: code,
      isDay,
      usAqi: 28,
      uvIndex: 4.0,
      locationName: `${locationName} (OpenWeatherMap)`,
      condition: weatherDesc,
      hourlyTemps: sevenDayDetailed[0]?.hourly.map(h => h.temp) || [tempF],
      hourly: sevenDayDetailed[0]?.hourly || [],
      sevenDayDetailed,
      provider: 'openweathermap',
      lastUpdated: Date.now(),
    };
  }

  // --- Provider 5: WeatherAPI.com (Key or Demo Simulator) ---
  public async fetchWeatherApi(
    lat: number,
    lon: number,
    locationName: string
  ): Promise<WeatherTelemetry> {
    const apiKey = this.getProviderKey('weatherapi');
    if (!apiKey) {
      const base = await this.fetchOpenMeteoWeather(lat, lon, locationName);
      return {
        ...base,
        locationName: `${locationName} (WeatherAPI Demo)`,
        provider: 'weatherapi',
      };
    }

    const url = `https://api.weatherapi.com/v1/forecast.json?key=${apiKey}&q=${lat},${lon}&days=7&aqi=yes`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`WeatherAPI error ${res.status}: ${res.statusText}`);
    const data = await res.json();

    const cur = data.current || {};
    const tempF = Math.round(cur.temp_f ?? 70);
    const feelsLikeF = Math.round(cur.feelslike_f ?? 69);
    const humidity = cur.humidity ?? 50;
    const windMph = Math.round(cur.wind_mph ?? 7);
    const windDeg = cur.wind_degree ?? 180;
    const isDay = cur.is_day === 1;
    const condText = cur.condition?.text || 'Clear';
    const code = this.weatherApiConditionToWmo(condText);
    const condInfo = this.getConditionInfo(code, isDay);
    const usAqi = cur.air_quality?.['us-epa-index'] ? cur.air_quality['us-epa-index'] * 25 : 25;

    const forecastDays: any[] = data.forecast?.forecastday || [];
    const sevenDayDetailed: DailyForecast[] = forecastDays.map((fd, idx) => {
      const dayDate = new Date(fd.date + "T00:00:00");
      const dayName = idx === 0 ? "TODAY" : dayDate.toLocaleDateString("en-US", { weekday: "short" });
      const fCode = this.weatherApiConditionToWmo(fd.day?.condition?.text || '');
      const fCond = this.getConditionInfo(fCode, true);

      const hours: HourlyForecastPoint[] = (fd.hour || []).map((h: any) => {
        const hDate = new Date(h.time);
        const hCode = this.weatherApiConditionToWmo(h.condition?.text || '');
        const hCond = this.getConditionInfo(hCode, h.is_day === 1);
        return {
          time: h.time,
          hourLabel: hDate.toLocaleTimeString("en-US", { hour: "numeric", hour12: true }),
          temp: Math.round(h.temp_f),
          feelsLike: Math.round(h.feelslike_f),
          humidity: h.humidity,
          precipProb: h.chance_of_rain ?? 0,
          precipInches: h.precip_in ?? 0,
          windSpeed: Math.round(h.wind_mph),
          windGust: Math.round(h.gust_mph),
          uvIndex: h.uv ?? 3,
          weatherCode: hCode,
          condition: h.condition?.text || hCond.name,
          emoji: hCond.emoji,
        };
      });

      return {
        dayName,
        dateStr: fd.date,
        tempMin: Math.round(fd.day?.mintemp_f ?? 50),
        tempMax: Math.round(fd.day?.maxtemp_f ?? 70),
        precipProbMax: fd.day?.daily_chance_of_rain ?? 0,
        precipSum: fd.day?.totalprecip_in ?? 0,
        windMax: Math.round(fd.day?.maxwind_mph ?? 10),
        uvMax: fd.day?.uv ?? 4.0,
        sunrise: fd.astro?.sunrise || "6:30 AM",
        sunset: fd.astro?.sunset || "6:30 PM",
        condition: fd.day?.condition?.text || fCond.name,
        emoji: fCond.emoji,
        hourly: hours,
      };
    });

    return {
      tempF,
      feelsLikeF,
      humidity,
      windMph,
      windDeg,
      weatherCode: code,
      isDay,
      usAqi,
      uvIndex: cur.uv ?? 4.0,
      locationName: `${locationName} (WeatherAPI)`,
      condition: condInfo.name || condText,
      hourlyTemps: sevenDayDetailed[0]?.hourly.map(h => h.temp) || [tempF],
      hourly: sevenDayDetailed[0]?.hourly || [],
      sevenDayDetailed,
      provider: 'weatherapi',
      lastUpdated: Date.now(),
    };
  }

  // --- Helpers & WMO Translators ---
  public getConditionInfo(code: number, isDay: boolean): { name: string; emoji: string } {
    switch (code) {
      case 0: return { name: isDay ? "Sunny" : "Clear Sky", emoji: isDay ? "☀️" : "🌙" };
      case 1: return { name: "Mainly Clear", emoji: isDay ? "🌤️" : "🌙" };
      case 2: return { name: "Partly Cloudy", emoji: isDay ? "⛅" : "☁️" };
      case 3: return { name: "Overcast", emoji: "☁️" };
      case 45:
      case 48: return { name: "Coastal Fog", emoji: "🌫️" };
      case 51:
      case 53:
      case 55: return { name: "Light Drizzle", emoji: "🌦️" };
      case 61:
      case 63:
      case 65: return { name: "Rain Showers", emoji: "🌧️" };
      case 66:
      case 67: return { name: "Freezing Rain", emoji: "🌨️" };
      case 71: return { name: "Slight Snow", emoji: "🌨️" };
      case 73: return { name: "Moderate Snow", emoji: "🌨️" };
      case 75: return { name: "Heavy Snow", emoji: "🌨️" };
      case 77: return { name: "Snow Grains", emoji: "❄️" };
      case 80:
      case 81:
      case 82: return { name: "Heavy Showers", emoji: "🌧️" };
      case 85:
      case 86: return { name: "Snow Showers", emoji: "🌨️" };
      case 95: return { name: "Thunderstorm", emoji: "⛈️" };
      case 96:
      case 99: return { name: "Severe Thunderstorm", emoji: "⛈️" };
      default: return { name: isDay ? "Fair Weather" : "Clear Night", emoji: isDay ? "☀️" : "🌙" };
    }
  }

  private formatTime(timeStr?: string): string {
    if (!timeStr) return "--:--";
    try {
      const d = new Date(timeStr);
      return d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });
    } catch {
      return timeStr;
    }
  }

  public forecastStringToWmo(desc: string): number {
    const s = desc.toLowerCase();
    if (s.includes('thunder')) return 95;
    if (s.includes('heavy snow') || s.includes('blizzard')) return 75;
    if (s.includes('snow') || s.includes('flurr')) return 71;
    if (s.includes('rain') || s.includes('shower')) return 61;
    if (s.includes('drizzle')) return 51;
    if (s.includes('fog') || s.includes('haze')) return 45;
    if (s.includes('cloud') || s.includes('overcast')) return 3;
    if (s.includes('partly')) return 2;
    return 0;
  }

  private metSymbolToWmo(symbol: string): number {
    const s = symbol.toLowerCase();
    if (s.includes('thunder')) return 95;
    if (s.includes('snow') || s.includes('sleet')) return 71;
    if (s.includes('rain')) return 61;
    if (s.includes('fog')) return 45;
    if (s.includes('overcast') || s.includes('cloudy')) return 3;
    if (s.includes('partlycloudy')) return 2;
    if (s.includes('fair')) return 1;
    return 0;
  }

  private owmConditionToWmo(main: string): number {
    const m = main.toLowerCase();
    if (m.includes('thunder')) return 95;
    if (m.includes('snow')) return 71;
    if (m.includes('rain')) return 61;
    if (m.includes('drizzle')) return 51;
    if (m.includes('fog') || m.includes('mist') || m.includes('haze')) return 45;
    if (m.includes('cloud')) return 2;
    return 0;
  }

  private weatherApiConditionToWmo(text: string): number {
    const t = text.toLowerCase();
    if (t.includes('thunder')) return 95;
    if (t.includes('snow') || t.includes('blizzard') || t.includes('ice')) return 71;
    if (t.includes('rain') || t.includes('shower')) return 61;
    if (t.includes('drizzle')) return 51;
    if (t.includes('fog') || t.includes('mist')) return 45;
    if (t.includes('overcast')) return 3;
    if (t.includes('cloudy')) return 2;
    return 0;
  }
}
