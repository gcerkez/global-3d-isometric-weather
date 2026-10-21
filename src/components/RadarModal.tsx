import React, { useState, useEffect, useRef, useCallback } from 'react';
import { radarService } from '../services/radar/RadarService';
import { RadarFrame, RadarMetadata } from '../types/weather';

interface RadarModalProps {
  isOpen: boolean;
  onClose: () => void;
  lat: number;
  lon: number;
  locationName: string;
  colorScheme?: number;
  opacity?: number;
  onChangeColorScheme?: (scheme: number) => void;
  onChangeOpacity?: (opacity: number) => void;
}

export const RadarModal: React.FC<RadarModalProps> = ({
  isOpen,
  onClose,
  lat,
  lon,
  locationName,
  colorScheme = 1,
  opacity = 0.75,
  onChangeColorScheme,
  onChangeOpacity,
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

  // Load radar metadata on open
  useEffect(() => {
    if (!isOpen) return;
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
  }, [isOpen]);

  // Handle Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === ' ' || e.code === 'Space') {
        e.preventDefault();
        setIsPlaying((p) => !p);
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

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
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    // Deep dark background
    ctx.fillStyle = '#070d18';
    ctx.fillRect(0, 0, width, height);

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
    const canvasCenterY = height / 2;

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
            drawRadarOverlayHUD(ctx, width, height, canvasCenterX, canvasCenterY);
          };
          radarImg.onerror = () => {
            ctx.restore();
            drawRadarOverlayHUD(ctx, width, height, canvasCenterX, canvasCenterY);
          };
        };
      }
    }

    // Fallback immediate HUD draw
    drawRadarOverlayHUD(ctx, width, height, canvasCenterX, canvasCenterY);
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
    if (isOpen && metadata) {
      renderMap();
    }
  }, [isOpen, metadata, currentFrameIdx, zoom, selectedScheme, selectedOpacity, renderMap]);

  if (!isOpen) return null;

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
    <div className="modal-backdrop" onClick={onClose} data-testid="radar-modal-backdrop">
      <div
        className="modal-window radar-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        data-testid="radar-modal"
      >
        {/* Header */}
        <div className="modal-header">
          <div className="radar-title-box">
            <span className="radar-live-badge" data-testid="radar-live-badge">
              LIVE DOPPLER
            </span>
            <h2 className="modal-title" data-testid="radar-modal-title">
              Interactive Weather Radar • {locationName}
            </h2>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close Radar View"
            data-testid="close-radar-modal-btn"
          >
            ✕
          </button>
        </div>

        {/* Viewport */}
        <div className="radar-viewport-container">
          <canvas
            ref={canvasRef}
            width={720}
            height={440}
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

        {/* Timeline & Playback Bar */}
        <div className="radar-timeline-controls">
          <div className="playback-buttons">
            <button
              type="button"
              className="ctrl-btn"
              onClick={() => setCurrentFrameIdx((i) => Math.max(0, i - 1))}
              title="Step Backward"
              data-testid="radar-step-back"
            >
              ⏮
            </button>
            <button
              type="button"
              className={`ctrl-btn play-btn ${isPlaying ? 'active' : ''}`}
              onClick={() => setIsPlaying(!isPlaying)}
              title={isPlaying ? 'Pause Radar Loop' : 'Play Radar Loop'}
              data-testid="radar-play-pause-btn"
            >
              {isPlaying ? '⏸' : '▶'}
            </button>
            <button
              type="button"
              className="ctrl-btn"
              onClick={() => setCurrentFrameIdx((i) => Math.min(allFrames.length - 1, i + 1))}
              title="Step Forward"
              data-testid="radar-step-forward"
            >
              ⏭
            </button>
          </div>

          {/* Timeline Range Slider */}
          <div className="timeline-slider-box">
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
          </div>

          {/* Speed Selector */}
          <div className="speed-selector">
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

        {/* Settings Bar (Color Scheme & Opacity) */}
        <div className="radar-settings-bar">
          <div className="radar-setting-group">
            <label htmlFor="radarSchemeSelect" className="setting-label">Palette:</label>
            <select
              id="radarSchemeSelect"
              value={selectedScheme}
              onChange={(e) => handleSchemeChange(Number(e.target.value))}
              className="radar-scheme-select"
              data-testid="radar-scheme-select"
            >
              <option value={1}>Original (Universal)</option>
              <option value={2}>Universal Blue</option>
              <option value={4}>The Weather Channel</option>
              <option value={6}>NEXRAD Level-III</option>
            </select>
          </div>

          <div className="radar-setting-group">
            <label htmlFor="radarOpacityRange" className="setting-label">
              Opacity: {Math.round(selectedOpacity * 100)}%
            </label>
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
      </div>
    </div>
  );
};
