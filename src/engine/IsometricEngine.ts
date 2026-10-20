export interface EngineState {
  locationName: string;
  latitude: number;
  longitude: number;
  dioramaTheme: string;
  renderMode: string;
  timeMode: string;
  weatherOverride: string;
  particleIntensity: number;
  cameraZoom: number;
  cameraPanX: number;
  cameraPanY: number;
  tempF: number;
  humidity: number;
  windMph: number;
  windDeg: number;
  weatherCode: number;
  isDay: boolean;
  isGoldenHour: boolean;
  sevenDayDetailed: any[];
}

export class IsometricEngine {
  public canvas: HTMLCanvasElement;
  public ctx: CanvasRenderingContext2D | null;
  public state: EngineState;
  private imageCache: Map<string, HTMLImageElement> = new Map();
  private animId: number | null = null;
  private startTime: number = Date.now();
  private onHudUpdate?: (state: EngineState) => void;

  constructor(canvas: HTMLCanvasElement, options?: { onHudUpdate?: (st: EngineState) => void }) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.onHudUpdate = options?.onHudUpdate;

    this.state = {
      locationName: 'Yorktown, VA',
      latitude: 37.2388,
      longitude: -76.5097,
      dioramaTheme: 'yorktown',
      renderMode: 'hybrid',
      timeMode: 'realtime',
      weatherOverride: 'realtime',
      particleIntensity: 0.50,
      cameraZoom: 1.0,
      cameraPanX: 0.0,
      cameraPanY: 0.0,
      tempF: 68,
      humidity: 50,
      windMph: 7,
      windDeg: 180,
      weatherCode: 0,
      isDay: true,
      isGoldenHour: false,
      sevenDayDetailed: [],
    };

    this.startLoop();
  }

  public computeMoonPhase(dateObj: Date = new Date()): { phase: number; name: string; emoji: string } {
    const synodicDays = 29.530588853;
    const refNewMoonMs = Date.UTC(2000, 0, 6, 18, 14, 0);
    const daysSince = (dateObj.getTime() - refNewMoonMs) / 86400000.0;
    let phase = (daysSince % synodicDays) / synodicDays;
    if (phase < 0) phase += 1.0;

    let name = 'New Moon';
    let emoji = '🌑';
    if (phase < 0.03 || phase >= 0.97) { name = 'New Moon'; emoji = '🌑'; }
    else if (phase < 0.22) { name = 'Waxing Crescent'; emoji = '🌒'; }
    else if (phase < 0.28) { name = 'First Quarter'; emoji = '🌓'; }
    else if (phase < 0.47) { name = 'Waxing Gibbous'; emoji = '🌔'; }
    else if (phase < 0.53) { name = 'Full Moon'; emoji = '🌕'; }
    else if (phase < 0.72) { name = 'Waning Gibbous'; emoji = '🌖'; }
    else if (phase < 0.78) { name = 'Last Quarter'; emoji = '🌗'; }
    else { name = 'Waning Crescent'; emoji = '🌘'; }

    return { phase, name, emoji };
  }

  public setLocation(name: string, lat: number, lon: number): void {
    this.state.locationName = name;
    this.state.latitude = lat;
    this.state.longitude = lon;
    this.calculateSunPosition();
  }

  public setDioramaTheme(theme: string): void {
    this.state.dioramaTheme = theme;
  }

  public adjustCamera(action: 'zoom_in' | 'zoom_out' | 'pan_left' | 'pan_right' | 'reset'): void {
    switch (action) {
      case 'zoom_in':
        this.state.cameraZoom = Math.min(2.40, this.state.cameraZoom * 1.15);
        break;
      case 'zoom_out':
        this.state.cameraZoom = Math.max(0.65, this.state.cameraZoom / 1.15);
        break;
      case 'pan_left':
        this.state.cameraPanX = Math.max(-180, this.state.cameraPanX - 28);
        break;
      case 'pan_right':
        this.state.cameraPanX = Math.min(180, this.state.cameraPanX + 28);
        break;
      case 'reset':
        this.state.cameraZoom = 1.0;
        this.state.cameraPanX = 0.0;
        break;
    }
  }

  public updateTelemetry(telemetry: Partial<EngineState>): void {
    Object.assign(this.state, telemetry);
    this.calculateSunPosition();
    if (this.onHudUpdate) {
      this.onHudUpdate(this.state);
    }
  }

  public refreshAll(): void {
    this.calculateSunPosition();
    if (this.onHudUpdate) {
      this.onHudUpdate(this.state);
    }
  }

  public destroy(): void {
    if (this.animId !== null) {
      cancelAnimationFrame(this.animId);
      this.animId = null;
    }
  }

  private startLoop(): void {
    const render = () => {
      this.drawFrame();
      this.animId = requestAnimationFrame(render);
    };
    this.animId = requestAnimationFrame(render);
  }

  private calculateSunPosition(): void {
    const now = new Date();
    const hour = now.getHours() + now.getMinutes() / 60;
    this.state.isDay = hour >= 6.2 && hour < 19.2;
    this.state.isGoldenHour = (hour >= 6.0 && hour <= 7.2) || (hour >= 18.0 && hour <= 19.2);
  }

  private getEffectiveTimeState(): 'day' | 'sunset' | 'night' {
    if (this.state.timeMode !== 'realtime') {
      return this.state.timeMode as 'day' | 'sunset' | 'night';
    }
    if (!this.state.isDay) return 'night';
    if (this.state.isGoldenHour) return 'sunset';
    return 'day';
  }

  private getEffectiveWeatherCategory(): string {
    if (this.state.weatherOverride !== 'realtime') {
      return this.state.weatherOverride;
    }
    const c = this.state.weatherCode;
    if (c === 0) return 'clear';
    if (c <= 2) return 'partly_cloudy';
    if (c === 3) return 'overcast';
    if (c === 45 || c === 48) return 'fog';
    if ((c >= 51 && c <= 67) || (c >= 80 && c <= 82)) return 'rain';
    if ((c >= 71 && c <= 77) || c === 85 || c === 86) return 'snow';
    if (c >= 95) return 'thunderstorm';
    return 'clear';
  }

  private getImage(theme: string, timeState: 'day' | 'sunset' | 'night'): HTMLImageElement {
    const suffix = timeState === 'day' ? '' : `_${timeState}`;
    const filename = `${theme}_island${suffix}.png`;
    const fullPath = `./images/${filename}`;

    if (this.imageCache.has(fullPath)) {
      return this.imageCache.get(fullPath)!;
    }

    const img = new Image();
    img.src = fullPath;
    this.imageCache.set(fullPath, img);
    return img;
  }

  /**
   * Renders a multi-puff 3D shaded volumetric cloud with ambient underside,
   * mid-tone body, and sunlit top highlights matching the diorama clay/voxel style.
   */
  public drawIsometricPuffyCloud(
    ctx: CanvasRenderingContext2D,
    cx: Float64Array | number,
    cy: number,
    cScale: number,
    s: number,
    isStormy: boolean,
    isSunset: boolean,
    isNight: boolean
  ): void {
    ctx.save();
    const baseColor = isStormy ? (isNight ? "#243242" : "#5b7083") : (isSunset ? "#9e7b96" : (isNight ? "#334860" : "#c2d4e6"));
    const midColor  = isStormy ? (isNight ? "#334559" : "#7b90a5") : (isSunset ? "#e6a58e" : (isNight ? "#4d6782" : "#e4eef7"));
    const topColor  = isStormy ? (isNight ? "#485e75" : "#9cb0c4") : (isSunset ? "#ffe0b5" : (isNight ? "#6c89a8" : "#ffffff"));

    const puffs = [
      { dx: -38, dy: 8,  r: 22 },
      { dx: -16, dy: -6, r: 30 },
      { dx: 14,  dy: -3, r: 27 },
      { dx: 38,  dy: 9,  r: 20 },
      { dx: -4,  dy: 10, r: 25 },
      { dx: 20,  dy: 11, r: 22 }
    ];

    const totalScale = cScale * s;
    const xCenter = typeof cx === 'number' ? cx : Number(cx);

    // Layer 1: Ambient underside / base shadow
    ctx.fillStyle = baseColor;
    for (const p of puffs) {
      ctx.beginPath();
      ctx.arc(xCenter + p.dx * totalScale, cy + (p.dy + 6) * totalScale, p.r * totalScale, 0, Math.PI * 2);
      ctx.fill();
    }

    // Layer 2: Mid-tone volume body
    ctx.fillStyle = midColor;
    for (const p of puffs) {
      ctx.beginPath();
      ctx.arc(xCenter + p.dx * totalScale, cy + (p.dy + 2) * totalScale, p.r * 0.96 * totalScale, 0, Math.PI * 2);
      ctx.fill();
    }

    // Layer 3: Sunlit top highlight domes
    ctx.fillStyle = topColor;
    for (const p of puffs) {
      ctx.beginPath();
      ctx.arc(xCenter + (p.dx - 3) * totalScale, cy + (p.dy - 2) * totalScale, p.r * 0.86 * totalScale, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.restore();
  }

  private drawFrame(): void {
    const ctx = this.ctx;
    if (!ctx) return;

    const w = this.canvas.width;
    const h = this.canvas.height;
    if (w <= 0 || h <= 0) return;

    ctx.clearRect(0, 0, w, h);

    const t = (Date.now() - this.startTime) / 1000.0;
    const tState = this.getEffectiveTimeState();
    const cat = this.getEffectiveWeatherCategory();
    const s = Math.min(w / 1376.0, h / 768.0);

    // 1. Sky backdrop
    const skyGrad = ctx.createLinearGradient(0, 0, 0, h);
    if (tState === 'night') {
      skyGrad.addColorStop(0, '#0a1420');
      skyGrad.addColorStop(1, '#16283d');
    } else if (tState === 'sunset') {
      skyGrad.addColorStop(0, '#3a506b');
      skyGrad.addColorStop(1, '#f29a6b');
    } else if (cat === 'rain' || cat === 'thunderstorm') {
      skyGrad.addColorStop(0, '#4a6278');
      skyGrad.addColorStop(1, '#7b92a8');
    } else {
      skyGrad.addColorStop(0, '#7fc4f2');
      skyGrad.addColorStop(1, '#a8dbf8');
    }
    ctx.fillStyle = skyGrad;
    ctx.fillRect(0, 0, w, h);

    // Stars in night sky
    if (tState === 'night') {
      ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
      for (let i = 0; i < 30; i++) {
        const sx = ((i * 137.5) % w);
        const sy = ((i * 83.3) % (h * 0.45));
        const sr = (i % 3 === 0) ? 1.6 : 1.0;
        ctx.beginPath();
        ctx.arc(sx, sy, sr, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // 2. Diorama Island rendering
    const img = this.getImage(this.state.dioramaTheme, tState);
    const aspect = 1376.0 / 768.0;
    let dw = w;
    let dh = dw / aspect;
    if (dh > h) {
      dh = h;
      dw = dh * aspect;
    }

    const shiftX = (this.state.cameraPanX / 688.0) * w;
    const dx = (w - dw) / 2 + shiftX;
    const dy = (h - dh) / 2;

    ctx.save();
    const pivotX = dx + dw / 2;
    const pivotY = dy + dh * 0.56;
    ctx.translate(pivotX, pivotY);
    ctx.scale(this.state.cameraZoom, this.state.cameraZoom);
    ctx.translate(-pivotX, -pivotY);

    if (img && img.complete && img.naturalWidth > 0) {
      ctx.drawImage(img, dx, dy, dw, dh);
    }
    ctx.restore();

    // 3. 3D Drifting Volumetric Puffy Clouds & Ground Shadows
    const isCloudy = (cat === 'partly_cloudy' || cat === 'overcast' || cat === 'rain' || cat === 'thunderstorm' || cat === 'snow');
    if (isCloudy) {
      const cloudCount = (cat === 'partly_cloudy' || cat === 'snow') ? 3 : 4;
      const isStormy = (cat === 'rain' || cat === 'thunderstorm' || cat === 'overcast');

      // First pass: Ground shadows on island terrain
      ctx.fillStyle = tState === 'night' ? 'rgba(0, 0, 0, 0.08)' : 'rgba(15, 35, 60, 0.11)';
      for (let ci = 0; ci < cloudCount; ci++) {
        const cProg = ((ci * 0.31 + t * 0.025) % 1.0);
        const cloudX = (cProg * 1.30 - 0.15) * w;
        if (cloudX >= dx + 0.05 * dw && cloudX <= dx + 0.95 * dw) {
          const shadowX = cloudX - 12 * s;
          const shadowY = dy + (0.28 + (ci * 0.15) % 0.38) * dh;
          const cw = (70 + ci * 22) * s;
          const ch = cw * 0.38;
          ctx.beginPath();
          ctx.ellipse(shadowX, shadowY, cw, ch, 0, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      // Second pass: 3D volumetric puffy clouds floating in sky render view
      for (let ci = 0; ci < cloudCount; ci++) {
        const cProg = ((ci * 0.31 + t * 0.025) % 1.0);
        const cloudX = (cProg * 1.30 - 0.15) * w;
        const cloudY = 36 * s + (24 + (ci * 38) % 76) * s;
        const cloudScale = (0.75 + (ci % 3) * 0.18) * (isStormy ? 1.15 : 1.0);
        this.drawIsometricPuffyCloud(ctx, cloudX, cloudY, cloudScale, s, isStormy, tState === 'sunset', tState === 'night');
      }
    }

    // 4. Fog / Mist banks
    if (cat === 'fog') {
      for (let fg = 0; fg < 4; fg++) {
        const fogProg = ((fg * 0.28 + t * 0.04) % 1.0);
        const fogX = (fogProg * 1.3 - 0.15) * w;
        const fogY = dy + (0.35 + (fg * 0.14) % 0.45) * dh;
        const fogW = (120 + fg * 30) * s;
        const fogH = (26 + fg * 8) * s;
        ctx.fillStyle = `rgba(225, 238, 248, ${(0.22 + fg * 0.04)})`;
        ctx.beginPath();
        ctx.ellipse(fogX, fogY, fogW, fogH, 0, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // 5. Thunderstorm Lightning
    if (cat === 'thunderstorm' && Math.sin(t * 7.5) > 0.94) {
      ctx.fillStyle = 'rgba(240, 248, 255, 0.28)';
      ctx.fillRect(0, 0, w, h);

      ctx.strokeStyle = 'rgba(255, 255, 255, 0.95)';
      ctx.lineWidth = 2.4 * s;
      ctx.beginPath();
      const bx = dx + 0.38 * dw;
      const by = dy + 0.10 * dh;
      ctx.moveTo(bx, by);
      ctx.lineTo(bx + 16 * s, by + 32 * s);
      ctx.lineTo(bx - 8 * s, by + 60 * s);
      ctx.lineTo(bx + 20 * s, by + 98 * s);
      ctx.lineTo(bx + 4 * s, by + 130 * s);
      ctx.stroke();
    }

    // 6. Rain & Snow
    const pScale = Math.max(0.15, this.state.particleIntensity);
    if (cat === 'rain' || cat === 'thunderstorm') {
      const dropCount = Math.max(12, Math.round(50 * pScale));
      ctx.strokeStyle = 'rgba(215, 240, 255, 0.65)';
      ctx.lineWidth = 1.4 * s;
      ctx.beginPath();
      for (let i = 0; i < dropCount; i++) {
        const prog = ((t * 0.9 + i * 0.117) % 1.0);
        const rx = ((i * 0.6180339 + 0.12) % 1.0) * w - prog * 0.08 * w;
        const ry = prog * (h + 30) - 15;
        ctx.moveTo(rx, ry);
        ctx.lineTo(rx - 4 * s, ry + 16 * s);
      }
      ctx.stroke();

      // Splash ripples
      ctx.strokeStyle = 'rgba(225, 245, 255, 0.40)';
      ctx.lineWidth = 1.0 * s;
      for (let j = 0; j < 5; j++) {
        const spProg = ((t * 0.45 + j * 0.23) % 1.0);
        const spX = dx + (0.26 + ((j * 0.19) % 0.48)) * dw;
        const spY = dy + (0.34 + ((j * 0.17) % 0.40)) * dh;
        ctx.beginPath();
        ctx.ellipse(spX, spY, (4 + spProg * 8) * s, (2 + spProg * 4) * s, 0, 0, Math.PI * 2);
        ctx.stroke();
      }
    } else if (cat === 'snow') {
      const flakeCount = Math.max(6, Math.round(30 * pScale));
      ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
      for (let i = 0; i < flakeCount; i++) {
        const prog = ((t * 0.14 + i * 0.137) % 1.0);
        const sx = ((i * 0.6180339 + 0.08) % 1.0) * w + Math.sin(t * 1.8 + i) * 8 * s;
        const sy = prog * (h + 20) - 10;
        ctx.beginPath();
        ctx.arc(sx, sy, 2.4 * s, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }
}
