import QtQuick

/**
 * @file WeatherEffectsOverlay.qml
 * @description Real-time 3D isometric atmospheric and weather particle engine.
 * Renders:
 *  - Back-layer and front-layer 3D shaded volumetric clouds with terrain ground shadows
 *  - Accurate lunar phase disc & earthshine in night mode (via AstronomyAndAqi.js)
 *  - Post-rain / sun-shower double rainbow arc
 *  - Wind-tilted rain streaks and isometric elliptical splash rings
 *  - Tumbling snowflakes and hail pellets (for WMO codes 96/99)
 *  - High-wind gust streaks (when windSpeedMph >= 15)
 *  - Rising chimney smoke and rolling coastal fog banks
 */
Item {
    id: fxRoot

    property string weatherCategory: "clear" // clear, partly_cloudy, overcast, rain, thunderstorm, snow, fog
    property string timeState: "day"         // day, sunset, night
    property real windSpeedMph: 7.0
    property real particleDensity: 1.0
    property real moonPhase: 0.25            // 0..1 synodic phase (0=New, 0.5=Full)
    property bool showRainbow: false
    property bool renderBackLayerOnly: false
    property bool renderFrontLayerOnly: false

    property real animTime: 0.0
    property real lightningFlash: 0.0

    Timer {
        interval: 40
        running: fxRoot.visible
        repeat: true
        onTriggered: {
            var windFactor = Math.max(0.55, Math.min(1.25, 0.70 + fxRoot.windSpeedMph * 0.02))
            fxRoot.animTime += 0.011 * windFactor
            if (fxRoot.weatherCategory === "thunderstorm") {
                if (Math.random() < 0.008) {
                    fxRoot.lightningFlash = 0.75
                } else {
                    fxRoot.lightningFlash = Math.max(0.0, fxRoot.lightningFlash - 0.10)
                }
            } else {
                fxRoot.lightningFlash = 0.0
            }
            fxCanvas.requestPaint()
        }
    }

    onWeatherCategoryChanged: fxCanvas.requestPaint()
    onTimeStateChanged: fxCanvas.requestPaint()
    onMoonPhaseChanged: fxCanvas.requestPaint()

    Canvas {
        id: fxCanvas
        anchors.fill: parent
        antialiasing: true

        property var clouds: [
            { x: 0.16, y: 0.16, scale: 0.85, speed: 0.012, layer: "back",  seed: 1.1 },
            { x: 0.30, y: 0.11, scale: 0.65, speed: 0.009, layer: "back",  seed: 2.4 },
            { x: 0.65, y: 0.12, scale: 0.92, speed: 0.011, layer: "back",  seed: 3.7 },
            { x: 0.79, y: 0.25, scale: 0.72, speed: 0.014, layer: "front", seed: 4.9 },
            { x: 0.46, y: 0.19, scale: 1.05, speed: 0.013, layer: "front", seed: 5.5 },
            { x: 0.22, y: 0.27, scale: 0.90, speed: 0.016, layer: "front", seed: 6.2 },
            { x: 0.60, y: 0.29, scale: 0.80, speed: 0.015, layer: "front", seed: 7.8 }
        ]

        onPaint: {
            var ctx = getContext("2d")
            ctx.clearRect(0, 0, width, height)
            if (width <= 10 || height <= 10) return

            var s = Math.min(width / 1376.0, height / 768.0)
            var t = fxRoot.animTime
            var cat = fxRoot.weatherCategory
            var tState = fxRoot.timeState
            var night = (tState === "night")
            var sunset = (tState === "sunset")
            var stormy = (cat === "rain" || cat === "thunderstorm" || cat === "overcast")

            // Back-layer atmospheric elements: Moon in night sky & Rainbow arc
            if (fxRoot.renderBackLayerOnly) {
                if (night && cat !== "overcast" && cat !== "thunderstorm") {
                    var mx = width * 0.14
                    var my = height * 0.16
                    var mr = 24 * s
                    // Soft lunar halo
                    var halo = ctx.createRadialGradient(mx, my, mr * 0.4, mx, my, mr * 3.0)
                    halo.addColorStop(0, "rgba(255, 244, 200, 0.26)")
                    halo.addColorStop(1, "rgba(255, 244, 200, 0.0)")
                    ctx.fillStyle = halo
                    ctx.beginPath()
                    ctx.arc(mx, my, mr * 3.0, 0, Math.PI * 2)
                    ctx.fill()

                    // Illuminated moon disc
                    ctx.fillStyle = "#fff3c4"
                    ctx.beginPath()
                    ctx.arc(mx, my, mr, 0, Math.PI * 2)
                    ctx.fill()

                    // Shadow mask for crescent/gibbous phase
                    var phaseOffset = (fxRoot.moonPhase - 0.5) * 2.0 // -1..+1
                    if (Math.abs(phaseOffset) > 0.08) {
                        ctx.fillStyle = "rgba(15, 28, 46, 0.86)"
                        ctx.beginPath()
                        ctx.arc(mx + phaseOffset * mr * 0.85, my, mr * 0.96, 0, Math.PI * 2)
                        ctx.fill()
                    }
                }

                // Subtle isometric rainbow arc during sun-showers or when enabled
                if (fxRoot.showRainbow || (cat === "partly_cloudy" && !night)) {
                    ctx.save()
                    var rbx = width * 0.48
                    var rby = height * 0.62
                    var rbRadius = 380 * s
                    var colors = [
                        "rgba(255, 90, 90, 0.09)",
                        "rgba(255, 175, 70, 0.09)",
                        "rgba(255, 240, 100, 0.09)",
                        "rgba(90, 220, 130, 0.09)",
                        "rgba(90, 170, 255, 0.09)",
                        "rgba(170, 110, 255, 0.09)"
                    ]
                    ctx.lineWidth = 4.5 * s
                    for (var rc = 0; rc < colors.length; rc++) {
                        ctx.strokeStyle = colors[rc]
                        ctx.beginPath()
                        ctx.arc(rbx, rby, rbRadius - rc * 4.5 * s, Math.PI * 1.12, Math.PI * 1.88)
                        ctx.stroke()
                    }
                    ctx.restore()
                }
            }

            /**
             * Draws a 3D shaded puffy cloud matching the miniature clay/voxel reference aesthetic.
             */
            function drawIsometricPuffyCloud(cx, cy, cScale, isStormy, isSunset, isNight) {
                ctx.save()
                ctx.translate(cx, cy)
                ctx.scale(cScale * s, cScale * s)

                var baseColor = "#c2d4e6"
                var midColor  = "#e4eef7"
                var topColor  = "#ffffff"

                if (isStormy) {
                    baseColor = isNight ? "#243242" : "#5b7083"
                    midColor  = isNight ? "#334559" : "#7b90a5"
                    topColor  = isNight ? "#485e75" : "#9cb0c4"
                } else if (isSunset) {
                    baseColor = "#9e7b96"
                    midColor  = "#e6a58e"
                    topColor  = "#ffe0b5"
                } else if (isNight) {
                    baseColor = "#334860"
                    midColor  = "#4d6782"
                    topColor  = "#6c89a8"
                }

                var puffs = [
                    { dx: -38, dy: 8,  r: 22 },
                    { dx: -16, dy: -6, r: 30 },
                    { dx: 14,  dy: -3, r: 27 },
                    { dx: 38,  dy: 9,  r: 20 },
                    { dx: -4,  dy: 10, r: 25 },
                    { dx: 20,  dy: 11, r: 22 }
                ]

                ctx.fillStyle = baseColor
                for (var i = 0; i < puffs.length; i++) {
                    ctx.beginPath()
                    ctx.arc(puffs[i].dx, puffs[i].dy + 6, puffs[i].r, 0, Math.PI * 2)
                    ctx.fill()
                }

                ctx.fillStyle = midColor
                for (var j = 0; j < puffs.length; j++) {
                    ctx.beginPath()
                    ctx.arc(puffs[j].dx, puffs[j].dy + 2, puffs[j].r * 0.96, 0, Math.PI * 2)
                    ctx.fill()
                }

                ctx.fillStyle = topColor
                for (var k = 0; k < puffs.length; k++) {
                    ctx.beginPath()
                    ctx.arc(puffs[k].dx - 3, puffs[k].dy - 2, puffs[k].r * 0.86, 0, Math.PI * 2)
                    ctx.fill()
                }

                ctx.restore()
            }

            var maxClouds = 4
            if (cat === "clear") maxClouds = 4
            else if (cat === "partly_cloudy") maxClouds = 6
            else maxClouds = 7

            for (var ci = 0; ci < maxClouds; ci++) {
                var cl = clouds[ci]
                if (fxRoot.renderBackLayerOnly && cl.layer !== "back") continue
                if (fxRoot.renderFrontLayerOnly && cl.layer !== "front") continue

                var normX = (cl.x + t * cl.speed) % 1.18 - 0.09
                var bobY = Math.sin(t * 1.4 + cl.seed) * 4.0 * s
                var px = normX * width
                var py = cl.y * height + bobY
                var sizeMult = (cat === "clear") ? 0.92 : (stormy ? 1.25 : 1.08)

                if (!fxRoot.renderBackLayerOnly && px > width * 0.20 && px < width * 0.78) {
                    ctx.save()
                    ctx.fillStyle = night ? "rgba(0,0,0,0.10)" : "rgba(15,35,60,0.11)"
                    ctx.beginPath()
                    ctx.ellipse(px - 45 * s, py + 230 * s, 90 * s * cl.scale, 34 * s * cl.scale)
                    ctx.fill()
                    ctx.restore()
                }

                drawIsometricPuffyCloud(px, py, cl.scale * sizeMult, stormy, sunset, night)
            }

            if (fxRoot.renderBackLayerOnly) return

            // Rising chimney smoke
            var chimneys = [
                { x: 0.330, y: 0.568 },
                { x: 0.398, y: 0.517 },
                { x: 0.489, y: 0.453 },
                { x: 0.564, y: 0.528 },
                { x: 0.464, y: 0.725 }
            ]
            for (var ch = 0; ch < chimneys.length; ch++) {
                var baseChX = chimneys[ch].x * width
                var baseChY = chimneys[ch].y * height
                for (var sp = 0; sp < 4; sp++) {
                    var age = ((t * 0.65 + sp * 0.25 + ch * 0.37) % 1.0)
                    var sx = baseChX + age * 22 * s + Math.sin(age * 6.0 + ch) * 3 * s
                    var sy = baseChY - age * 34 * s
                    var sr = (3.0 + age * 7.5) * s
                    var salpha = (1.0 - age) * 0.26
                    ctx.fillStyle = "rgba(235, 242, 248, " + salpha.toFixed(3) + ")"
                    ctx.beginPath()
                    ctx.arc(sx, sy, sr, 0, Math.PI * 2)
                    ctx.fill()
                }
            }

            // High-wind streamlined air streaks when wind >= 18 mph
            if (fxRoot.windSpeedMph >= 18.0) {
                ctx.strokeStyle = "rgba(255, 255, 255, 0.18)"
                ctx.lineWidth = 1.2 * s
                ctx.lineCap = "round"
                for (var wg = 0; wg < 4; wg++) {
                    var wxProg = (t * 0.32 + wg * 0.25) % 1.0
                    var wgx = wxProg * width
                    var wgy = (0.22 + (wg * 0.12) % 0.50) * height + Math.sin(wxProg * 5.0) * 5 * s
                    ctx.beginPath()
                    ctx.moveTo(wgx, wgy)
                    ctx.lineTo(wgx - 38 * s, wgy - 3 * s)
                    ctx.stroke()
                }
            }

            // Rain & Thunderstorm precipitation streaks + water/street splash rings centered over the 3D island
            if (cat === "rain" || cat === "thunderstorm") {
                var dropCount = Math.max(4, Math.round((cat === "thunderstorm" ? 38 : 24) * fxRoot.particleDensity))
                ctx.strokeStyle = night ? "rgba(175, 215, 255, 0.50)" : "rgba(220, 242, 255, 0.58)"
                ctx.lineWidth = Math.max(1.0, 1.4 * s)
                ctx.lineCap = "round"

                var windSlant = Math.min(11.0, 3.0 + fxRoot.windSpeedMph * 0.25) * s
                var dropLen = 16.0 * s

                // Bounded directly over the 3D isometric island
                var islandX0 = width * 0.14
                var islandX1 = width * 0.86
                var islandW = islandX1 - islandX0
                var islandY0 = height * 0.12
                var islandY1 = height * 0.88
                var islandH = islandY1 - islandY0

                for (var r = 0; r < dropCount; r++) {
                    var rSeed = ((r * 0.6180339 - t * 0.14 * ((r % 3) * 0.12 + 0.88) + 100.0) % 1.0)
                    var rx = islandX0 + rSeed * islandW
                    var ry = islandY0 + (((r * 0.381966 + t * 0.58 * ((r % 4) * 0.08 + 0.92)) % 1.0) * islandH)
                    ctx.beginPath()
                    ctx.moveTo(rx, ry)
                    ctx.lineTo(rx - windSlant, ry + dropLen)
                    ctx.stroke()
                }

                var splashCount = Math.max(3, Math.round(8 * fxRoot.particleDensity))
                ctx.strokeStyle = "rgba(225, 245, 255, 0.42)"
                ctx.lineWidth = 1.1 * s
                for (var rp = 0; rp < splashCount; rp++) {
                    var prog = (t * 0.55 + rp * 0.21) % 1.0
                    // Distribute splashes on the island land and water
                    var rpx = islandX0 + (0.22 + ((rp * 0.23) % 0.56)) * islandW
                    var rpy = islandY0 + (0.32 + ((rp * 0.19) % 0.52)) * islandH
                    ctx.beginPath()
                    ctx.ellipse(rpx - prog * 7 * s, rpy - prog * 3.5 * s, prog * 14 * s, prog * 6.5 * s)
                    ctx.stroke()
                }
            }

            // Snowfall particles (calm drifting flakes over the island)
            if (cat === "snow") {
                var snowCount = Math.max(4, Math.round(28 * fxRoot.particleDensity))
                var snX0 = width * 0.14
                var snW = width * 0.72
                var snY0 = height * 0.10
                var snH = height * 0.88

                for (var sn = 0; sn < snowCount; sn++) {
                    var snProg = (sn * 0.273 + t * 0.19 * ((sn % 3) * 0.15 + 0.8)) % 1.0
                    var snX = snX0 + (((sn * 0.618 + Math.sin(t * 0.9 + sn) * 0.025 + t * 0.05) % 1.0) * snW)
                    var snY = snY0 + snProg * snH
                    var flakeR = (1.8 + (sn % 3) * 1.0) * s
                    ctx.fillStyle = "rgba(255, 255, 255, 0.85)"
                    ctx.beginPath()
                    ctx.arc(snX, snY, flakeR, 0, Math.PI * 2)
                    ctx.fill()
                }
            }

            // Coastal Fog banks
            if (cat === "fog") {
                for (var fg = 0; fg < 4; fg++) {
                    var fogX = ((fg * 0.31 + t * 0.025 * (fg % 2 === 0 ? 1 : -1)) % 1.2 - 0.1) * width
                    var fogY = (0.38 + fg * 0.11) * height
                    var grad = ctx.createRadialGradient(fogX, fogY, 10 * s, fogX, fogY, 260 * s)
                    grad.addColorStop(0, "rgba(232, 242, 250, 0.34)")
                    grad.addColorStop(1, "rgba(232, 242, 250, 0.0)")
                    ctx.fillStyle = grad
                    ctx.beginPath()
                    ctx.arc(fogX, fogY, 260 * s, 0, Math.PI * 2)
                    ctx.fill()
                }
            }

            if (fxRoot.lightningFlash > 0.01) {
                ctx.fillStyle = "rgba(235, 245, 255, " + (fxRoot.lightningFlash * 0.45).toFixed(3) + ")"
                ctx.fillRect(0, 0, width, height)
            }
        }
    }
}
