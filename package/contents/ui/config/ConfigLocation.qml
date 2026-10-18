import QtQuick
import QtQuick.Controls as QQC2
import QtQuick.Layouts
import org.kde.kirigami as Kirigami
import org.kde.kcmutils as KCM

KCM.SimpleKCM {
    id: root

    property string cfg_locationName
    property string cfg_regionSubtitle
    property double cfg_latitude
    property double cfg_longitude
    property string cfg_temperatureUnit
    property string cfg_windSpeedUnit
    property int cfg_refreshIntervalMinutes
    property string cfg_dioramaTheme
    property string cfg_renderMode

    ListModel {
        id: searchResultsModel
    }

    property bool searching: false
    property string searchStatusText: ""

    function searchCity(query) {
        if (!query || query.trim().length < 2) {
            searchStatusText = "Enter at least 2 characters to search."
            return
        }
        searching = true
        searchStatusText = "Searching Open-Meteo geocoding directory..."
        searchResultsModel.clear()

        var xhr = new XMLHttpRequest()
        var url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodeURIComponent(query.trim()) + "&count=8&language=en&format=json"
        xhr.open("GET", url)
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                searching = false
                if (xhr.status === 200) {
                    try {
                        var resp = JSON.parse(xhr.responseText)
                        if (resp.results && resp.results.length > 0) {
                            for (var i = 0; i < resp.results.length; i++) {
                                var r = resp.results[i]
                                var admin = r.admin1 ? r.admin1 : ""
                                var country = r.country_code ? r.country_code : ""
                                var displayName = r.name + (admin ? (", " + admin) : (country ? (", " + country) : ""))
                                searchResultsModel.append({
                                    "displayName": displayName,
                                    "fullName": r.name + (admin ? ", " + admin : "") + (r.country ? " (" + r.country + ")" : ""),
                                    "lat": r.latitude,
                                    "lon": r.longitude,
                                    "elevation": r.elevation || 0
                                })
                            }
                            searchStatusText = "Found " + resp.results.length + " locations. Click one to update live weather coordinates (your 3D diorama scene remains unchanged)."
                        } else {
                            searchStatusText = "No matching locations found."
                        }
                    } catch (e) {
                        searchStatusText = "Error parsing search response."
                    }
                } else {
                    searchStatusText = "Network error while searching (" + xhr.status + ")."
                }
            }
        }
        xhr.send()
    }

    function applyWeatherLocation(name, subtitle, lat, lon) {
        cfg_locationName = name
        cfg_regionSubtitle = subtitle
        cfg_latitude = lat
        cfg_longitude = lon
    }

    function detectCurrentLocation() {
        searchStatusText = "Auto-detecting current location via network/IP..."
        var xhr = new XMLHttpRequest()
        xhr.open("GET", "https://get.geojs.io/v1/ip/geo.json")
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200) {
                    try {
                        var resp = JSON.parse(xhr.responseText)
                        if (resp.city && resp.latitude && resp.longitude) {
                            var disp = resp.city + (resp.region ? (", " + resp.region) : "")
                            applyWeatherLocation(disp, "Current Local Area", parseFloat(resp.latitude), parseFloat(resp.longitude))
                            searchStatusText = "📍 Auto-detected current location: " + disp + " (" + parseFloat(resp.latitude).toFixed(4) + ", " + parseFloat(resp.longitude).toFixed(4) + ")"
                        } else {
                            searchStatusText = "Could not parse location data."
                        }
                    } catch (e) {
                        searchStatusText = "Error parsing IP location response."
                    }
                } else {
                    searchStatusText = "Network error detecting location (" + xhr.status + ")."
                }
            }
        }
        xhr.send()
    }

    Kirigami.FormLayout {
        anchors.fill: parent

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Active Weather Telemetry Location (Independent of 3D Diorama Visual)")
        }

        QQC2.Button {
            Kirigami.FormData.label: i18n("Auto-Detect:")
            text: i18n("📍 Detect Current Computer Location")
            icon.name: "mark-location"
            Layout.fillWidth: true
            onClicked: root.detectCurrentLocation()
        }

        QQC2.TextField {
            id: locationNameField
            Kirigami.FormData.label: i18n("Display Name:")
            text: root.cfg_locationName
            onTextEdited: root.cfg_locationName = text
            Layout.fillWidth: true
        }

        QQC2.TextField {
            id: subtitleField
            Kirigami.FormData.label: i18n("Regional Subtitle:")
            text: root.cfg_regionSubtitle
            onTextEdited: root.cfg_regionSubtitle = text
            Layout.fillWidth: true
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Coordinates:")
            spacing: 8

            QQC2.Label { text: i18n("Lat:") }
            QQC2.TextField {
                text: root.cfg_latitude.toFixed(4)
                implicitWidth: 110
                onEditingFinished: {
                    var v = parseFloat(text)
                    if (!isNaN(v)) root.cfg_latitude = v
                }
            }

            QQC2.Label { text: i18n("Lon:") }
            QQC2.TextField {
                text: root.cfg_longitude.toFixed(4)
                implicitWidth: 110
                onEditingFinished: {
                    var v = parseFloat(text)
                    if (!isNaN(v)) root.cfg_longitude = v
                }
            }
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Search Any City (Open-Meteo Geocoding)")
        }

        RowLayout {
            Kirigami.FormData.label: i18n("Search City:")
            Layout.fillWidth: true
            spacing: 6

            QQC2.TextField {
                id: searchInput
                placeholderText: i18n("e.g., Tabb, Yorktown, Williamsburg, San Francisco, Tokyo...")
                Layout.fillWidth: true
                onAccepted: root.searchCity(text)
            }
            QQC2.Button {
                text: i18n("Search")
                icon.name: "search"
                enabled: !root.searching
                onClicked: root.searchCity(searchInput.text)
            }
        }

        QQC2.Label {
            visible: root.searchStatusText.length > 0
            text: root.searchStatusText
            wrapMode: Text.WordWrap
            Layout.fillWidth: true
            opacity: 0.75
            font.italic: true
        }

        ColumnLayout {
            visible: searchResultsModel.count > 0
            Layout.fillWidth: true
            spacing: 4

            Repeater {
                model: searchResultsModel
                delegate: QQC2.ItemDelegate {
                    Layout.fillWidth: true
                    text: model.fullName + "  [" + model.lat.toFixed(3) + ", " + model.lon.toFixed(3) + "]"
                    icon.name: "mark-location"
                    onClicked: {
                        root.applyWeatherLocation(
                            model.displayName,
                            "Live Open-Meteo Telemetry • 3D Isometric Weather",
                            model.lat,
                            model.lon
                        )
                        root.searchStatusText = "Selected weather location: " + model.displayName + " (3D Diorama kept as configured in Appearance)"
                    }
                }
            }
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Quick Weather Coordinate Presets")
        }

        Flow {
            Kirigami.FormData.label: i18n("Virginia & US:")
            Layout.fillWidth: true
            spacing: 6

            QQC2.Button {
                text: "Yorktown, VA"
                onClicked: root.applyWeatherLocation("Yorktown, VA", "Historic Colonial Waterfront & 1781 Battlefield", 37.2388, -76.5097)
            }
            QQC2.Button {
                text: "Tabb, VA"
                onClicked: root.applyWeatherLocation("Tabb, VA", "York County & Virginia Peninsula", 37.1310, -76.4452)
            }
            QQC2.Button {
                text: "Jamestown, VA"
                onClicked: root.applyWeatherLocation("Jamestown, VA", "Historic 1607 James River Settlement", 37.2085, -76.7741)
            }
            QQC2.Button {
                text: "Williamsburg, VA"
                onClicked: root.applyWeatherLocation("Williamsburg, VA", "Historic Colonial Capital", 37.2707, -76.7075)
            }
            QQC2.Button {
                text: "Washington, D.C."
                onClicked: root.applyWeatherLocation("Washington, D.C.", "National Mall & Tidal Basin", 38.8951, -77.0364)
            }
            QQC2.Button {
                text: "New York, NY"
                onClicked: root.applyWeatherLocation("New York, NY", "Manhattan & Hudson Harbor", 40.7128, -74.0060)
            }
            QQC2.Button {
                text: "San Francisco, CA"
                onClicked: root.applyWeatherLocation("San Francisco, CA", "Bay Area & Golden Gate Waterfront", 37.7749, -122.4194)
            }
            QQC2.Button {
                text: "Chicago, IL"
                onClicked: root.applyWeatherLocation("Chicago, IL", "Lake Michigan & Loop Waterfront", 41.8781, -87.6298)
            }
        }

        Kirigami.Separator {
            Kirigami.FormData.isSection: true
            Kirigami.FormData.label: i18n("Units & Polling")
        }

        QQC2.ComboBox {
            id: tempUnitCombo
            Kirigami.FormData.label: i18n("Temperature Display:")
            model: [
                { text: i18n("Dual: °F (°C) — Reference Style (e.g. 46°F (8°C))"), value: "BOTH" },
                { text: i18n("Fahrenheit only (°F)"), value: "F" },
                { text: i18n("Celsius only (°C)"), value: "C" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_temperatureUnit))
            onActivated: root.cfg_temperatureUnit = currentValue
        }

        QQC2.ComboBox {
            id: windUnitCombo
            Kirigami.FormData.label: i18n("Wind Speed Unit:")
            model: [
                { text: i18n("Miles per hour (MPH)"), value: "mph" },
                { text: i18n("Kilometers per hour (KM/H)"), value: "kmh" },
                { text: i18n("Knots (KN)"), value: "kn" },
                { text: i18n("Meters per second (M/S)"), value: "ms" }
            ]
            textRole: "text"
            valueRole: "value"
            Component.onCompleted: currentIndex = Math.max(0, indexOfValue(root.cfg_windSpeedUnit))
            onActivated: root.cfg_windSpeedUnit = currentValue
        }

        QQC2.SpinBox {
            Kirigami.FormData.label: i18n("Refresh Interval (minutes):")
            from: 5
            to: 120
            stepSize: 5
            value: root.cfg_refreshIntervalMinutes
            onValueModified: root.cfg_refreshIntervalMinutes = value
        }
    }
}
