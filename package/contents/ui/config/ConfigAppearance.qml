import QtQuick
import QtQuick.Controls as QQC2
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import org.kde.kcmutils as KCM

KCM.SimpleKCM {
    id: root

    property string cfg_renderMode
    property string cfg_timeOfDayMode
    property string cfg_weatherOverride
    property string cfg_hudPosition
    property double cfg_hudOpacity
    property bool cfg_transparentBackground
    property bool cfg_showLandmarkLabels
    property bool cfg_showLocationBadge
    property double cfg_particleDensity

    Kirigami.FormLayout {
        anchors.fill: parent

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("3D Isometric Rendering Engine")
        }

        QQC2.ComboBox {
            Kirigami.FormData.label: i18n("Render Mode:")
            model: [
                { text: i18n("Hybrid: High-Detail Yorktown Isometric Art + Real-Time 3D Weather FX (Recommended)"), value: "hybrid" },
                { text: i18n("Interactive Procedural 3D Diorama (Rotatable/Zoomable + Live OpenStreetMap 3D Buildings)"), value: "procedural3d" },
                { text: i18n("AI Generative Diorama (Auto-synthesizes 3D Isometric Art for Any Selected City)"), value: "ai_generative" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_renderMode))
            onActivated: root.cfg_renderMode = currentValue
        }

        QQC2.CheckBox {
            Kirigami.FormData.label: i18n("Desktop Backdrop:")
            text: i18n("Transparent Desktop Cutout Mode (float 3D island directly over desktop wallpaper)")
            checked: root.cfg_transparentBackground
            onToggled: root.cfg_transparentBackground = checked
        }

        QQC2.CheckBox {
            Kirigami.FormData.label: i18n("Interactive Landmarks:")
            text: i18n("Show historic Yorktown landmark tooltips on hover (Coleman Bridge, Victory Monument, Redoubts, etc.)")
            checked: root.cfg_showLandmarkLabels
            onToggled: root.cfg_showLandmarkLabels = checked
        }

        QQC2.CheckBox {
            Kirigami.FormData.label: i18n("Location Badge:")
            text: i18n("Show location & render mode badge in bottom-right corner")
            checked: root.cfg_showLocationBadge
            onToggled: root.cfg_showLocationBadge = checked
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Atmosphere, Time of Day & Weather Simulation")
        }

        QQC2.ComboBox {
            Kirigami.FormData.label: i18n("Lighting / Time of Day:")
            model: [
                { text: i18n("Real-Time Solar Cycle (Matches location's live day/sunset/night)"), value: "realtime" },
                { text: i18n("Always Sunny Daylight (Reference Blue Sky)"), value: "day" },
                { text: i18n("Golden Hour / Colonial Sunset"), value: "sunset" },
                { text: i18n("Starry Night (Illuminated Colonial Windows & Monument)"), value: "night" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_timeOfDayMode))
            onActivated: root.cfg_timeOfDayMode = currentValue
        }

        QQC2.ComboBox {
            Kirigami.FormData.label: i18n("Weather Condition:")
            model: [
                { text: i18n("Live Real-Time Open-Meteo Weather"), value: "realtime" },
                { text: i18n("Preview: Clear Weather (Sunny & Crisp)"), value: "clear" },
                { text: i18n("Preview: Partly Cloudy (Floating 3D Isometric Clouds)"), value: "partly_cloudy" },
                { text: i18n("Preview: Overcast Sky"), value: "overcast" },
                { text: i18n("Preview: Rain Shower (Rain Streaks & Water Ripples)"), value: "rain" },
                { text: i18n("Preview: Thunderstorm (Heavy Rain & Lightning Flashes)"), value: "thunderstorm" },
                { text: i18n("Preview: Snowfall (Drifting Snowflakes & Chimney Smoke)"), value: "snow" },
                { text: i18n("Preview: York River Coastal Fog"), value: "fog" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_weatherOverride))
            onActivated: root.cfg_weatherOverride = currentValue
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Weather FX Intensity:")
            spacing: 10
            QQC2.Slider {
                from: 0.25
                to: 2.0
                stepSize: 0.25
                value: root.cfg_particleDensity
                onMoved: root.cfg_particleDensity = value
                Layout.preferredWidth: 180
            }
            QQC2.Label {
                text: Math.round(root.cfg_particleDensity * 100) + "%"
            }
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Weather Overlay Card (HUD)")
        }

        QQC2.ComboBox {
            Kirigami.FormData.label: i18n("Overlay Position:")
            model: [
                { text: i18n("Top Right (Reference Default)"), value: "top-right" },
                { text: i18n("Top Left"), value: "top-left" },
                { text: i18n("Bottom Right"), value: "bottom-right" },
                { text: i18n("Bottom Left"), value: "bottom-left" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_hudPosition))
            onActivated: root.cfg_hudPosition = currentValue
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Card Opacity:")
            spacing: 10
            QQC2.Slider {
                from: 0.40
                to: 0.98
                stepSize: 0.02
                value: root.cfg_hudOpacity
                onMoved: root.cfg_hudOpacity = value
                Layout.preferredWidth: 180
            }
            QQC2.Label {
                text: Math.round(root.cfg_hudOpacity * 100) + "%"
            }
        }
    }
}
