// extract plugin versions from gradle.properties
val javafxPluginVersion: String by settings
val launch4jVersion: String by settings
val versionsPluginVersion: String by settings

pluginManagement {
    plugins {
        id("org.openjfx.javafxplugin") version javafxPluginVersion
        id ("edu.sc.seis.launch4j") version launch4jVersion
        id ("com.github.ben-manes.versions") version versionsPluginVersion
    }
}

rootProject.name = "jgnash"

include ("bootloader", "jgnash-bayes", "jgnash-resources", "jgnash-core", "jgnash-convert",
        "jgnash-plugin", "jgnash-fx", "jgnash-report-core", "jgnash-fx-test-plugin", "mt940")

