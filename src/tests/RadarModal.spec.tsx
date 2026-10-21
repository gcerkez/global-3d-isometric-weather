import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { RadarModal } from '../components/RadarModal';
import { radarService } from '../services/radar/RadarService';

describe('RadarModal Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();

    // Mock HTMLCanvasElement getContext
    HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue({
      clearRect: vi.fn(),
      fillRect: vi.fn(),
      strokeRect: vi.fn(),
      beginPath: vi.fn(),
      arc: vi.fn(),
      fill: vi.fn(),
      stroke: vi.fn(),
      moveTo: vi.fn(),
      lineTo: vi.fn(),
      fillText: vi.fn(),
      save: vi.fn(),
      restore: vi.fn(),
      setLineDash: vi.fn(),
      drawImage: vi.fn(),
    }) as unknown as typeof HTMLCanvasElement.prototype.getContext;

    // Mock fetchRadarMetadata with sample frames
    vi.spyOn(radarService, 'fetchRadarMetadata').mockResolvedValue({
      host: 'https://tilecache.rainviewer.com',
      past: [
        { time: 1792548000, path: '/v2/radar/past_1', isNowcast: false },
        { time: 1792549000, path: '/v2/radar/past_2', isNowcast: false },
      ],
      nowcast: [
        { time: 1792550000, path: '/v2/radar/nowcast_1', isNowcast: true },
      ],
      generated: 1792549000,
    });
  });

  it('does not render when isOpen is false', () => {
    const { container } = render(
      <RadarModal
        isOpen={false}
        onClose={vi.fn()}
        lat={37.2388}
        lon={-76.5097}
        locationName="Yorktown, VA"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders radar modal with location title and controls when open', async () => {
    const handleClose = vi.fn();
    render(
      <RadarModal
        isOpen={true}
        onClose={handleClose}
        lat={37.2388}
        lon={-76.5097}
        locationName="Yorktown, VA"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('radar-modal')).toBeInTheDocument();
    });
    expect(screen.getByTestId('radar-modal-title')).toHaveTextContent(/Yorktown, VA/i);

    // Close button works
    fireEvent.click(screen.getByTestId('close-radar-modal-btn'));
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('supports playback pause/play toggle and timeline scrubbing', async () => {
    render(
      <RadarModal
        isOpen={true}
        onClose={vi.fn()}
        lat={37.2388}
        lon={-76.5097}
        locationName="Yorktown, VA"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('radar-play-pause-btn')).toBeInTheDocument();
    });

    const playPauseBtn = screen.getByTestId('radar-play-pause-btn');
    expect(playPauseBtn).toHaveTextContent('⏸'); // Initially playing

    // Toggle pause
    fireEvent.click(playPauseBtn);
    expect(playPauseBtn).toHaveTextContent('▶');

    // Scrub timeline
    const slider = screen.getByTestId('radar-timeline-slider');
    fireEvent.change(slider, { target: { value: '0' } });
    expect((slider as HTMLInputElement).value).toBe('0');
  });

  it('triggers palette and opacity changes', async () => {
    const handleChangeScheme = vi.fn();
    const handleChangeOpacity = vi.fn();

    render(
      <RadarModal
        isOpen={true}
        onClose={vi.fn()}
        lat={37.2388}
        lon={-76.5097}
        locationName="Yorktown, VA"
        onChangeColorScheme={handleChangeScheme}
        onChangeOpacity={handleChangeOpacity}
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('radar-scheme-select')).toBeInTheDocument();
    });

    const select = screen.getByTestId('radar-scheme-select');
    fireEvent.change(select, { target: { value: '4' } });
    expect(handleChangeScheme).toHaveBeenCalledWith(4);

    const opacitySlider = screen.getByTestId('radar-opacity-slider');
    fireEvent.change(opacitySlider, { target: { value: '0.9' } });
    expect(handleChangeOpacity).toHaveBeenCalledWith(0.9);
  });

  it('supports zoom in and zoom out buttons', async () => {
    render(
      <RadarModal
        isOpen={true}
        onClose={vi.fn()}
        lat={37.2388}
        lon={-76.5097}
        locationName="Yorktown, VA"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('radar-canvas')).toBeInTheDocument();
    });

    const zoomIn = screen.getByTestId('radar-zoom-in');
    const zoomOut = screen.getByTestId('radar-zoom-out');
    const resetCenter = screen.getByTestId('radar-reset-center');

    fireEvent.click(zoomIn);
    fireEvent.click(zoomOut);
    fireEvent.click(resetCenter);
    expect(screen.getByTestId('radar-canvas')).toBeInTheDocument();
  });
});
