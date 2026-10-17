.pragma library

/**
 * @file AstronomyAndAqi.js
 * @description Astronomical calculation helpers (Lunar synodic phase, solar arc progress,
 * golden hour detection) and Air Quality / UV Index classification utilities.
 *
 * Reconstructed and adapted using the REA 5-Stage Framework from:
 * - pnedyalkov91/advanced-weather-widget (Evidence ID: ev_0b811fd40fcac6f3139892849db1fe27b60353ae5370afb74b5cb99ab1cebbf9)
 * - Samy879/minimalist-animated-weather (Evidence ID: ev_94739ca0ea684467e034f554fbab5a41cc708bef3a702dd7df681cce0afa0a03)
 */

/** Mean synodic month length in days (New Moon to New Moon). */
var SYNODIC_MONTH_DAYS = 29.530588853;

/** Known reference New Moon epoch: 2000-01-06 18:14 UTC (Unix ms). */
var KNOWN_NEW_MOON_MS = 947182440000;

/**
 * Calculates lunar phase telemetry for a given timestamp or Date object.
 *
 * @param {Date|number} [dateOrMs] - JavaScript Date or Unix epoch milliseconds (defaults to now).
 * @returns {{
 *   phase: number,
 *   ageDays: number,
 *   illumination: number,
 *   name: string,
 *   emoji: string,
 *   isWaxing: boolean
 * }} Lunar phase descriptor where `phase` is in [0, 1) (0 = New, 0.5 = Full).
 */
function calculateMoonPhase(dateOrMs) {
    var ms = (typeof dateOrMs === "number")
        ? dateOrMs
        : (dateOrMs instanceof Date ? dateOrMs.getTime() : Date.now());

    var elapsedDays = (ms - KNOWN_NEW_MOON_MS) / 86400000.0;
    var phase = ((elapsedDays / SYNODIC_MONTH_DAYS) % 1.0 + 1.0) % 1.0;
    var ageDays = phase * SYNODIC_MONTH_DAYS;
    var illumination = 0.5 * (1.0 - Math.cos(phase * 2.0 * Math.PI));

    var name = "Waning Crescent";
    var emoji = "🌘";
    if (ageDays < 1.85) {
        name = "New Moon";
        emoji = "🌑";
    } else if (ageDays < 7.38) {
        name = "Waxing Crescent";
        emoji = "🌒";
    } else if (ageDays < 9.23) {
        name = "First Quarter";
        emoji = "🌓";
    } else if (ageDays < 14.77) {
        name = "Waxing Gibbous";
        emoji = "🌔";
    } else if (ageDays < 16.62) {
        name = "Full Moon";
        emoji = "🌕";
    } else if (ageDays < 22.15) {
        name = "Waning Gibbous";
        emoji = "🌖";
    } else if (ageDays < 24.0) {
        name = "Last Quarter";
        emoji = "🌗";
    }

    return {
        phase: phase,
        ageDays: ageDays,
        illumination: illumination,
        name: name,
        emoji: emoji,
        isWaxing: phase < 0.5
    };
}

/**
 * Computes solar cycle state (daylight progress, golden hour, night) from
 * Open-Meteo ISO-8601 sunrise and sunset timestamps.
 *
 * @param {number} nowMs - Current timestamp in Unix milliseconds.
 * @param {string} sunriseIso - ISO timestamp string for today's sunrise.
 * @param {string} sunsetIso - ISO timestamp string for today's sunset.
 * @param {boolean} fallbackIsDay - Fallback boolean from Open-Meteo `current.is_day`.
 * @returns {{
 *   timeState: string,
 *   sunProgress: number,
 *   isGoldenHour: boolean,
 *   sunriseFormatted: string,
 *   sunsetFormatted: string
 * }} Solar state descriptor (`timeState` in `"day" | "sunset" | "night"`).
 */
function computeSolarState(nowMs, sunriseIso, sunsetIso, fallbackIsDay) {
    var srMs = sunriseIso ? Date.parse(sunriseIso) : NaN;
    var ssMs = sunsetIso ? Date.parse(sunsetIso) : NaN;

    function fmtClock(msVal) {
        if (isNaN(msVal)) return "--:--";
        var d = new Date(msVal);
        var h = d.getHours();
        var m = d.getMinutes();
        var ampm = h >= 12 ? "PM" : "AM";
        var h12 = h % 12;
        if (h12 === 0) h12 = 12;
        return h12 + ":" + (m < 10 ? "0" + m : m) + " " + ampm;
    }

    if (isNaN(srMs) || isNaN(ssMs) || ssMs <= srMs) {
        return {
            timeState: fallbackIsDay ? "day" : "night",
            sunProgress: fallbackIsDay ? 0.5 : 0.0,
            isGoldenHour: false,
            sunriseFormatted: "--:--",
            sunsetFormatted: "--:--"
        };
    }

    var nearSunrise = Math.abs(nowMs - srMs) <= 45 * 60 * 1000;
    var nearSunset  = Math.abs(nowMs - ssMs) <= 55 * 60 * 1000;
    var isGolden = nearSunrise || nearSunset;
    var isDaylight = (nowMs >= srMs - 15 * 60 * 1000) && (nowMs <= ssMs + 20 * 60 * 1000);
    var prog = 0.0;
    if (nowMs > srMs && nowMs < ssMs) {
        prog = (nowMs - srMs) / (ssMs - srMs);
    }

    var state = "night";
    if (isGolden) state = "sunset";
    else if (isDaylight) state = "day";

    return {
        timeState: state,
        sunProgress: Math.max(0.0, Math.min(1.0, prog)),
        isGoldenHour: isGolden,
        sunriseFormatted: fmtClock(srMs),
        sunsetFormatted: fmtClock(ssMs)
    };
}

/**
 * Classifies a US EPA Air Quality Index (0..500) into official EPA band metadata.
 *
 * @param {number} aqi - Numeric US EPA AQI value.
 * @returns {{ value: number, label: string, shortLabel: string, color: string, advisory: string }}
 */
function classifyUsAqi(aqi) {
    var v = Math.max(0, Math.round(aqi || 0));
    if (v <= 50) {
        return { value: v, label: "Good", shortLabel: "GOOD", color: "#4ade80", advisory: "Clean air quality" };
    }
    if (v <= 100) {
        return { value: v, label: "Moderate", shortLabel: "MOD", color: "#facc15", advisory: "Acceptable air quality" };
    }
    if (v <= 150) {
        return { value: v, label: "Sensitive Groups", shortLabel: "USG", color: "#fb923c", advisory: "Sensitive groups reduce prolonged outdoor exertion" };
    }
    if (v <= 200) {
        return { value: v, label: "Unhealthy", shortLabel: "UNHL", color: "#f87171", advisory: "Everyone may begin to experience health effects" };
    }
    if (v <= 300) {
        return { value: v, label: "Very Unhealthy", shortLabel: "V.UNHL", color: "#c084fc", advisory: "Health alert: avoid prolonged outdoor exertion" };
    }
    return { value: v, label: "Hazardous", shortLabel: "HAZ", color: "#a855f7", advisory: "Health warning of emergency conditions" };
}

/**
 * Classifies WHO UV Index (0..11+) into risk category and badge color.
 *
 * @param {number} uv - UV index value.
 * @returns {{ value: number, label: string, color: string }}
 */
function classifyUvIndex(uv) {
    var v = Math.max(0, Math.round((uv || 0) * 10) / 10);
    if (v < 3) return { value: v, label: "Low", color: "#4ade80" };
    if (v < 6) return { value: v, label: "Mod", color: "#facc15" };
    if (v < 8) return { value: v, label: "High", color: "#fb923c" };
    if (v < 11) return { value: v, label: "V.High", color: "#f87171" };
    return { value: v, label: "Extreme", color: "#c084fc" };
}
