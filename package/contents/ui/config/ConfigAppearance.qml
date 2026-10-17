import QtQuick
import QtQuick.Controls as QQC2
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import org.kde.kcmutils as KCM
import "../js/LocationGenerator.js" as LocGen

KCM.SimpleKCM {
    id: root

    property string cfg_locationName
    property double cfg_latitude
    property double cfg_longitude
    property string cfg_renderMode
    property string cfg_dioramaTheme
    property string cfg_customDioramaUrl
    property double cfg_dioramaZoom
    property int cfg_dioramaOffsetX
    property string cfg_timeOfDayMode
    property string cfg_weatherOverride
    property string cfg_hudPosition
    property double cfg_hudOpacity
    property bool cfg_transparentBackground
    property bool cfg_showLandmarkLabels
    property bool cfg_showLocationBadge
    property double cfg_particleDensity

    property string generatorStatus: ""
    property bool generatingCutout: false

    /**
     * Synthesizes a custom high-resolution 3D isometric diorama cutout for any city
     * directly inside the Configuration Menu by querying Wikipedia GeoSearch for
     * real landmarks and generating a 1376x768 isometric island URL.
     */
    function generateCustomHighResDiorama(targetCityName) {
        var city = (targetCityName && targetCityName.trim().length > 0)
            ? targetCityName.trim()
            : (root.cfg_locationName || "Yorktown, VA")

        root.generatingCutout = true
        root.generatorStatus = "Discovering landmarks for " + city + " and synthesizing high-res 3D isometric cutout..."

        LocGen.fetchWikipediaLandmarks(root.cfg_latitude || 37.2388, root.cfg_longitude || -76.5097, function(landmarks) {
            var titles = []
            for (var i = 0; i < landmarks.length; i++) {
                titles.push(landmarks[i].title)
            }
            var timeMode = (root.cfg_timeOfDayMode === "realtime") ? "day" : root.cfg_timeOfDayMode
            var wxMode = (root.cfg_weatherOverride === "realtime") ? "clear" : root.cfg_weatherOverride
            var url = LocGen.buildGenerativeDioramaUrl(city, titles, timeMode, wxMode)

            root.cfg_customDioramaUrl = url
            root.cfg_dioramaTheme = "custom_city"
            root.cfg_renderMode = "hybrid"
            dioramaThemeCombo.currentIndex = Math.max(0, dioramaThemeCombo.indexOfValue("custom_city"))
            renderModeCombo.currentIndex = Math.max(0, renderModeCombo.indexOfValue("hybrid"))
            root.generatingCutout = false
            root.generatorStatus = "✓ Generated custom high-res 3D isometric diorama for " + city
                + (titles.length > 0 ? (" (featuring " + titles.slice(0, 3).join(", ") + ")") : "") + ". Click Apply to save."
        })
    }

    Kirigami.FormLayout {
        anchors.fill: parent

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("3D Isometric Rendering Engine & Pre-Generated Dioramas")
        }

        QQC2.ComboBox {
            id: renderModeCombo
            Kirigami.FormData.label: i18n("Render Mode:")
            model: [
                { text: i18n("Hybrid: High-Detail 3D Isometric Cutout + Real-Time Weather FX (Recommended)"), value: "hybrid" },
                { text: i18n("Interactive Procedural 3D Diorama (Zoomable/Centerable + Live OpenStreetMap 3D Buildings)"), value: "procedural3d" },
                { text: i18n("AI Generative Diorama (Auto-synthesizes 3D Isometric Art for Any Selected City)"), value: "ai_generative" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_renderMode))
            onActivated: root.cfg_renderMode = currentValue
        }

        QQC2.ComboBox {
            id: dioramaThemeCombo
            Kirigami.FormData.label: i18n("3D Diorama Scene:")
            model: [
                { text: i18n("Historic Yorktown, VA (1781 Colonial Waterfront & Coleman Bridge)"), value: "yorktown" },
                { text: i18n("Pre-Generated Generic: Coastal Harbor & Lighthouse Town (Non-City-Specific)"), value: "coastal" },
                { text: i18n("Pre-Generated Generic: Modern Downtown Metropolis & Bridge (Non-City-Specific)"), value: "modern" },
                { text: i18n("Pre-Generated Generic: Alpine Mountain & Lake Village (Non-City-Specific)"), value: "alpine" },
                { text: i18n("Custom Generated High-Res City Diorama Cutout (Any Global City)"), value: "custom_city" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_dioramaTheme))
            onActivated: root.cfg_dioramaTheme = currentValue
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Generate Custom High-Res Diorama Cutout for Any City")
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Target City / Prompt:")
            Layout.fillWidth: true
            spacing: 8

            QQC2.TextField {
                id: customCityPromptField
                text: root.cfg_locationName || "Yorktown, VA"
                placeholderText: i18n("Enter any city or architectural theme (e.g. Kyoto, Paris, Charleston, SC)...")
                Layout.fillWidth: true
            }

            QQC2.Button {
                text: root.generatingCutout ? i18n("Generating...") : i18n("✨ Generate High-Res Cutout")
                icon.name: "view-refresh"
                enabled: !root.generatingCutout
                onClicked: root.generateCustomHighResDiorama(customCityPromptField.text)
            }
        }

        QQC2.Label {
            visible: root.generatorStatus.length > 0
            text: root.generatorStatus
            wrapMode: Text.WordWrap
            Layout.fillWidth: true
            opacity: 0.85
            font.italic: true
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Isometric Zoom & Horizontal Centering (Overlay Stays Fixed)")
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Isometric Zoom:")
            spacing: 10
            QQC2.Slider {
                id: zoomSlider
                from: 0.65
                to: 2.40
                stepSize: 0.05
                value: root.cfg_dioramaZoom
                onMoved: root.cfg_dioramaZoom = Math.round(value * 100) / 100
                Layout.preferredWidth: 180
            }
            QQC2.Label {
                text: Math.round(root.cfg_dioramaZoom * 100) + "%"
                Layout.minimumWidth: 44
            }
            QQC2.Button {
                text: i18n("Reset Zoom")
                onClicked: root.cfg_dioramaZoom = 1.0
            }
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Horizontal Position:")
            spacing: 8
            QQC2.Button {
                text: i18n("◀ Left")
                onClicked: root.cfg_dioramaOffsetX = Math.max(-220, root.cfg_dioramaOffsetX - 24)
            }
            QQC2.Slider {
                id: offsetXSlider
                from: -220
                to: 220
                stepSize: 4
                value: root.cfg_dioramaOffsetX
                onMoved: root.cfg_dioramaOffsetX = Math.round(value)
                Layout.preferredWidth: 150
            }
            QQC2.Button {
                text: i18n("▶ Right")
                onClicked: root.cfg_dioramaOffsetX = Math.min(220, root.cfg_dioramaOffsetX + 24)
            }
            QQC2.Button {
                text: i18n("🎯 Center (0 px)")
                onClicked: root.cfg_dioramaOffsetX = 0
            }
            QQC2.Label {
                text: (root.cfg_dioramaOffsetX > 0 ? "+" : "") + root.cfg_dioramaOffsetX + " px"
                Layout.minimumWidth: 52
            }
        }

        QQC2.CheckBox {
            Kirigami.FormData.label: i18n("Desktop Backdrop:")
            text: i18n("Transparent Desktop Cutout Mode (float 3D island directly over desktop wallpaper)")
            checked: root.cfg_transparentBackground
            onToggled: root.cfg_transparentBackground = checked
        }

        QQC2.CheckBox {
            Kirigami.FormData.label: i18n("Interactive Landmarks:")
            text: i18n("Show landmark tooltips on hover")
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
                { text: i18n("Golden Hour / Sunset"), value: "sunset" },
                { text: i18n("Starry Night (Illuminated Windows & Landmarks)"), value: "night" }
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
                { text: i18n("Preview: Coastal Fog"), value: "fog" }
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
