import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WeatherCanvas } from '../components/WeatherCanvas';
import { App } from '../App';

describe('WeatherCanvas Component', () => {
  it('renders camera controls, refresh button, and location badge when hideControls is false', () => {
    render(
      <WeatherCanvas
        locationName="Yorktown, VA"
        hideControls={false}
      />
    );

    expect(screen.getByTestId('iso-canvas')).toBeInTheDocument();
    expect(screen.getByTestId('camera-pill')).toBeInTheDocument();
    expect(screen.getByTestId('cam-zoom-out')).toBeInTheDocument();
    expect(screen.getByTestId('cam-zoom-in')).toBeInTheDocument();
    expect(screen.getByTestId('cam-pan-left')).toBeInTheDocument();
    expect(screen.getByTestId('cam-pan-up')).toBeInTheDocument();
    expect(screen.getByTestId('cam-reset')).toBeInTheDocument();
    expect(screen.getByTestId('cam-pan-down')).toBeInTheDocument();
    expect(screen.getByTestId('cam-pan-right')).toBeInTheDocument();
    expect(screen.getByTestId('manual-refresh-btn')).toBeInTheDocument();
    expect(screen.getByTestId('location-badge')).toBeInTheDocument();
    expect(screen.getByText(/Yorktown, VA • 3D Isometric/i)).toBeInTheDocument();
  });

  it('adjusts camera framing when clicking pan and zoom controls', () => {
    let capturedEngine: any = null;
    render(
      <WeatherCanvas
        locationName="Yorktown, VA"
        hideControls={false}
        onEngineReady={(engine) => {
          capturedEngine = engine;
        }}
      />
    );

    expect(capturedEngine).not.toBeNull();
    expect(capturedEngine.state.cameraZoom).toBe(1.0);
    expect(capturedEngine.state.cameraPanX).toBe(0.0);
    expect(capturedEngine.state.cameraPanY).toBe(0.0);

    // Pan Up
    screen.getByTestId('cam-pan-up').click();
    expect(capturedEngine.state.cameraPanY).toBe(-24);

    // Pan Down twice
    screen.getByTestId('cam-pan-down').click();
    screen.getByTestId('cam-pan-down').click();
    expect(capturedEngine.state.cameraPanY).toBe(24);

    // Pan Left
    screen.getByTestId('cam-pan-left').click();
    expect(capturedEngine.state.cameraPanX).toBe(-28);

    // Pan Right twice
    screen.getByTestId('cam-pan-right').click();
    screen.getByTestId('cam-pan-right').click();
    expect(capturedEngine.state.cameraPanX).toBe(28);

    // Zoom In
    screen.getByTestId('cam-zoom-in').click();
    expect(capturedEngine.state.cameraZoom).toBeCloseTo(1.15, 2);

    // Reset / Center
    screen.getByTestId('cam-reset').click();
    expect(capturedEngine.state.cameraZoom).toBe(1.0);
    expect(capturedEngine.state.cameraPanX).toBe(0.0);
    expect(capturedEngine.state.cameraPanY).toBe(0.0);

    // setCameraFraming
    capturedEngine.setCameraFraming(1.5, 45, -30);
    expect(capturedEngine.state.cameraZoom).toBe(1.5);
    expect(capturedEngine.state.cameraPanX).toBe(45);
    expect(capturedEngine.state.cameraPanY).toBe(-30);
  });

  it('omits camera controls, refresh button, and location badge when hideControls is true in preview mode', () => {
    render(
      <WeatherCanvas
        locationName="Colorado Springs, CO"
        hideControls={true}
      />
    );

    expect(screen.getByTestId('iso-canvas')).toBeInTheDocument();
    expect(screen.queryByTestId('camera-pill')).toBeNull();
    expect(screen.queryByTestId('manual-refresh-btn')).toBeNull();
    expect(screen.queryByTestId('location-badge')).toBeNull();
  });
});

describe('App Standalone Preview Mode (#preview)', () => {
  it('renders pure preview canvas without desktop hud overlay when #preview route is active and initializes framing', () => {
    window.location.hash = '#preview?lat=38.83&lon=-104.82&name=Colorado%20Springs,%20CO&zoom=1.25&panX=-56&panY=48';

    render(<App />);

    // In preview mode, app-preview-root is rendered
    expect(screen.getByTestId('app-preview-root')).toBeInTheDocument();
    expect(screen.getByTestId('iso-canvas')).toBeInTheDocument();

    const engine = (window as any).isoEngine;
    expect(engine).toBeDefined();
    expect(engine.state.cameraZoom).toBe(1.25);
    expect(engine.state.cameraPanX).toBe(-56);
    expect(engine.state.cameraPanY).toBe(48);

    // Desktop HUD overlay elements MUST NOT be rendered
    expect(screen.queryByTestId('hud-card')).toBeNull();
    expect(screen.queryByTestId('hud-card-loading')).toBeNull();
    expect(screen.queryByTestId('camera-pill')).toBeNull();
    expect(screen.queryByTestId('manual-refresh-btn')).toBeNull();
    expect(screen.queryByTestId('location-badge')).toBeNull();

    // Reset window hash
    window.location.hash = '';
  });
});
