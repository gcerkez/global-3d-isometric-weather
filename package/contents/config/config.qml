import QtQuick
import org.kde.plasma.configuration

ConfigModel {
    ConfigCategory {
        name: i18n("Location & Units")
        icon: "mark-location"
        source: "config/ConfigLocation.qml"
    }
    ConfigCategory {
        name: i18n("3D Scene & Weather FX")
        icon: "weather-clear"
        source: "config/ConfigAppearance.qml"
    }
}
