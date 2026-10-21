import { RadarMetadata, RadarFrame } from '../../types/weather';

export class RadarService {
  private static cachedMetadata: RadarMetadata | null = null;
  private static lastFetchTime = 0;
  private static readonly CACHE_TTL_MS = 60_000; // 1 minute cache

  public static clearCache(): void {
    RadarService.cachedMetadata = null;
    RadarService.lastFetchTime = 0;
  }

  /**
   * Fetches the latest global weather radar frames from RainViewer API
   */
  public async fetchRadarMetadata(): Promise<RadarMetadata> {
    const now = Date.now();
    if (RadarService.cachedMetadata && now - RadarService.lastFetchTime < RadarService.CACHE_TTL_MS) {
      return RadarService.cachedMetadata;
    }

    try {
      const res = await fetch('https://api.rainviewer.com/public/weather-maps.json');
      if (!res.ok) {
        throw new Error(`RainViewer API error: ${res.status}`);
      }
      const data = await res.json();

      const host = data.host || 'https://tilecache.rainviewer.com';
      const pastFrames: RadarFrame[] = (data.radar?.past || []).map((f: { time: number; path: string }) => ({
        time: f.time,
        path: f.path,
        isNowcast: false,
      }));
      const nowcastFrames: RadarFrame[] = (data.radar?.nowcast || []).map((f: { time: number; path: string }) => ({
        time: f.time,
        path: f.path,
        isNowcast: true,
      }));

      const metadata: RadarMetadata = {
        host,
        past: pastFrames,
        nowcast: nowcastFrames,
        generated: data.generated || Math.floor(now / 1000),
      };

      RadarService.cachedMetadata = metadata;
      RadarService.lastFetchTime = now;
      return metadata;
    } catch (err) {
      console.warn('[RadarService] Failed to fetch live RainViewer maps, generating fallback frames:', err);
      return this.generateFallbackMetadata();
    }
  }

  /**
   * Constructs the tile URL for a specific radar frame
   */
  public getTileUrl(
    host: string,
    framePath: string,
    zoom: number,
    x: number,
    y: number,
    colorScheme = 1
  ): string {
    return `${host}${framePath}/256/${zoom}/${x}/${y}/${colorScheme}/1_1.png`;
  }

  /**
   * Constructs the base map tile URL using Esri World Dark Gray Base (100% free, no API key, no watermark)
   * Note: Esri REST coordinate scheme is {zoom}/{y}/{x}
   */
  public getBaseMapTileUrl(zoom: number, x: number, y: number): string {
    return `https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/${zoom}/${y}/${x}`;
  }

  /**
   * Constructs OpenStreetMap standard base map tile URL as secondary fallback
   */
  public getFallbackBaseMapTileUrl(zoom: number, x: number, y: number): string {
    return `https://tile.openstreetmap.org/${zoom}/${x}/${y}.png`;
  }

  /**
   * Computes Web Mercator (EPSG:3857) tile coordinates from latitude & longitude
   */
  public latLonToTile(lat: number, lon: number, zoom: number): { x: number; y: number } {
    const n = Math.pow(2, zoom);
    const x = Math.floor(((lon + 180) / 360) * n);
    const latRad = (lat * Math.PI) / 180;
    const y = Math.floor(
      ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) * n
    );
    return {
      x: Math.max(0, Math.min(n - 1, x)),
      y: Math.max(0, Math.min(n - 1, y)),
    };
  }

  /**
   * Formats a Unix timestamp (seconds) into a friendly local time string
   */
  public formatFrameTime(unixSeconds: number): string {
    const d = new Date(unixSeconds * 1000);
    return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  }

  /**
   * Fallback metadata in case of offline mode or network rate limiting
   */
  private generateFallbackMetadata(): RadarMetadata {
    const nowSec = Math.floor(Date.now() / 1000);
    const past: RadarFrame[] = [];
    for (let i = 6; i >= 1; i--) {
      past.push({
        time: nowSec - i * 600,
        path: `/v2/radar/fallback_${i}`,
        isNowcast: false,
      });
    }
    const nowcast: RadarFrame[] = [];
    for (let i = 1; i <= 3; i++) {
      nowcast.push({
        time: nowSec + i * 600,
        path: `/v2/radar/nowcast_${i}`,
        isNowcast: true,
      });
    }
    return {
      host: 'https://tilecache.rainviewer.com',
      past,
      nowcast,
      generated: nowSec,
    };
  }
}

export const radarService = new RadarService();
