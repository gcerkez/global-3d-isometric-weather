import React, { useState, useEffect, useRef, useCallback } from 'react';
import { radarService } from '../services/radar/RadarService';
import { RadarFrame, RadarMetadata } from '../types/weather';

export interface RadarViewProps {
  lat: number;
  lon: number;
  locationName: string;
  colorScheme?: number;
  opacity?: number;
  onChangeColorScheme?: (scheme: number) => void;
  onChangeOpacity?: (opacity: number) => void;
  height?: number | string;
}

export const RadarView: React.FC<RadarViewProps> = ({
  lat,
  lon,
  locationName,
  colorScheme = 1,
  opacity = 0.75,
  onChangeColorScheme,
  onChangeOpacity,
  height = 420,
}) => {
  const [metadata, setMetadata] = useState<RadarMetadata | null>(null);
  const [allFrames, setAllFrames] = useState<RadarFrame[]>([]);
  const [currentFrameIdx, setCurrentFrameIdx] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(true);
  const [zoom, setZoom] = useState<number>(7);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [selectedScheme, setSelectedScheme] = useState<number>(colorScheme);
  const [selectedOpacity, setSelectedOpacity] = useState<number>(opacity);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const playTimerRef = useRef<number | null>(null);

  // Load radar metadata
  useEffect(() => {
    setIsLoading(true);
    radarService.fetchRadarMetadata().then((data) => {
      setMetadata(data);
      const combined = [...data.past, ...data.nowcast];
      setAllFrames(combined);
      // Default to the last observed past frame (Now)
      const nowIdx = data.past.length > 0 ? data.past.length - 1 : 0;
      setCurrentFrameIdx(nowIdx);
      setIsLoading(false);
    });
  }, [lat, lon]);

  // Spacebar toggle play/pause
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === ' ' || e.code === 'Space') {
        const target = e.target as HTMLElement;
        if (target && (target.tagName === 'INPUT' || target.tagName === 'SELECT' || target.tagName === 'TEXTAREA')) {
          return;
        }
        e.preventDefault();
        setIsPlaying((p) => !p);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Animation playback loop
  useEffect(() => {
    if (!isPlaying || allFrames.length <= 1) {
      if (playTimerRef.current) clearInterval(playTimerRef.current);
      return;
    }

    const intervalMs = Math.round(700 / playbackSpeed);
    playTimerRef.current = window.setInterval(() => {
      setCurrentFrameIdx((prev) => (prev + 1) % allFrames.length);
    }, intervalMs);

    return () => {
      if (playTimerRef.current) clearInterval(playTimerRef.current);
    };
  }, [isPlaying, allFrames.length, playbackSpeed]);

  // Render composite map on canvas
  const renderMap = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas || !metadata || allFrames.length === 0) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const heightPx = canvas.height;
    ctx.clearRect(0, 0, width, heightPx);

    // Deep dark background
    ctx.fillStyle = '#070d18';
    ctx.fillRect(0, 0, width, heightPx);

    const centerTile = radarService.latLonToTile(lat, lon, zoom);
    const tileSize = 256;
    const curFrame = allFrames[currentFrameIdx] || allFrames[0];

    // Compute center offset so user coordinate is in the middle of canvas
    const n = Math.pow(2, zoom);
    const preciseX = ((lon + 180) / 360) * n;
    const latRad = (lat * Math.PI) / 180;
    const preciseY =
      ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) * n;

    const pixelOffsetX = (preciseX - centerTile.x) * tileSize;
    const pixelOffsetY = (preciseY - centerTile.y) * tileSize;
    const canvasCenterX = width / 2;
    const canvasCenterY = heightPx / 2;

    // Draw 3x3 grid around center tile
    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        const tx = centerTile.x + dx;
        const ty = centerTile.y + dy;
        if (tx < 0 || tx >= n || ty < 0 || ty >= n) continue;

        const posX = canvasCenterX + dx * tileSize - pixelOffsetX;
        const posY = canvasCenterY + dy * tileSize - pixelOffsetY;

        // Base Map Tile
        const baseImg = new Image();
        baseImg.crossOrigin = 'anonymous';
        baseImg.src = radarService.getBaseMapTileUrl(zoom, tx, ty);
        baseImg.onload = () => {
          ctx.save();
          ctx.drawImage(baseImg, posX, posY, tileSize, tileSize);

          // Radar Tile
          const radarImg = new Image();
          radarImg.crossOrigin = 'anonymous';
          radarImg.src = radarService.getTileUrl(
            metadata.host,
            curFrame.path,
            zoom,
            tx,
            ty,
            selectedScheme
          );
          radarImg.onload = () => {
            ctx.globalAlpha = selectedOpacity;
            ctx.drawImage(radarImg, posX, posY, tileSize, tileSize);
            ctx.restore();
            drawRadarOverlayHUD(ctx, width, heightPx, canvasCenterX, canvasCenterY);
          };
          radarImg.onerror = () => {
            ctx.restore();
            drawRadarOverlayHUD(ctx, width, heightPx, canvasCenterX, canvasCenterY);
          };
        };
        baseImg.onerror = () => {
          ctx.save();
          ctx.fillStyle = '#0a101d';
          ctx.fillRect(posX, posY, tileSize, tileSize);
          ctx.strokeStyle = '#1e293b';
          ctx.strokeRect(posX, posY, tileSize, tileSize);
          ctx.restore();

          // Radar Tile
          const radarImg = new Image();
          radarImg.crossOrigin = 'anonymous';
          radarImg.src = radarService.getTileUrl(
            metadata.host,
            curFrame.path,
            zoom,
            tx,
            ty,
            selectedScheme
          );
          radarImg.onload = () => {
            ctx.save();
            ctx.globalAlpha = selectedOpacity;
            ctx.drawImage(radarImg, posX, posY, tileSize, tileSize);
            ctx.restore();
            drawRadarOverlayHUD(ctx, width, heightPx, canvasCenterX, canvasCenterY);
          };
          radarImg.onerror = () => {
            drawRadarOverlayHUD(ctx, width, heightPx, canvasCenterX, canvasCenterY);
          };
        };
      }
    }

    // Fallback immediate HUD draw
    drawRadarOverlayHUD(ctx, width, heightPx, canvasCenterX, canvasCenterY);
  }, [lat, lon, zoom, metadata, allFrames, currentFrameIdx, selectedScheme, selectedOpacity]);

  // Helper to draw range rings and location pin
  const drawRadarOverlayHUD = (
    ctx: CanvasRenderingContext2D,
    _w: number,
    _h: number,
    cx: number,
    cy: number
  ) => {
    ctx.save();

    // Range rings
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.25)';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);

    // Ring 1 (approx 25 mi)
    ctx.beginPath();
    ctx.arc(cx, cy, 60, 0, Math.PI * 2);
    ctx.stroke();

    // Ring 2 (approx 50 mi)
    ctx.beginPath();
    ctx.arc(cx, cy, 120, 0, Math.PI * 2);
    ctx.stroke();

    // Ring 3 (approx 100 mi)
    ctx.beginPath();
    ctx.arc(cx, cy, 180, 0, Math.PI * 2);
    ctx.stroke();

    ctx.setLineDash([]);

    // Location Pulse Marker
    ctx.fillStyle = 'rgba(239, 68, 68, 0.35)';
    ctx.beginPath();
    ctx.arc(cx, cy, 14, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#ef4444';
    ctx.beginPath();
    ctx.arc(cx, cy, 5, 0, Math.PI * 2);
    ctx.fill();

    // Crosshairs
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.4)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(cx - 10, cy);
    ctx.lineTo(cx + 10, cy);
    ctx.moveTo(cx, cy - 10);
    ctx.lineTo(cx, cy + 10);
    ctx.stroke();

    // Location Label Pin
    ctx.font = 'bold 11px sans-serif';
    ctx.fillStyle = '#ffffff';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.8)';
    ctx.shadowBlur = 4;
    ctx.fillText(`📍 ${locationName}`, cx + 12, cy - 8);

    ctx.restore();
  };

  useEffect(() => {
    if (metadata) {
      renderMap();
    }
  }, [metadata, currentFrameIdx, zoom, selectedScheme, selectedOpacity, renderMap]);

  const currentFrame = allFrames[currentFrameIdx];
  const isNowcast = currentFrame?.isNowcast || false;
  const timeFormatted = currentFrame ? radarService.formatFrameTime(currentFrame.time) : '--:--';

  const handleSchemeChange = (val: number) => {
    setSelectedScheme(val);
    if (onChangeColorScheme) onChangeColorScheme(val);
  };

  const handleOpacityChange = (val: number) => {
    setSelectedOpacity(val);
    if (onChangeOpacity) onChangeOpacity(val);
  };

  return (
    <div className="radar-view-root" data-testid="radar-view">
      {/* Viewport */}
      <div
        className="radar-viewport-container"
        style={{ height: typeof height === 'number' ? `${height}px` : height }}
      >
        <canvas
          ref={canvasRef}
          width={720}
          height={typeof height === 'number' ? height : 420}
          className="radar-canvas"
          data-testid="radar-canvas"
        />

        {isLoading && (
          <div className="radar-loading-overlay">
            <div className="radar-spinner" />
            <span>FETCHING DOPPLER RADAR TILES...</span>
          </div>
        )}

        {/* Floating Map Zoom Controls */}
        <div className="radar-map-controls">
          <button
            type="button"
            className="map-ctrl-btn"
            onClick={() => setZoom((z) => Math.min(10, z + 1))}
            title="Zoom In"
            data-testid="radar-zoom-in"
          >
            ＋
          </button>
          <button
            type="button"
            className="map-ctrl-btn"
            onClick={() => setZoom((z) => Math.max(4, z - 1))}
            title="Zoom Out"
            data-testid="radar-zoom-out"
          >
            －
          </button>
          <button
            type="button"
            className="map-ctrl-btn"
            onClick={() => setZoom(7)}
            title="Reset Radar Center"
            data-testid="radar-reset-center"
          >
            🎯
          </button>
        </div>

        {/* Timestamp Readout Pill */}
        <div className="radar-timestamp-badge" data-testid="radar-timestamp-badge">
          <span className={`radar-frame-type-pill ${isNowcast ? 'nowcast' : 'past'}`}>
            {isNowcast ? '🔮 NOWCAST' : '📡 OBSERVED'}
          </span>
          <span className="radar-time-text">{timeFormatted}</span>
        </div>

        {/* Reflectivity dBZ Legend */}
        <div className="radar-legend-bar" data-testid="radar-legend-bar">
          <span className="legend-label">dBZ:</span>
          <div className="legend-scale">
            <span className="dbz-block dbz-15" title="Light Rain (15 dBZ)">15</span>
            <span className="dbz-block dbz-30" title="Moderate Rain (30 dBZ)">30</span>
            <span className="dbz-block dbz-40" title="Heavy Rain (40 dBZ)">40</span>
            <span className="dbz-block dbz-50" title="Very Heavy (50 dBZ)">50</span>
            <span className="dbz-block dbz-60" title="Severe / Hail (60+ dBZ)">60+</span>
          </div>
        </div>
      </div>

      {/* Interactive Control Panel */}
      <div className="radar-controls-card">
        {/* Row 1: Dedicated Timeline Scrubber with Time Range Labels */}
        <div className="radar-scrubber-section">
          <div className="scrubber-header-row">
            <span className="scrubber-time-badge">
              <span className={`time-dot ${isNowcast ? 'nowcast' : 'past'}`} />
              <strong>{timeFormatted}</strong> ({isNowcast ? '🔮 Predicted' : '📡 Observed'})
            </span>
            <span className="scrubber-frame-count">
              Frame {currentFrameIdx + 1} of {Math.max(1, allFrames.length)}
            </span>
          </div>

          <div className="timeline-slider-wrapper">
            <span className="timeline-bound-label">-50m</span>
            <input
              type="range"
              min={0}
              max={Math.max(0, allFrames.length - 1)}
              value={currentFrameIdx}
              onChange={(e) => {
                setIsPlaying(false);
                setCurrentFrameIdx(Number(e.target.value));
              }}
              className="radar-timeline-slider"
              data-testid="radar-timeline-slider"
              aria-label="Radar Timeline Scrubber"
            />
            <span className="timeline-bound-label">+30m</span>
          </div>
        </div>

        {/* Row 2: Playback Action Row (Step back, Large Play/Pause, Step forward, Speed) */}
        <div className="radar-action-row">
          <div className="playback-buttons-group">
            <button
              type="button"
              className="ctrl-btn step-btn"
              onClick={() => setCurrentFrameIdx((i) => Math.max(0, i - 1))}
              title="Step Backward 10 minutes"
              data-testid="radar-step-back"
            >
              ⏮ -10m
            </button>
            <button
              type="button"
              className={`ctrl-btn play-btn-prominent ${isPlaying ? 'active' : ''}`}
              onClick={() => setIsPlaying(!isPlaying)}
              title={isPlaying ? 'Pause Radar Loop' : 'Play Radar Loop'}
              data-testid="radar-play-pause-btn"
            >
              {isPlaying ? '⏸ Pause' : '▶ Play Loop'}
            </button>
            <button
              type="button"
              className="ctrl-btn step-btn"
              onClick={() => setCurrentFrameIdx((i) => Math.min(allFrames.length - 1, i + 1))}
              title="Step Forward 10 minutes"
              data-testid="radar-step-forward"
            >
              +10m ⏭
            </button>
          </div>

          <div className="speed-selector-group">
            <span className="speed-group-title">Speed:</span>
            <button
              type="button"
              className={`speed-pill ${playbackSpeed === 0.5 ? 'active' : ''}`}
              onClick={() => setPlaybackSpeed(0.5)}
            >
              0.5x
            </button>
            <button
              type="button"
              className={`speed-pill ${playbackSpeed === 1 ? 'active' : ''}`}
              onClick={() => setPlaybackSpeed(1)}
            >
              1x
            </button>
            <button
              type="button"
              className={`speed-pill ${playbackSpeed === 2 ? 'active' : ''}`}
              onClick={() => setPlaybackSpeed(2)}
            >
              2x
            </button>
          </div>
        </div>

        {/* Row 3: Visual Settings (Palette Dropdown & Opacity Slider) */}
        <div className="radar-settings-grid">
          <div className="radar-setting-field">
            <label htmlFor="radarSchemeSelect" className="setting-label">
              🎨 Color Palette:
            </label>
            <select
              id="radarSchemeSelect"
              value={selectedScheme}
              onChange={(e) => handleSchemeChange(Number(e.target.value))}
              className="radar-scheme-select"
              data-testid="radar-scheme-select"
            >
              <option value={1}>Original Universal</option>
              <option value={2}>Universal Blue</option>
              <option value={4}>The Weather Channel</option>
              <option value={6}>NEXRAD Level-III</option>
            </select>
          </div>

          <div className="radar-setting-field">
            <div className="setting-label-row">
              <label htmlFor="radarOpacityRange" className="setting-label">
                👁️ Radar Opacity:
              </label>
              <span className="opacity-val-badge">{Math.round(selectedOpacity * 100)}%</span>
            </div>
            <input
              id="radarOpacityRange"
              type="range"
              min={0.2}
              max={1.0}
              step={0.05}
              value={selectedOpacity}
              onChange={(e) => handleOpacityChange(Number(e.target.value))}
              className="radar-opacity-slider"
              data-testid="radar-opacity-slider"
            />
          </div>
        </div>

        {/* Row 4: Helper Instructions / Legend */}
        <div className="radar-help-footer">
          <div className="help-tip-text">
            💡 <strong>Controls:</strong> Tap <strong>▶ Play</strong> to loop radar frames. Drag slider to scrub time. Use <strong>＋</strong>/<strong>－</strong> to zoom.
          </div>
        </div>
      </div>
    </div>
  );
};
