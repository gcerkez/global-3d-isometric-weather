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
    expect(screen.getByTestId('manual-refresh-btn')).toBeInTheDocument();
    expect(screen.getByTestId('location-badge')).toBeInTheDocument();
    expect(screen.getByText(/Yorktown, VA • 3D Isometric/i)).toBeInTheDocument();
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
  it('renders pure preview canvas without desktop hud overlay when #preview route is active', () => {
    window.location.hash = '#preview?lat=38.83&lon=-104.82&name=Colorado%20Springs,%20CO';

    render(<App />);

    // In preview mode, app-preview-root is rendered
    expect(screen.getByTestId('app-preview-root')).toBeInTheDocument();
    expect(screen.getByTestId('iso-canvas')).toBeInTheDocument();

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
