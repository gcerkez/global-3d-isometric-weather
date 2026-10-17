/**
 * YorktownHybridScene.qml
 * -----------------------
 * Renders the high-detail 3D isometric diorama layer with real-time animated
 * Canvas overlays (water wave sparkles, snow frost accumulation, night/sunset
 * window and streetlamp emissive illumination, and interactive landmark pins).
 *
 * Supports:
 *   1. Built-in Historic Yorktown, VA transparent multi-time-of-day cutouts
 *   2. On-demand AI-generated isometric diorama artwork for any global location
 *   3. Dynamic Wikipedia GeoSearch landmark pins for any selected coordinates
 */
import QtQuick

Item {
    id: hybridScene

    property string weatherCategory: "clear"
    property string timeState: "day" // day, sunset, night
    property bool showLandmarkLabels: true
    property real animPhase: 0.0
    property string aiDioramaUrl: ""
    property bool useAiDiorama: false
    property var dynamicLandmarks: []

    readonly property var defaultYorktownLandmarks: [
        {
            title: "George P. Coleman Memorial Bridge",
            desc: "Double-swing steel truss bridge spanning the York River between Yorktown & Gloucester Point.",
            nx: 0.365, ny: 0.365
        },
        {
            title: "18th-Century Tall Ship (York River)",
            desc: "Historic three-masted schooner sailing the 1781 Battle of the Chesapeake & Yorktown waters.",
            nx: 0.228, ny: 0.475
        },
        {
            title: "Yorktown Victory Monument",
            desc: "Ornate column atop the Yorktown bluff commemorating the 1781 Revolutionary War victory.",
            nx: 0.695, ny: 0.235
        },
        {
            title: "1781 Siege Redoubts & Artillery",
            desc: "Preserved Revolutionary War earthen fortifications & bronze siege cannons overlooking the river.",
            nx: 0.775, ny: 0.485
        },
        {
            title: "Historic Main Street & Custom House",
            desc: "18th-century Colonial brick buildings including the Custom House (1721), Nelson House & Swan Tavern.",
            nx: 0.495, ny: 0.635
        }
    ]

    Timer {
        interval: 50
        running: hybridScene.visible
        repeat: true
        onTriggered: {
            hybridScene.animPhase += 0.05
            waterAndLightsCanvas.requestPaint()
        }
    }

    onTimeStateChanged: waterAndLightsCanvas.requestPaint()
    onWeatherCategoryChanged: waterAndLightsCanvas.requestPaint()

    // Aspect-ratio preserving container matching the 1376x768 diorama coordinates
    Item {
        id: islandViewport
        width: Math.min(parent.width, parent.height * (1376.0 / 768.0))
        height: width * (768.0 / 1376.0)
        anchors.centerIn: parent
        anchors.horizontalCenterOffset: -Math.round(parent.width * 0.042)

        // Main transparent cutout of Historic Yorktown 3D Isometric Diorama (or AI Generative location diorama)
        Image {
            id: dioramaImage
            anchors.fill: parent
            source: (hybridScene.useAiDiorama && hybridScene.aiDioramaUrl.length > 0)
                ? hybridScene.aiDioramaUrl
                : (hybridScene.timeState === "night"
                    ? "../images/yorktown_island_night.png"
                    : (hybridScene.timeState === "sunset"
                        ? "../images/yorktown_island_sunset.png"
                        : "../images/yorktown_island.png"))
            fillMode: Image.PreserveAspectFit
            smooth: true
            mipmap: true
        }

        // Dynamic Water Shimmer, Snow Frost, Atmospheric Tint & Night/Evening Emissive Illumination Layer
        Canvas {
            id: waterAndLightsCanvas
            anchors.fill: parent
            antialiasing: true

            // Precise normalized coordinates (0..1 on 1376x768) for Yorktown landmarks & windows
            property var colonialWindows: [
                // Front-left brick building (Custom House)
                { x: 0.348, y: 0.655 }, { x: 0.360, y: 0.646 }, { x: 0.372, y: 0.638 },
                { x: 0.348, y: 0.680 }, { x: 0.372, y: 0.663 },
                // Second brick building along shoreline (Nelson House)
                { x: 0.422, y: 0.596 }, { x: 0.435, y: 0.588 }, { x: 0.448, y: 0.579 },
                { x: 0.422, y: 0.621 }, { x: 0.448, y: 0.604 },
                // Bridge-end brick tavern (Swan Tavern / Main St)
                { x: 0.495, y: 0.531 }, { x: 0.508, y: 0.522 }, { x: 0.520, y: 0.514 },
                // Upper row brick buildings
                { x: 0.548, y: 0.556 }, { x: 0.562, y: 0.566 }, { x: 0.575, y: 0.575 },
                { x: 0.601, y: 0.592 }, { x: 0.615, y: 0.602 }, { x: 0.628, y: 0.611 },
                { x: 0.662, y: 0.665 }, { x: 0.677, y: 0.675 },
                // Lower-center colonial buildings
                { x: 0.498, y: 0.802 }, { x: 0.512, y: 0.812 }, { x: 0.525, y: 0.822 },
                { x: 0.535, y: 0.795 }, { x: 0.547, y: 0.785 },
                { x: 0.611, y: 0.708 }, { x: 0.585, y: 0.725 }
            ]

            property var streetLamps: [
                { x: 0.347, y: 0.722 },
                { x: 0.413, y: 0.761 },
                { x: 0.478, y: 0.616 },
                { x: 0.483, y: 0.542 },
                { x: 0.566, y: 0.606 },
                { x: 0.635, y: 0.662 }
            ]

            onPaint: {
                var ctx = getContext("2d")
                ctx.clearRect(0, 0, width, height)
                if (width <= 10 || height <= 10) return

                var s = width / 1376.0
                var p = hybridScene.animPhase
                var tState = hybridScene.timeState
                var cat = hybridScene.weatherCategory

                // 1. Animated York River water wave sparkles / ripples
                var waterPoints = [
                    { x: 0.14, y: 0.47 }, { x: 0.18, y: 0.52 }, { x: 0.24, y: 0.56 },
                    { x: 0.28, y: 0.46 }, { x: 0.33, y: 0.42 }, { x: 0.38, y: 0.34 },
                    { x: 0.44, y: 0.28 }, { x: 0.49, y: 0.32 }, { x: 0.21, y: 0.39 }
                ]
                for (var wi = 0; wi < waterPoints.length; wi++) {
                    var wp = waterPoints[wi]
                    var waveAlpha = 0.18 + 0.18 * Math.sin(p * 1.8 + wi * 1.3)
                    var wx = (wp.x + Math.sin(p * 0.7 + wi) * 0.006) * width
                    var wy = wp.y * height
                    ctx.strokeStyle = tState === "sunset"
                        ? "rgba(255, 210, 145, " + waveAlpha.toFixed(3) + ")"
                        : (tState === "night"
                            ? "rgba(150, 205, 255, " + (waveAlpha * 0.7).toFixed(3) + ")"
                            : "rgba(235, 250, 255, " + waveAlpha.toFixed(3) + ")")
                    ctx.lineWidth = 1.6 * s
                    ctx.lineCap = "round"
                    ctx.beginPath()
                    ctx.moveTo(wx - 10 * s, wy - 4 * s)
                    ctx.lineTo(wx + 10 * s, wy + 4 * s)
                    ctx.stroke()
                }

                // 2. Subtle snow frost highlights on roofs and hilltop when snowing
                if (cat === "snow") {
                    var snowZones = [
                        { x: 0.695, y: 0.345, r: 60 },
                        { x: 0.770, y: 0.480, r: 85 },
                        { x: 0.515, y: 0.490, r: 45 },
                        { x: 0.435, y: 0.555, r: 42 },
                        { x: 0.360, y: 0.610, r: 42 }
                    ]
                    for (var sz = 0; sz < snowZones.length; sz++) {
                        var z = snowZones[sz]
                        var sGrad = ctx.createRadialGradient(z.x * width, z.y * height, 4 * s, z.x * width, z.y * height, z.r * s)
                        sGrad.addColorStop(0, "rgba(245, 250, 255, 0.28)")
                        sGrad.addColorStop(1, "rgba(245, 250, 255, 0.0)")
                        ctx.fillStyle = sGrad
                        ctx.beginPath()
                        ctx.arc(z.x * width, z.y * height, z.r * s, 0, Math.PI * 2)
                        ctx.fill()
                    }
                }

                // 3. Evening / Night / Thunderstorm Illumination (Colonial Windows, Streetlamps, Coleman Bridge, Tall Ship, Victory Monument)
                var lightsActive = (tState === "night" || tState === "sunset" || cat === "thunderstorm")
                if (lightsActive) {
                    var intensity = (tState === "night") ? 1.0 : (tState === "sunset" ? 0.68 : 0.55)

                    // Victory Monument architectural uplighting on the hill (x=0.695, y=0.22..0.34)
                    var monX = 0.695 * width
                    var monBaseY = 0.335 * height
                    var monTopY = 0.115 * height
                    var monGrad = ctx.createRadialGradient(monX, monBaseY - 40 * s, 4 * s, monX, monBaseY - 50 * s, 110 * s)
                    monGrad.addColorStop(0, "rgba(255, 244, 214, " + (0.48 * intensity).toFixed(3) + ")")
                    monGrad.addColorStop(0.5, "rgba(255, 230, 175, " + (0.20 * intensity).toFixed(3) + ")")
                    monGrad.addColorStop(1, "rgba(255, 220, 150, 0.0)")
                    ctx.fillStyle = monGrad
                    ctx.beginPath()
                    ctx.arc(monX, monBaseY - 45 * s, 110 * s, 0, Math.PI * 2)
                    ctx.fill()

                    // Beacon at top of Victory Monument
                    var beaconAlpha = (0.55 + 0.35 * Math.sin(p * 2.5)) * intensity
                    ctx.fillStyle = "rgba(255, 240, 180, " + beaconAlpha.toFixed(3) + ")"
                    ctx.beginPath()
                    ctx.arc(monX, monTopY - 8 * s, 5 * s, 0, Math.PI * 2)
                    ctx.fill()

                    // Warm glowing Colonial brick windows
                    for (var wIdx = 0; wIdx < colonialWindows.length; wIdx++) {
                        var cw = colonialWindows[wIdx]
                        var wxPos = cw.x * width
                        var wyPos = cw.y * height
                        var flicker = 0.88 + 0.12 * Math.sin(p * 1.7 + wIdx * 2.1)

                        var wGlow = ctx.createRadialGradient(wxPos, wyPos, 1 * s, wxPos, wyPos, 14 * s)
                        wGlow.addColorStop(0, "rgba(255, 220, 120, " + (0.85 * intensity * flicker).toFixed(3) + ")")
                        wGlow.addColorStop(0.4, "rgba(255, 180, 70, " + (0.38 * intensity * flicker).toFixed(3) + ")")
                        wGlow.addColorStop(1, "rgba(255, 160, 50, 0.0)")
                        ctx.fillStyle = wGlow
                        ctx.beginPath()
                        ctx.arc(wxPos, wyPos, 14 * s, 0, Math.PI * 2)
                        ctx.fill()

                        ctx.fillStyle = "rgba(255, 242, 185, " + (0.92 * intensity * flicker).toFixed(3) + ")"
                        ctx.fillRect(wxPos - 2.2 * s, wyPos - 3.0 * s, 4.4 * s, 6.0 * s)
                    }

                    // Cobblestone streetlamps along Main Street
                    for (var sl = 0; sl < streetLamps.length; sl++) {
                        var lamp = streetLamps[sl]
                        var lx = lamp.x * width
                        var ly = lamp.y * height
                        var lGlow = ctx.createRadialGradient(lx, ly, 1 * s, lx, ly, 26 * s)
                        lGlow.addColorStop(0, "rgba(255, 230, 150, " + (0.90 * intensity).toFixed(3) + ")")
                        lGlow.addColorStop(0.35, "rgba(255, 190, 90, " + (0.35 * intensity).toFixed(3) + ")")
                        lGlow.addColorStop(1, "rgba(255, 170, 60, 0.0)")
                        ctx.fillStyle = lGlow
                        ctx.beginPath()
                        ctx.arc(lx, ly, 26 * s, 0, Math.PI * 2)
                        ctx.fill()
                    }

                    // Coleman Memorial Bridge deck lights & marine navigation lights
                    for (var bl = 0; bl <= 8; bl++) {
                        var frac = bl / 8.0
                        var bx = (0.262 + frac * (0.485 - 0.262)) * width
                        var by = (0.312 + frac * (0.468 - 0.312)) * height
                        var bGlow = ctx.createRadialGradient(bx, by, 1 * s, bx, by, 12 * s)
                        bGlow.addColorStop(0, "rgba(255, 235, 180, " + (0.78 * intensity).toFixed(3) + ")")
                        bGlow.addColorStop(1, "rgba(255, 210, 120, 0.0)")
                        ctx.fillStyle = bGlow
                        ctx.beginPath()
                        ctx.arc(bx, by, 12 * s, 0, Math.PI * 2)
                        ctx.fill()
                    }

                    // Center swing-pier marine channel marker light (green/red)
                    ctx.fillStyle = "rgba(80, 255, 150, " + (0.85 * intensity).toFixed(3) + ")"
                    ctx.beginPath()
                    ctx.arc(0.335 * width, 0.415 * height, 3.2 * s, 0, Math.PI * 2)
                    ctx.fill()

                    // 18th-Century Tall Ship warm stern & deck lanterns
                    var shipLights = [
                        { x: 0.205, y: 0.530 },
                        { x: 0.232, y: 0.512 },
                        { x: 0.254, y: 0.494 }
                    ]
                    for (var sh = 0; sh < shipLights.length; sh++) {
                        var sx = shipLights[sh].x * width
                        var sy = shipLights[sh].y * height
                        var sGlow = ctx.createRadialGradient(sx, sy, 1 * s, sx, sy, 16 * s)
                        sGlow.addColorStop(0, "rgba(255, 215, 110, " + (0.88 * intensity).toFixed(3) + ")")
                        sGlow.addColorStop(1, "rgba(255, 180, 60, 0.0)")
                        ctx.fillStyle = sGlow
                        ctx.beginPath()
                        ctx.arc(sx, sy, 16 * s, 0, Math.PI * 2)
                        ctx.fill()
                    }
                }
            }
        }

        // Interactive Landmark Hotspots (uses dynamic Wikipedia GeoSearch landmarks or Historic Yorktown defaults)
        Repeater {
            model: hybridScene.showLandmarkLabels
                ? ((hybridScene.dynamicLandmarks && hybridScene.dynamicLandmarks.length > 0)
                    ? hybridScene.dynamicLandmarks
                    : hybridScene.defaultYorktownLandmarks)
                : []

            delegate: Item {
                x: modelData.nx * islandViewport.width - width / 2
                y: modelData.ny * islandViewport.height - height / 2
                width: 34
                height: 34
                z: spotMouse.containsMouse ? 50 : 10

                // Subtle interactive pulsing ring when mouse hovers over the landmark
                Rectangle {
                    anchors.centerIn: parent
                    width: spotMouse.containsMouse ? 18 : 10
                    height: width
                    radius: width / 2
                    color: spotMouse.containsMouse ? "#f9b826" : Qt.rgba(1, 1, 1, 0.28)
                    border.color: "#ffffff"
                    border.width: spotMouse.containsMouse ? 2 : 1
                    opacity: spotMouse.containsMouse ? 1.0 : 0.45

                    Behavior on width { NumberAnimation { duration: 150 } }
                    Behavior on opacity { NumberAnimation { duration: 150 } }
                }

                // Tooltip popup card
                Rectangle {
                    visible: spotMouse.containsMouse
                    width: 230
                    height: tipCol.implicitHeight + 14
                    x: (modelData.nx > 0.65) ? -width + 12 : -width / 2 + 17
                    y: -height - 8
                    radius: 8
                    color: "#e81d2c3b"
                    border.color: "#7fb8e6"
                    border.width: 1

                    Column {
                        id: tipCol
                        anchors.fill: parent
                        anchors.margins: 7
                        spacing: 3

                        Text {
                            text: modelData.title
                            color: "#ffd166"
                            font.pixelSize: 11
                            font.weight: Font.Bold
                            width: parent.width
                            wrapMode: Text.WordWrap
                        }
                        Text {
                            text: modelData.desc
                            color: "#e6f1fa"
                            font.pixelSize: 10
                            width: parent.width
                            wrapMode: Text.WordWrap
                        }
                    }
                }

                MouseArea {
                    id: spotMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                }
            }
        }
    }
}
