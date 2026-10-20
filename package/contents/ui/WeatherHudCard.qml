import QtQuick
import QtQuick.Layouts

/**
 * @file WeatherHudCard.qml
 * @description Translucent slate-blue glassmorphic weather telemetry overlay card.
 *
 * Primary collapsed state replicates the San Francisco reference HUD:
 *   - Left: Animated vector weather icon (sun, moon, cloud, rain, thunderstorm, snow)
 *   - Right: 4-line uppercase telemetry (Condition, Dual/Single Temperature, Humidity, Wind)
 *
 * Expanded drawer state (toggled by clicking the card) provides extended weather telemetry:
 *   - Color-coded US EPA Air Quality Index (AQI) & WHO UV Index badges + Lunar phase
 *   - 12-hour temperature sparkline curve with precipitation probability bars
 *   - 3-day high/low forecast strip
 *   - Quick-action toggles for Render Mode (Hybrid / 3D / AI Generative), Sky, and Weather
 */
Item {
    id: hudRoot

    property string conditionText: "CLEAR WEATHER"
    property string weatherCategory: "clear"
    property bool isNight: false
    property string temperatureText: "68°F (20°C)"
    property string feelsLikeText: "67°F"
    property int humidity: 64
    property string windText: "8 MPH NNE WIND"
    property string locationName: "Yorktown, VA"
    property string lastUpdated: "Live"
    property double cardOpacity: 0.82
    property real scaleFactor: 1.0

    // Extended telemetry properties
    property int aqiValue: 24
    property string aqiLabel: "GOOD"
    property string aqiColor: "#4ade80"
    property real uvIndex: 3.5
    property string uvLabel: "Mod"
    property string uvColor: "#facc15"
    property string moonPhaseName: "Waxing Gibbous"
    property string moonEmoji: "🌔"
    property alias moonPhaseIcon: hudRoot.moonEmoji
    property string sunriseText: "6:58 AM"
    property string sunsetText: "6:24 PM"
    property var hourlyTemps: [66, 68, 70, 71, 69, 65, 61, 58]
    property var hourlyPrecipProb: [5, 10, 15, 20, 15, 10, 5, 5]
    property var forecastDays: []

    // Interactive quick-controls state & signals
    property string currentRenderMode: "hybrid"
    property string currentTimeMode: "realtime"
    property string currentWeatherOverride: "realtime"
    property string forecastUrl: ""
    property string liveClockText: Qt.formatTime(new Date(), "h:mm AP")
    signal toggleRenderModeRequested()
    signal cycleTimeModeRequested()
    signal cycleWeatherOverrideRequested()
    signal refreshRequested()

    property bool expanded: false

    Timer {
        interval: 1000
        running: true
        repeat: true
        onTriggered: hudRoot.liveClockText = Qt.formatTime(new Date(), "h:mm AP")
    }

    width: Math.round(248 * scaleFactor)
    height: expanded ? Math.round(298 * scaleFactor) : Math.round(94 * scaleFactor)

    Behavior on height {
        NumberAnimation { duration: 220; easing.type: Easing.OutCubic }
    }

    property real sunAngle: 0
    NumberAnimation on sunAngle {
        from: 0
        to: 360
        duration: 28000
        loops: Animation.Infinite
        running: visible
    }

    onSunAngleChanged: iconCanvas.requestPaint()
    onWeatherCategoryChanged: iconCanvas.requestPaint()
    onIsNightChanged: iconCanvas.requestPaint()
    onHourlyTempsChanged: sparklineCanvas.requestPaint()
    onHourlyPrecipProbChanged: sparklineCanvas.requestPaint()

    // Soft drop shadow behind the HUD card
    Rectangle {
        anchors.fill: mainCard
        anchors.topMargin: Math.round(4 * hudRoot.scaleFactor)
        anchors.leftMargin: Math.round(2 * hudRoot.scaleFactor)
        radius: mainCard.radius
        color: "#35081422"
    }

    Rectangle {
        id: mainCard
        anchors.fill: parent
        radius: Math.round(12 * hudRoot.scaleFactor)
        color: Qt.rgba(0.27, 0.39, 0.48, hudRoot.cardOpacity)
        border.color: Qt.rgba(0.78, 0.89, 0.96, 0.35)
        border.width: 1
        clip: true

        // Top gloss highlight
        Rectangle {
            anchors.top: parent.top
            anchors.left: parent.left
            anchors.right: parent.right
            height: Math.round(48 * hudRoot.scaleFactor)
            radius: parent.radius
            gradient: Gradient {
                GradientStop { position: 0.0; color: Qt.rgba(1, 1, 1, 0.10) }
                GradientStop { position: 1.0; color: Qt.rgba(1, 1, 1, 0.0) }
            }
        }

        Column {
            anchors.fill: parent
            anchors.margins: Math.round(10 * hudRoot.scaleFactor)
            spacing: Math.round(6 * hudRoot.scaleFactor)

            // Primary Reference-Style Row: Left Animated Weather Icon + Right 4-line Telemetry
            Row {
                width: parent.width
                height: Math.round(74 * hudRoot.scaleFactor)
                spacing: Math.round(10 * hudRoot.scaleFactor)

                Item {
                    width: Math.round(74 * hudRoot.scaleFactor)
                    height: Math.round(74 * hudRoot.scaleFactor)

                    Canvas {
                        id: iconCanvas
                        anchors.fill: parent
                        antialiasing: true
                        onPaint: {
                            var ctx = getContext("2d")
                            ctx.clearRect(0, 0, width, height)
                            var cx = width * 0.5
                            var cy = height * 0.5
                            var r = width * 0.23
                            var cat = hudRoot.weatherCategory
                            var sScale = width / 84.0

                            function drawSun(sx, sy, sr, rayLen, rayInner) {
                                ctx.save()
                                ctx.translate(sx, sy)
                                ctx.rotate(hudRoot.sunAngle * Math.PI / 180.0)
                                ctx.beginPath()
                                ctx.arc(0, 0, sr, 0, Math.PI * 2)
                                ctx.fillStyle = "#f9b826"
                                ctx.fill()

                                ctx.strokeStyle = "#f9b826"
                                ctx.lineWidth = Math.max(2.5, width * 0.042)
                                ctx.lineCap = "round"
                                for (var i = 0; i < 8; i++) {
                                    var ang = i * Math.PI / 4.0
                                    ctx.beginPath()
                                    ctx.moveTo(Math.cos(ang) * rayInner, Math.sin(ang) * rayInner)
                                    ctx.lineTo(Math.cos(ang) * rayLen, Math.sin(ang) * rayLen)
                                    ctx.stroke()
                                }
                                ctx.restore()
                            }

                            function drawMoon(mx, my, mr) {
                                ctx.save()
                                ctx.translate(mx, my)
                                ctx.beginPath()
                                ctx.arc(0, 0, mr, 0.25 * Math.PI, 1.75 * Math.PI, false)
                                ctx.arc(mr * 0.45, -mr * 0.15, mr * 0.82, 1.65 * Math.PI, 0.35 * Math.PI, true)
                                ctx.closePath()
                                ctx.fillStyle = "#ffe28a"
                                ctx.fill()
                                ctx.restore()
                            }

                            function drawCloud(clx, cly, scale, fillCol) {
                                ctx.save()
                                ctx.translate(clx, cly)
                                ctx.scale(scale, scale)
                                ctx.beginPath()
                                ctx.arc(-14, 4, 11, Math.PI * 0.5, Math.PI * 1.5)
                                ctx.arc(-4, -5, 13, Math.PI * 1.0, Math.PI * 1.85)
                                ctx.arc(11, -2, 11, Math.PI * 1.2, Math.PI * 1.95)
                                ctx.arc(16, 5, 9, Math.PI * 1.5, Math.PI * 0.5)
                                ctx.closePath()
                                ctx.fillStyle = fillCol
                                ctx.fill()
                                ctx.restore()
                            }

                            if (cat === "clear") {
                                if (hudRoot.isNight) drawMoon(cx, cy, r * 1.15)
                                else drawSun(cx, cy, r, width * 0.44, width * 0.31)
                            } else if (cat === "partly_cloudy") {
                                if (hudRoot.isNight) drawMoon(cx - 8 * sScale, cy - 8 * sScale, r * 0.85)
                                else drawSun(cx - 8 * sScale, cy - 8 * sScale, r * 0.72, width * 0.31, width * 0.22)
                                drawCloud(cx + 4 * sScale, cy + 8 * sScale, sScale * 1.15, "#f1f6fa")
                            } else if (cat === "overcast" || cat === "fog") {
                                drawCloud(cx - 4 * sScale, cy - 4 * sScale, sScale * 1.0, "#b8c9d9")
                                drawCloud(cx + 4 * sScale, cy + 5 * sScale, sScale * 1.18, "#eaf2f8")
                                if (cat === "fog") {
                                    ctx.strokeStyle = "#d4e4f2"
                                    ctx.lineWidth = 2.5 * sScale
                                    ctx.lineCap = "round"
                                    for (var f = 0; f < 2; f++) {
                                        var fy = cy + (20 + f * 7) * sScale
                                        ctx.beginPath()
                                        ctx.moveTo(cx - 18 * sScale, fy)
                                        ctx.lineTo(cx + 18 * sScale, fy)
                                        ctx.stroke()
                                    }
                                }
                            } else if (cat === "rain" || cat === "thunderstorm") {
                                drawCloud(cx, cy - 5 * sScale, sScale * 1.22, cat === "thunderstorm" ? "#9ab0c4" : "#dce8f2")
                                ctx.strokeStyle = "#6cc8ff"
                                ctx.lineWidth = 2.4 * sScale
                                ctx.lineCap = "round"
                                var dropOffset = (hudRoot.sunAngle * 0.6) % 8
                                for (var d = -1; d <= 1; d++) {
                                    var dx = cx + d * 11 * sScale
                                    var dy = cy + (12 + (d + 1) * 2 + dropOffset * 0.5) * sScale
                                    ctx.beginPath()
                                    ctx.moveTo(dx, dy)
                                    ctx.lineTo(dx - 4 * sScale, dy + 9 * sScale)
                                    ctx.stroke()
                                }
                                if (cat === "thunderstorm") {
                                    ctx.fillStyle = "#ffd43b"
                                    ctx.beginPath()
                                    ctx.moveTo(cx + 2 * sScale, cy + 6 * sScale)
                                    ctx.lineTo(cx - 5 * sScale, cy + 18 * sScale)
                                    ctx.lineTo(cx + 1 * sScale, cy + 18 * sScale)
                                    ctx.lineTo(cx - 2 * sScale, cy + 29 * sScale)
                                    ctx.lineTo(cx + 8 * sScale, cy + 15 * sScale)
                                    ctx.lineTo(cx + 2 * sScale, cy + 15 * sScale)
                                    ctx.closePath()
                                    ctx.fill()
                                }
                            } else if (cat === "snow") {
                                drawCloud(cx, cy - 5 * sScale, sScale * 1.22, "#eef5fc")
                                ctx.fillStyle = "#ffffff"
                                for (var s = -1; s <= 1; s++) {
                                    ctx.beginPath()
                                    ctx.arc(cx + s * 11 * sScale, cy + 18 * sScale, 3.2 * sScale, 0, Math.PI * 2)
                                    ctx.fill()
                                }
                            }
                        }
                    }
                }

                // 4-Line Telemetry Column matching the San Francisco reference layout
                Column {
                    width: parent.width - Math.round(86 * hudRoot.scaleFactor)
                    anchors.verticalCenter: parent.verticalCenter
                    spacing: Math.round(2 * hudRoot.scaleFactor)

                    Row {
                        width: parent.width
                        spacing: 5

                        Text {
                            text: hudRoot.liveClockText
                            color: "#ffd166"
                            font.pixelSize: Math.max(9, Math.round(11 * hudRoot.scaleFactor))
                            font.weight: Font.Bold
                        }

                        Text {
                            text: "• " + hudRoot.conditionText.toUpperCase()
                            color: "#ffffff"
                            font.pixelSize: Math.max(9, Math.round(11.5 * hudRoot.scaleFactor))
                            font.weight: Font.DemiBold
                            font.letterSpacing: 0.4
                            elide: Text.ElideRight
                            width: Math.max(10, parent.width - 65 * hudRoot.scaleFactor)
                        }
                    }

                    Text {
                        text: hudRoot.temperatureText
                        color: "#ffffff"
                        font.pixelSize: Math.max(10, Math.round(12.5 * hudRoot.scaleFactor))
                        font.weight: Font.Bold
                        font.letterSpacing: 0.4
                        elide: Text.ElideRight
                        width: parent.width
                    }

                    Text {
                        text: hudRoot.humidity + "% HUMIDITY"
                        color: "#eef6fc"
                        font.pixelSize: Math.max(9, Math.round(11.5 * hudRoot.scaleFactor))
                        font.weight: Font.Medium
                        font.letterSpacing: 0.4
                        elide: Text.ElideRight
                        width: parent.width
                    }

                    Text {
                        text: hudRoot.windText.toUpperCase()
                        color: "#eef6fc"
                        font.pixelSize: Math.max(9, Math.round(11.5 * hudRoot.scaleFactor))
                        font.weight: Font.Medium
                        font.letterSpacing: 0.4
                        elide: Text.ElideRight
                        width: parent.width
                    }
                }
            }

            // Expanded Drawer: AQI/UV/Moon Badges + 12h Sparkline + 3-Day Forecast + Quick Mode Controls
            Column {
                visible: hudRoot.expanded
                opacity: hudRoot.expanded ? 1.0 : 0.0
                width: parent.width
                spacing: Math.round(6 * hudRoot.scaleFactor)

                Behavior on opacity {
                    NumberAnimation { duration: 180 }
                }

                Rectangle {
                    width: parent.width
                    height: 1
                    color: Qt.rgba(1, 1, 1, 0.2)
                }

                // Location + Feels Like + Astronomy Row
                Text {
                    text: "📍 " + hudRoot.locationName.toUpperCase() + " • FEELS " + hudRoot.feelsLikeText
                    color: "#d9ecff"
                    font.pixelSize: Math.max(8, Math.round(9.5 * hudRoot.scaleFactor))
                    font.weight: Font.DemiBold
                    width: parent.width
                    elide: Text.ElideRight
                }

                // AQI, UV & Lunar Phase Badges
                Row {
                    width: parent.width
                    spacing: Math.round(5 * hudRoot.scaleFactor)

                    Rectangle {
                        height: Math.round(18 * hudRoot.scaleFactor)
                        width: aqiTxt.implicitWidth + Math.round(10 * hudRoot.scaleFactor)
                        radius: 4
                        color: Qt.rgba(0.08, 0.14, 0.20, 0.65)
                        border.color: hudRoot.aqiColor
                        border.width: 1
                        Text {
                            id: aqiTxt
                            anchors.centerIn: parent
                            text: "AQI " + hudRoot.aqiValue + " " + hudRoot.aqiLabel
                            color: hudRoot.aqiColor
                            font.pixelSize: Math.max(8, Math.round(8.5 * hudRoot.scaleFactor))
                            font.weight: Font.Bold
                        }
                    }

                    Rectangle {
                        height: Math.round(18 * hudRoot.scaleFactor)
                        width: uvTxt.implicitWidth + Math.round(10 * hudRoot.scaleFactor)
                        radius: 4
                        color: Qt.rgba(0.08, 0.14, 0.20, 0.65)
                        border.color: hudRoot.uvColor
                        border.width: 1
                        Text {
                            id: uvTxt
                            anchors.centerIn: parent
                            text: "UV " + hudRoot.uvIndex.toFixed(1)
                            color: hudRoot.uvColor
                            font.pixelSize: Math.max(8, Math.round(8.5 * hudRoot.scaleFactor))
                            font.weight: Font.Bold
                        }
                    }

                    Rectangle {
                        height: Math.round(18 * hudRoot.scaleFactor)
                        width: moonTxt.implicitWidth + Math.round(10 * hudRoot.scaleFactor)
                        radius: 4
                        color: Qt.rgba(0.08, 0.14, 0.20, 0.65)
                        border.color: Qt.rgba(1, 1, 1, 0.22)
                        border.width: 1
                        Text {
                            id: moonTxt
                            anchors.centerIn: parent
                            text: hudRoot.moonEmoji + " " + hudRoot.moonPhaseName
                            color: "#e6f2ff"
                            font.pixelSize: Math.max(8, Math.round(8.5 * hudRoot.scaleFactor))
                            font.weight: Font.Medium
                        }
                    }
                }

                // 12h Hourly Temperature & Precipitation Sparkline
                Rectangle {
                    width: parent.width
                    height: Math.round(44 * hudRoot.scaleFactor)
                    radius: Math.round(5 * hudRoot.scaleFactor)
                    color: Qt.rgba(0.10, 0.17, 0.24, 0.48)
                    border.color: Qt.rgba(1, 1, 1, 0.12)
                    border.width: 1

                    Canvas {
                        id: sparklineCanvas
                        anchors.fill: parent
                        anchors.margins: 4
                        antialiasing: true
                        onPaint: {
                            var ctx = getContext("2d")
                            ctx.clearRect(0, 0, width, height)
                            var temps = hudRoot.hourlyTemps || []
                            var probs = hudRoot.hourlyPrecipProb || []
                            if (temps.length < 2) return

                            var minT = temps[0], maxT = temps[0]
                            for (var i = 1; i < temps.length; i++) {
                                if (temps[i] < minT) minT = temps[i]
                                if (temps[i] > maxT) maxT = temps[i]
                            }
                            var spanT = Math.max(2.0, maxT - minT)

                            // Secondary series: precipitation probability bars at bottom
                            var stepX = width / (temps.length - 1)
                            ctx.fillStyle = "rgba(108, 200, 255, 0.28)"
                            for (var p = 0; p < probs.length; p++) {
                                var barH = (Math.min(100, Math.max(0, probs[p])) / 100.0) * (height * 0.65)
                                var bx = p * stepX - 3
                                ctx.fillRect(bx, height - barH, 6, barH)
                            }

                            // Primary series: smooth temperature curve
                            ctx.strokeStyle = "#f9b826"
                            ctx.lineWidth = 1.8
                            ctx.beginPath()
                            for (var j = 0; j < temps.length; j++) {
                                var tx = j * stepX
                                var ty = height - 6 - ((temps[j] - minT) / spanT) * (height - 14)
                                if (j === 0) ctx.moveTo(tx, ty)
                                else ctx.lineTo(tx, ty)
                            }
                            ctx.stroke()
                        }
                    }

                    Text {
                        anchors.top: parent.top
                        anchors.left: parent.left
                        anchors.margins: 4
                        text: "NEXT 12H TEMP & PRECIP%"
                        color: "#9ec2e0"
                        font.pixelSize: Math.max(7, Math.round(7.5 * hudRoot.scaleFactor))
                        font.weight: Font.Bold
                    }
                }

                // 7-Day Forecast Strip (Scrollable Flickable)
                Flickable {
                    width: parent.width
                    height: Math.round(38 * hudRoot.scaleFactor)
                    contentWidth: forecastRow.width
                    boundsBehavior: Flickable.StopAtBounds
                    clip: true
                    visible: hudRoot.forecastDays && hudRoot.forecastDays.length > 0

                    Row {
                        id: forecastRow
                        spacing: Math.round(5 * hudRoot.scaleFactor)

                        Repeater {
                            model: hudRoot.forecastDays
                            delegate: Rectangle {
                                width: Math.round(62 * hudRoot.scaleFactor)
                                height: Math.round(38 * hudRoot.scaleFactor)
                                radius: Math.round(5 * hudRoot.scaleFactor)
                                color: Qt.rgba(0.12, 0.20, 0.28, 0.55)
                                border.color: (modelData.precipProb >= 10) ? Qt.rgba(0.22, 0.74, 0.97, 0.45) : Qt.rgba(1, 1, 1, 0.14)
                                border.width: 1

                                Column {
                                    anchors.centerIn: parent
                                    spacing: 1
                                    Row {
                                        anchors.horizontalCenter: parent.horizontalCenter
                                        spacing: 2
                                        Text {
                                            text: modelData.emoji ? modelData.emoji : ""
                                            font.pixelSize: Math.max(7, Math.round(8.5 * hudRoot.scaleFactor))
                                        }
                                        Text {
                                            text: modelData.dayName
                                            color: "#bfd8ee"
                                            font.pixelSize: Math.max(7, Math.round(8.5 * hudRoot.scaleFactor))
                                            font.weight: Font.Bold
                                        }
                                    }
                                    Text {
                                        anchors.horizontalCenter: parent.horizontalCenter
                                        text: (modelData.precipProb >= 10) ? (modelData.tempRange + " 💧" + modelData.precipProb + "%") : modelData.tempRange
                                        color: (modelData.precipProb >= 10) ? "#7dd3fc" : "#ffffff"
                                        font.pixelSize: Math.max(7, Math.round(8 * hudRoot.scaleFactor))
                                        font.weight: Font.DemiBold
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Quick-Toggle Bar (Mode / Lighting / Weather Test / Refresh)
                Row {
                    width: parent.width
                    spacing: Math.round(4 * hudRoot.scaleFactor)

                    Repeater {
                        model: [
                            {
                                label: hudRoot.currentRenderMode === "hybrid"
                                    ? "🎨 Hybrid"
                                    : (hudRoot.currentRenderMode === "ai_generative" ? "✨ AI City" : "🧊 3D"),
                                action: "mode"
                            },
                            {
                                label: "☀ " + (hudRoot.currentTimeMode === "realtime" ? "Auto" : hudRoot.currentTimeMode.toUpperCase()),
                                action: "time"
                            },
                            {
                                label: "📅 7-Day",
                                action: "forecast"
                            },
                            {
                                label: "↻",
                                action: "refresh"
                            }
                        ]
                        delegate: Rectangle {
                            height: Math.round(22 * hudRoot.scaleFactor)
                            width: modelData.action === "refresh"
                                ? Math.round(24 * hudRoot.scaleFactor)
                                : (parent.width - Math.round(36 * hudRoot.scaleFactor)) / 3
                            radius: Math.round(4 * hudRoot.scaleFactor)
                            color: pillMouse.containsMouse ? Qt.rgba(1, 1, 1, 0.26) : Qt.rgba(0.10, 0.18, 0.25, 0.55)
                            border.color: Qt.rgba(1, 1, 1, 0.22)
                            border.width: 1

                            Text {
                                anchors.centerIn: parent
                                text: modelData.label
                                color: "#ffffff"
                                font.pixelSize: Math.max(7, Math.round(8.5 * hudRoot.scaleFactor))
                                font.weight: Font.DemiBold
                                elide: Text.ElideRight
                            }

                            MouseArea {
                                id: pillMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (modelData.action === "mode") hudRoot.toggleRenderModeRequested()
                                    else if (modelData.action === "time") hudRoot.cycleTimeModeRequested()
                                    else if (modelData.action === "forecast") hudRoot.expanded = !hudRoot.expanded
                                    else if (modelData.action === "refresh") hudRoot.refreshRequested()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Click top portion of card to expand/collapse forecast & quick controls
        MouseArea {
            anchors.top: parent.top
            anchors.left: parent.left
            anchors.right: parent.right
            height: Math.round(94 * hudRoot.scaleFactor)
            cursorShape: Qt.PointingHandCursor
            onClicked: hudRoot.expanded = !hudRoot.expanded
        }
    }
}
