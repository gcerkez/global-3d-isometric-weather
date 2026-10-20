import React, { useRef, useEffect } from 'react';
import { IsometricEngine, EngineState } from '../engine/IsometricEngine';

interface WeatherCanvasProps {
  onEngineReady?: (engine: IsometricEngine) => void;
  onHudUpdate?: (state: EngineState) => void;
  onRefreshClick?: () => void;
  locationName: string;
}

export const WeatherCanvas: React.FC<WeatherCanvasProps> = ({
  onEngineReady,
  onHudUpdate,
  onRefreshClick,
  locationName,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const engineRef = useRef<IsometricEngine | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const handleResize = () => {
      const dpr = window.devicePixelRatio || 1;
      canvas.width = (window.innerWidth || 1376) * dpr;
      canvas.height = (window.innerHeight || 768) * dpr;
    };
    handleResize();
    window.addEventListener('resize', handleResize);

    const engine = new IsometricEngine(canvas, {
      onHudUpdate: (st) => {
        if (onHudUpdate) onHudUpdate(st);
      },
    });

    engineRef.current = engine;
    (window as unknown as { isoEngine: IsometricEngine }).isoEngine = engine;

    if (onEngineReady) {
      onEngineReady(engine);
    }

    return () => {
      window.removeEventListener('resize', handleResize);
      engine.destroy();
    };
  }, []);

  const handleCamera = (action: 'zoom_in' | 'zoom_out' | 'pan_left' | 'pan_right' | 'reset') => {
    if (engineRef.current) {
      engineRef.current.adjustCamera(action);
    }
  };

  return (
    <div className="canvas-wrapper" data-testid="canvas-wrapper">
      <canvas id="isoCanvas" ref={canvasRef} data-testid="iso-canvas" />

      {/* Camera Controls */}
      <div className="camera-pill" data-testid="camera-pill">
        <button
          type="button"
          onClick={() => handleCamera('zoom_out')}
          title="Zoom Out"
          data-testid="cam-zoom-out"
        >
          ➖
        </button>
        <button
          type="button"
          onClick={() => handleCamera('zoom_in')}
          title="Zoom In"
          data-testid="cam-zoom-in"
        >
          ➕
        </button>
        <button
          type="button"
          onClick={() => handleCamera('pan_left')}
          title="Pan Left"
          data-testid="cam-pan-left"
        >
          ◀
        </button>
        <button
          type="button"
          onClick={() => handleCamera('reset')}
          title="Center View"
          data-testid="cam-reset"
        >
          🎯
        </button>
        <button
          type="button"
          onClick={() => handleCamera('pan_right')}
          title="Pan Right"
          data-testid="cam-pan-right"
        >
          ▶
        </button>
      </div>

      {/* Transparent red refresh button at bottom right */}
      <button
        type="button"
        className="refresh-btn-floating"
        onClick={onRefreshClick}
        title="Manual Refresh Weather"
        data-testid="manual-refresh-btn"
      >
        ↻
      </button>

      {/* Location Badge */}
      <div className="location-badge" data-testid="location-badge">
        {locationName} • 3D Isometric
      </div>
    </div>
  );
};
