import React, { useRef, useEffect, useState, useId } from 'react';
import { DailyForecast, HourlyForecastPoint } from '../types/weather';

export type ChartMode = 'precip' | 'temp' | 'wind';

interface HourlyBreakdownChartProps {
  day: DailyForecast;
  tempUnit?: 'F' | 'C';
}

export const HourlyBreakdownChart: React.FC<HourlyBreakdownChartProps> = ({
  day,
  tempUnit = 'F',
}) => {
  const [mode, setMode] = useState<ChartMode>('precip');
  const [hoveredHour, setHoveredHour] = useState<HourlyForecastPoint | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const chartId = useId();

  const convertTemp = (f: number) => {
    return tempUnit === 'C' ? Math.round(((f - 32) * 5) / 9) : Math.round(f);
  };

  const hours = day.hourly || [];

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;
    const w = (rect.width || 560) * dpr;
    const h = (rect.height || 180) * dpr;
    canvas.width = w;
    canvas.height = h;

    ctx.clearRect(0, 0, w, h);

    const padL = 36 * dpr;
    const padR = 14 * dpr;
    const padT = 24 * dpr;
    const padB = 26 * dpr;
    const chartW = Math.max(10, w - padL - padR);
    const chartH = Math.max(10, h - padT - padB);

    if (hours.length === 0) {
      ctx.fillStyle = '#64748b';
      ctx.font = `${12 * dpr}px sans-serif`;
      ctx.textAlign = 'center';
      ctx.fillText('No hourly data available', w / 2, h / 2);
      return;
    }

    const count = hours.length;
    const stepX = chartW / count;

    // Draw background grid
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
    ctx.lineWidth = 1 * dpr;
    ctx.font = `${9.5 * dpr}px sans-serif`;
    ctx.fillStyle = '#64748b';
    ctx.textAlign = 'right';

    if (mode === 'precip') {
      [100, 75, 50, 25, 0].forEach((pct) => {
        const y = padT + chartH * (1 - pct / 100);
        ctx.beginPath();
        ctx.moveTo(padL, y);
        ctx.lineTo(padL + chartW, y);
        ctx.stroke();
        ctx.fillText(`${pct}%`, padL - 5 * dpr, y + 3 * dpr);
      });

      const barW = Math.max(4 * dpr, stepX * 0.65);
      hours.forEach((pt, i) => {
        const cx = padL + i * stepX + stepX / 2;
        const barH = chartH * (pt.precipProb / 100);
        const top = padT + chartH - barH;

        if (barH > 1) {
          const grad = ctx.createLinearGradient(0, top, 0, padT + chartH);
          grad.addColorStop(0, '#38bdf8');
          grad.addColorStop(1, '#1d4ed8');
          ctx.fillStyle = grad;
          ctx.beginPath();
          if (ctx.roundRect) {
            ctx.roundRect(cx - barW / 2, top, barW, barH, [3 * dpr, 3 * dpr, 0, 0]);
          } else {
            ctx.rect(cx - barW / 2, top, barW, barH);
          }
          ctx.fill();
        }

        if (pt.precipProb >= 20) {
          ctx.font = `bold ${9 * dpr}px sans-serif`;
          ctx.fillStyle = '#7dd3fc';
          ctx.textAlign = 'center';
          ctx.fillText(`${pt.precipProb}%`, cx, Math.max(padT + 10 * dpr, top - 3 * dpr));
        }

        // Hour labels every 3 hours
        if (i % 3 === 0) {
          ctx.font = `${9 * dpr}px sans-serif`;
          ctx.fillStyle = '#94a3b8';
          ctx.textAlign = 'center';
          ctx.fillText(pt.hourLabel, cx, h - 8 * dpr);
        }
      });
    } else if (mode === 'temp') {
      const temps = hours.map((h) => (tempUnit === 'C' ? ((h.temp - 32) * 5) / 9 : h.temp));
      const minT = Math.min(...temps);
      const maxT = Math.max(...temps);
      const range = Math.max(4, maxT - minT);

      [maxT, (minT + maxT) / 2, minT].forEach((t) => {
        const frac = (t - minT) / range;
        const y = padT + chartH * (1 - frac);
        ctx.beginPath();
        ctx.moveTo(padL, y);
        ctx.lineTo(padL + chartW, y);
        ctx.stroke();
        ctx.fillText(`${Math.round(t)}°`, padL - 5 * dpr, y + 3 * dpr);
      });

      const points = hours.map((_, i) => ({
        x: padL + i * stepX + stepX / 2,
        y: padT + chartH * (1 - (temps[i] - minT) / range),
      }));

      if (points.length > 1) {
        // Area under temperature curve
        const grad = ctx.createLinearGradient(0, padT, 0, padT + chartH);
        grad.addColorStop(0, 'rgba(255, 209, 102, 0.35)');
        grad.addColorStop(1, 'rgba(255, 209, 102, 0.00)');
        ctx.fillStyle = grad;

        ctx.beginPath();
        ctx.moveTo(points[0].x, padT + chartH);
        ctx.lineTo(points[0].x, points[0].y);
        for (let i = 0; i < points.length - 1; i++) {
          const xc = (points[i].x + points[i + 1].x) / 2;
          const yc = (points[i].y + points[i + 1].y) / 2;
          ctx.quadraticCurveTo(points[i].x, points[i].y, xc, yc);
        }
        ctx.lineTo(points[points.length - 1].x, points[points.length - 1].y);
        ctx.lineTo(points[points.length - 1].x, padT + chartH);
        ctx.closePath();
        ctx.fill();

        // Stroke line
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (let i = 0; i < points.length - 1; i++) {
          const xc = (points[i].x + points[i + 1].x) / 2;
          const yc = (points[i].y + points[i + 1].y) / 2;
          ctx.quadraticCurveTo(points[i].x, points[i].y, xc, yc);
        }
        ctx.lineTo(points[points.length - 1].x, points[points.length - 1].y);
        ctx.strokeStyle = '#ffd166';
        ctx.lineWidth = 2.5 * dpr;
        ctx.stroke();

        // Temperature points
        hours.forEach((_, i) => {
          if (i % 3 === 0 || i === points.length - 1) {
            ctx.fillStyle = '#ffd166';
            ctx.beginPath();
            ctx.arc(points[i].x, points[i].y, 3 * dpr, 0, Math.PI * 2);
            ctx.fill();

            ctx.font = `bold ${9 * dpr}px sans-serif`;
            ctx.fillStyle = '#fff';
            ctx.textAlign = 'center';
            ctx.fillText(`${Math.round(temps[i])}°`, points[i].x, points[i].y - 6 * dpr);

            ctx.font = `${9 * dpr}px sans-serif`;
            ctx.fillStyle = '#94a3b8';
            ctx.fillText(hours[i].hourLabel, points[i].x, h - 8 * dpr);
          }
        });
      }
    } else if (mode === 'wind') {
      const winds = hours.map((h) => h.windSpeed);
      const maxW = Math.max(15, ...winds);

      [maxW, maxW / 2, 0].forEach((wVal) => {
        const frac = wVal / maxW;
        const y = padT + chartH * (1 - frac);
        ctx.beginPath();
        ctx.moveTo(padL, y);
        ctx.lineTo(padL + chartW, y);
        ctx.stroke();
        ctx.fillText(`${Math.round(wVal)}m`, padL - 5 * dpr, y + 3 * dpr);
      });

      const points = hours.map((_, i) => ({
        x: padL + i * stepX + stepX / 2,
        y: padT + chartH * (1 - winds[i] / maxW),
      }));

      if (points.length > 1) {
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (let i = 0; i < points.length - 1; i++) {
          const xc = (points[i].x + points[i + 1].x) / 2;
          const yc = (points[i].y + points[i + 1].y) / 2;
          ctx.quadraticCurveTo(points[i].x, points[i].y, xc, yc);
        }
        ctx.lineTo(points[points.length - 1].x, points[points.length - 1].y);
        ctx.strokeStyle = '#34d399';
        ctx.lineWidth = 2.2 * dpr;
        ctx.stroke();

        hours.forEach((_, i) => {
          if (i % 3 === 0) {
            ctx.font = `${9 * dpr}px sans-serif`;
            ctx.fillStyle = '#94a3b8';
            ctx.textAlign = 'center';
            ctx.fillText(hours[i].hourLabel, points[i].x, h - 8 * dpr);
          }
        });
      }
    }
  }, [day, mode, tempUnit, hours]);

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas || hours.length === 0) return;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const padL = 36;
    const padR = 14;
    const chartW = rect.width - padL - padR;
    if (x < padL || x > padL + chartW) {
      setHoveredHour(null);
      return;
    }
    const idx = Math.floor(((x - padL) / chartW) * hours.length);
    if (idx >= 0 && idx < hours.length) {
      setHoveredHour(hours[idx]);
    }
  };

  const handleMouseLeave = () => {
    setHoveredHour(null);
  };

  const avgHum =
    hours.length > 0
      ? Math.round(hours.reduce((acc, h) => acc + h.humidity, 0) / hours.length)
      : 55;
  const uvRating = day.uvMax < 3 ? 'Low' : day.uvMax < 6 ? 'Mod' : 'High';

  return (
    <div className="hourly-breakdown-root" data-testid={`hourly-breakdown-${chartId}`}>
      <div className="graph-header">
        <div className="graph-title" data-testid="graph-title">
          Hourly Breakdown • {day.dayName}
        </div>
        <div className="mat-button-toggle-group" role="group">
          <button
            type="button"
            className={`mat-button-toggle ${mode === 'precip' ? 'active' : ''}`}
            onClick={() => setMode('precip')}
            data-testid="mode-precip-btn"
          >
            🌧️ Precip
          </button>
          <button
            type="button"
            className={`mat-button-toggle ${mode === 'temp' ? 'active' : ''}`}
            onClick={() => setMode('temp')}
            data-testid="mode-temp-btn"
          >
            🌡️ Temp
          </button>
          <button
            type="button"
            className={`mat-button-toggle ${mode === 'wind' ? 'active' : ''}`}
            onClick={() => setMode('wind')}
            data-testid="mode-wind-btn"
          >
            💨 Wind
          </button>
        </div>
      </div>

      <div className="graph-canvas-box">
        <canvas
          ref={canvasRef}
          className="hourly-canvas"
          onMouseMove={handleMouseMove}
          onMouseLeave={handleMouseLeave}
          data-testid="hourly-canvas"
        />
      </div>

      {hoveredHour ? (
        <div className="graph-hint active" data-testid="graph-hover-info">
          ⏰ <strong>{hoveredHour.hourLabel}</strong>: {hoveredHour.emoji} {hoveredHour.condition} •{' '}
          {convertTemp(hoveredHour.temp)}°{tempUnit} • 💧 {hoveredHour.precipProb}% ({hoveredHour.precipInches.toFixed(2)} in) • 💨{' '}
          {Math.round(hoveredHour.windSpeed)} MPH
        </div>
      ) : (
        <div className="graph-hint" data-testid="graph-hint">
          👆 Move mouse or finger across the graph to inspect hourly details.
        </div>
      )}

      <div className="metrics-grid">
        <div className="metric-tile" data-testid="metric-tile-precip">
          <div className="metric-tile-title">🌧️ PRECIPITATION</div>
          <div className="metric-tile-val">{day.precipSum.toFixed(2)} in</div>
          <div className="metric-tile-sub">Peak chance: {day.precipProbMax}%</div>
        </div>

        <div className="metric-tile" data-testid="metric-tile-sun">
          <div className="metric-tile-title">🌅 SUN CYCLE</div>
          <div className="metric-tile-val">{day.sunrise}</div>
          <div className="metric-tile-sub">Sunset: {day.sunset}</div>
        </div>

        <div className="metric-tile" data-testid="metric-tile-wind">
          <div className="metric-tile-title">💨 WIND &amp; GUSTS</div>
          <div className="metric-tile-val">{Math.round(day.windMax)} MPH</div>
          <div className="metric-tile-sub">Peak gusts</div>
        </div>

        <div className="metric-tile" data-testid="metric-tile-uv">
          <div className="metric-tile-title">☀️ UV &amp; HUMIDITY</div>
          <div className="metric-tile-val">
            {day.uvMax.toFixed(1)} {uvRating}
          </div>
          <div className="metric-tile-sub">Avg Humidity: {avgHum}%</div>
        </div>
      </div>
    </div>
  );
};
