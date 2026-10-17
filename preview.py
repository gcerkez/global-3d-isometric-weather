#!/usr/bin/env python3
"""
Standalone launcher and screenshot verification utility for the
Yorktown 3D Isometric Weather KDE Plasma 6 Widget.

Usage:
  ./preview.py                  # Launch interactive KDE Plasma widget window (plasmawindowed)
  ./preview.py --screenshots    # Render verification screenshots of Hybrid & 3D modes
"""
import os
import sys
import subprocess
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent
PLUGIN_ID = "com.antigravity.yorktownweather"


def capture_verification_renders(out_dir: Path):
    from PySide6.QtCore import QUrl, QTimer, Qt
    from PySide6.QtGui import QGuiApplication
    from PySide6.QtQuick import QQuickView

    out_dir.mkdir(parents=True, exist_ok=True)
    app = QGuiApplication(sys.argv)

    # Create a standalone QML harness that loads our exact UI components
    harness_qml = out_dir / "render_harness.qml"
    ui_dir = QUrl.fromLocalFile(str(ROOT_DIR / "package" / "contents" / "ui")).toString()
    harness_qml.write_text(f"""
import QtQuick
import "{ui_dir}"

Item {{
    id: root
    width: 960
    height: 536

    property string renderMode: "hybrid"
    property string timeState: "day"
    property string weatherCategory: "clear"
    property string conditionText: "CLEAR WEATHER"
    property string temperatureText: "68°F (20°C)"
    property int humidity: 64
    property string windText: "8 MPH NNE WIND"
    property bool hudExpanded: false

    Rectangle {{
        id: skyCard
        anchors.fill: parent
        radius: 14
        border.color: root.timeState === "night" ? "#3b526b" : "#bce0f7"
        border.width: 1.5
        clip: true

        gradient: Gradient {{
            GradientStop {{
                position: 0.0
                color: root.timeState === "night" ? "#0d1b2a"
                     : (root.timeState === "sunset" ? "#3a506b"
                     : (root.weatherCategory === "rain" || root.weatherCategory === "thunderstorm" ? "#4a6278" : "#7fc4f2"))
            }}
            GradientStop {{
                position: 1.0
                color: root.timeState === "night" ? "#1b314c"
                     : (root.timeState === "sunset" ? "#f29a6b"
                     : (root.weatherCategory === "rain" || root.weatherCategory === "thunderstorm" ? "#7b92a8" : "#a8dbf8"))
            }}
        }}
    }}

    WeatherEffectsOverlay {{
        anchors.fill: parent
        weatherCategory: root.weatherCategory
        timeState: root.timeState
        windSpeedMph: 9.0
        particleDensity: 1.0
        renderBackLayerOnly: true
    }}

    YorktownHybridScene {{
        anchors.fill: parent
        visible: root.renderMode === "hybrid"
        weatherCategory: root.weatherCategory
        timeState: root.timeState
        showLandmarkLabels: false
    }}

    YorktownProcedural3D {{
        anchors.fill: parent
        visible: root.renderMode === "procedural3d"
        weatherCategory: root.weatherCategory
        timeState: root.timeState
        locationName: "Yorktown, VA"
        windSpeedMph: 9.0
    }}

    WeatherEffectsOverlay {{
        anchors.fill: parent
        weatherCategory: root.weatherCategory
        timeState: root.timeState
        windSpeedMph: 9.0
        particleDensity: 1.0
        renderFrontLayerOnly: true
    }}

    WeatherHudCard {{
        scaleFactor: 1.15
        conditionText: root.conditionText
        weatherCategory: root.weatherCategory
        isNight: root.timeState === "night"
        temperatureText: root.temperatureText
        feelsLikeText: "67°F"
        humidity: root.humidity
        windText: root.windText
        locationName: "Yorktown, VA"
        expanded: root.hudExpanded
        currentRenderMode: root.renderMode
        currentTimeMode: root.timeState
        currentWeatherOverride: root.weatherCategory
        forecastDays: [
            {{ dayName: "TODAY", tempRange: "70° / 56°F" }},
            {{ dayName: "SUN", tempRange: "72° / 58°F" }},
            {{ dayName: "MON", tempRange: "66° / 53°F" }}
        ]
        anchors.top: parent.top
        anchors.right: parent.right
        anchors.margins: 20
    }}

    Rectangle {{
        anchors.right: parent.right
        anchors.bottom: parent.bottom
        anchors.rightMargin: 18
        anchors.bottomMargin: 14
        height: 26
        width: badgeText.implicitWidth + 22
        radius: 6
        color: "#b822303e"
        border.color: Qt.rgba(1, 1, 1, 0.22)
        border.width: 1

        Text {{
            id: badgeText
            anchors.centerIn: parent
            text: "Yorktown, VA • " + (root.renderMode === "hybrid" ? "3D Isometric" : "Interactive 3D")
            color: "#eef6fc"
            font.pixelSize: 12
            font.weight: Font.Medium
            font.letterSpacing: 0.4
        }}
    }}
}}
""")

    view = QQuickView()
    view.setColor(Qt.transparent)
    view.setSource(QUrl.fromLocalFile(str(harness_qml)))
    view.show()

    states = [
        ("yorktown_widget_sunny_day.png", "hybrid", "day", "clear", "CLEAR WEATHER", "68°F (20°C)", 62, "7 MPH NNE WIND", False),
        ("yorktown_widget_night_storm.png", "hybrid", "night", "rain", "LIGHT RAIN", "55°F (13°C)", 88, "14 MPH NE WIND", True),
        ("yorktown_widget_procedural3d.png", "procedural3d", "day", "partly_cloudy", "PARTLY CLOUDY", "68°F (20°C)", 64, "9 MPH NNE WIND", False),
    ]
    state_idx = [0]

    def apply_next():
        idx = state_idx[0]
        if idx >= len(states):
            app.quit()
            return
        fname, rmode, tstate, wcat, cond, temp, hum, wind, exp = states[idx]
        root_obj = view.rootObject()
        root_obj.setProperty("renderMode", rmode)
        root_obj.setProperty("timeState", tstate)
        root_obj.setProperty("weatherCategory", wcat)
        root_obj.setProperty("conditionText", cond)
        root_obj.setProperty("temperatureText", temp)
        root_obj.setProperty("humidity", hum)
        root_obj.setProperty("windText", wind)
        root_obj.setProperty("hudExpanded", exp)
        QTimer.singleShot(350, lambda: save_and_advance(fname))

    def save_and_advance(fname):
        img = view.grabWindow()
        out_path = out_dir / fname
        img.save(str(out_path))
        print(f"Saved {out_path}")
        state_idx[0] += 1
        QTimer.singleShot(100, apply_next)

    QTimer.singleShot(400, apply_next)
    app.exec()
    harness_qml.unlink(missing_ok=True)


if __name__ == "__main__":
    if "--screenshots" in sys.argv:
        out = Path("/home/gcerkez/.gemini/antigravity/brain/9d13020f-0e3a-4e92-8852-af20399e39f1")
        capture_verification_renders(out)
    else:
        print(f"Launching {PLUGIN_ID} via plasmawindowed...")
        os.execvp("plasmawindowed", ["plasmawindowed", PLUGIN_ID])
