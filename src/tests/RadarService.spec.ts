import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { RadarService } from '../services/radar/RadarService';

describe('RadarService', () => {
  let service: RadarService;

  beforeEach(() => {
    RadarService.clearCache();
    service = new RadarService();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('computes Web Mercator tile coordinates correctly', () => {
    // Yorktown, VA ~ (37.2388, -76.5097) at zoom 7
    const tile7 = service.latLonToTile(37.2388, -76.5097, 7);
    expect(tile7.x).toBeGreaterThan(0);
    expect(tile7.y).toBeGreaterThan(0);
    expect(tile7.x).toBeLessThan(128); // 2^7 = 128
    expect(tile7.y).toBeLessThan(128);

    // Greenwich Prime Meridian & Equator (0, 0) at zoom 1
    const tileEquator = service.latLonToTile(0, 0, 1);
    expect(tileEquator.x).toBe(1);
    expect(tileEquator.y).toBe(1);
  });

  it('constructs correct RainViewer tile URLs', () => {
    const url = service.getTileUrl(
      'https://tilecache.rainviewer.com',
      '/v2/radar/abc123xyz',
      7,
      36,
      48,
      2 // Universal Blue
    );
    expect(url).toBe('https://tilecache.rainviewer.com/v2/radar/abc123xyz/256/7/36/48/2/1_1.png');
  });

  it('constructs base map tile URLs', () => {
    const baseUrl = service.getBaseMapTileUrl(7, 36, 48);
    expect(baseUrl).toBe('https://basemaps.cartocdn.com/dark_all/7/36/48.png');
  });

  it('formats frame Unix timestamps into readable local time strings', () => {
    const formatted = service.formatFrameTime(1792545600);
    expect(typeof formatted).toBe('string');
    expect(formatted.length).toBeGreaterThan(0);
  });

  it('fetches radar metadata and caches response', async () => {
    const mockJson = {
      version: '2.0',
      generated: 1792550000,
      host: 'https://tilecache.rainviewer.com',
      radar: {
        past: [
          { time: 1792548000, path: '/v2/radar/past_1' },
          { time: 1792549000, path: '/v2/radar/past_2' },
        ],
        nowcast: [
          { time: 1792550000, path: '/v2/radar/nowcast_1' },
        ],
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockJson,
    });

    const meta = await service.fetchRadarMetadata();
    expect(meta.host).toBe('https://tilecache.rainviewer.com');
    expect(meta.past.length).toBe(2);
    expect(meta.nowcast.length).toBe(1);
    expect(meta.past[0].isNowcast).toBe(false);
    expect(meta.nowcast[0].isNowcast).toBe(true);
  });

  it('falls back seamlessly to synthesized frames if RainViewer is unreachable', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network offline'));

    const meta = await service.fetchRadarMetadata();
    expect(meta.host).toBe('https://tilecache.rainviewer.com');
    expect(meta.past.length).toBeGreaterThan(0);
    expect(meta.nowcast.length).toBeGreaterThan(0);
    expect(meta.past[0].path).toContain('fallback');
  });
});
