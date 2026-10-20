import QtQuick
import QtQuick.Controls as QQC2
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import org.kde.kcmutils as KCM

KCM.SimpleKCM {
    id: root

    property string cfg_locationName
    property double cfg_latitude
    property double cfg_longitude
    property string cfg_renderMode
    property string cfg_dioramaTheme
    property double cfg_dioramaZoom
    property int cfg_dioramaOffsetX
    property int cfg_dioramaOffsetY
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
            Kirigami.FormData.label: i18n("3D Isometric Rendering Engine & Pre-Generated Regional Dioramas")
        }

        QQC2.ComboBox {
            id: renderModeCombo
            Kirigami.FormData.label: i18n("Render Mode:")
            model: [
                { text: i18n("Pre-Generated 3D Isometric Cutout + Real-Time Weather FX (Recommended)"), value: "hybrid" },
                { text: i18n("Interactive Procedural 3D Vector Diorama (Zoomable/Centerable + Live OSM)"), value: "procedural3d" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_renderMode))
            onActivated: root.cfg_renderMode = currentValue
        }

        QQC2.ComboBox {
            id: dioramaThemeCombo
            Kirigami.FormData.label: i18n("3D Isometric Diorama:")
            Layout.fillWidth: true
            model: [
                { text: i18n("Historic Yorktown, VA — 1781 Colonial Waterfront, Coleman Bridge & Tall Ship (NPS)"), value: "yorktown" },
                { text: i18n("Historic Jamestown, VA — 1607 James Fort Palisade, 3 Ships & Turquoise River (NPS)"), value: "jamestown" },
                { text: i18n("Virginia Beach, VA — Oceanfront Boardwalk, Fishing Pier & Cape Henry Lighthouse"), value: "virginia_beach" },
                { text: i18n("Chesapeake Bay & Bridge-Tunnel, VA — Bay Bridge, Oyster & Crab Docks, Lighthouse"), value: "chesapeake_bay" },
                { text: i18n("Washington, D.C. & Mid-Atlantic — US Capitol, Monument, Lincoln Memorial & Tidal Basin"), value: "washington_dc" },
                { text: i18n("New York City & Northeast — Statue of Liberty, Brooklyn Bridge, Empire State & Harbor"), value: "new_york" },
                { text: i18n("Myrtle Beach & Grand Strand, SC — SkyWheel, Oceanfront Resorts & Boardwalk Pier"), value: "myrtle_beach" },
                { text: i18n("Atlanta & Georgia Piedmont — Peachtree Center Tower, Georgia Capitol Dome & Olympic Park"), value: "atlanta" },
                { text: i18n("Orlando & Theme Parks, FL — Fairytale Spires, Roller Coaster & Lake Eola Swan Lagoon"), value: "orlando" },
                { text: i18n("Miami Beach, Florida Keys & Tropics — Art Deco Ocean Drive, Overseas Bridge & Lagoon"), value: "miami_caribbean" },
                { text: i18n("New Orleans & Gulf South — St. Louis Cathedral, French Quarter, Steamboat & Bayou"), value: "new_orleans" },
                { text: i18n("Texas Capitol, Austin & Hill Country — Pink Granite Capitol Dome, River Walk & Oil Derrick"), value: "texas" },
                { text: i18n("Chicago & Great Lakes Midwest — Willis Tower, Red Bascule Bridge, Navy Pier & Bean"), value: "chicago_great_lakes" },
                { text: i18n("Denver Mile High Skyline, CO — Mile High Skyline, Historic Union Station & Rocky Foothills"), value: "denver" },
                { text: i18n("Colorado Springs & Pikes Peak, CO — Garden of the Gods Sandstone Spires, Depot & Manor"), value: "colorado_springs" },
                { text: i18n("Arizona Sonoran Desert & Red Rocks — Sedona Cathedral Rock, Adobe Mission & Sonoran Oasis"), value: "arizona" },
                { text: i18n("Grand Canyon & Southwest — Red-Rock Buttes, Cliff Dwellings, Arch Bridge & Colorado River"), value: "grand_canyon" },
                { text: i18n("Las Vegas Strip & Mojave Oasis, NV — Strip Mega-Resorts, High Roller Wheel & Bellagio Fountains"), value: "las_vegas" },
                { text: i18n("San Francisco & California Coast — Golden Gate Bridge, Alcatraz, Painted Ladies & Bay"), value: "san_francisco" },
                { text: i18n("Seattle & Pacific Northwest — Space Needle, Pike Place, Mt. Rainier & Puget Sound Ferry"), value: "seattle_pnw" },
                { text: i18n("New England & Maritime Atlantic — Lighthouse Point, Schooner & Historic Wharf"), value: "coastal" },
                { text: i18n("London & UK (Europe/London) — Big Ben, Parliament, Tower of London & Thames Bridge"), value: "london_uk" },
                { text: i18n("Paris & Western Europe (Europe/Paris) — Notre-Dame, Louvre Palace, Arc & Seine Bridge"), value: "paris_europe" },
                { text: i18n("Rome, Athens & Mediterranean — Colosseum, Parthenon, Roman Aqueduct & Harbor"), value: "rome_mediterranean" },
                { text: i18n("Cairo, Giza & Middle East — Ancient Citadel, Sacred Temple Pool & Pharos Lighthouse"), value: "cairo_giza" },
                { text: i18n("Tokyo, Kyoto & East Asia (Asia/Tokyo) — 5-Story Pagoda, Imperial Hall & Torii Bridge"), value: "tokyo_east_asia" },
                { text: i18n("Global Modern Metropolis (Sydney / Singapore / Toronto) — Skyline & Harbour Bridge"), value: "modern" },
                { text: i18n("Alpine Mountain & Glacial Lake Village (Rockies / Swiss Alps / Andes)"), value: "alpine" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_dioramaTheme))
            onActivated: root.cfg_dioramaTheme = currentValue
        }

        QQC2.Label {
            text: i18n("Tip: Your 3D Isometric Diorama selection is independent of your weather coordinates — e.g., you can monitor live weather in Tabb, VA while displaying the Jamestown, Yorktown, Grand Canyon, or Tokyo 3D render.")
            wrapMode: Text.WordWrap
            Layout.fillWidth: true
            opacity: 0.78
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

        RowLayout {
            Kirigami.FormData.label: i18n("Vertical Position:")
            spacing: 8
            QQC2.Button {
                text: i18n("▲ Up")
                onClicked: root.cfg_dioramaOffsetY = Math.max(-180, root.cfg_dioramaOffsetY - 24)
            }
            QQC2.Slider {
                id: offsetYSlider
                from: -180
                to: 180
                stepSize: 4
                value: root.cfg_dioramaOffsetY
                onMoved: root.cfg_dioramaOffsetY = Math.round(value)
                Layout.preferredWidth: 150
            }
            QQC2.Button {
                text: i18n("▼ Down")
                onClicked: root.cfg_dioramaOffsetY = Math.min(180, root.cfg_dioramaOffsetY + 24)
            }
            QQC2.Button {
                text: i18n("🎯 Center (0 px)")
                onClicked: root.cfg_dioramaOffsetY = 0
            }
            QQC2.Label {
                text: (root.cfg_dioramaOffsetY > 0 ? "+" : "") + root.cfg_dioramaOffsetY + " px"
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
