description = "jGnash Plugin"

val moduleName = "jgnash.plugin"
val javaFXVersion: String by project    // extract JavaFX version from gradle.properties

plugins {
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":jgnash-resources"))
}

javafx {
    version = javaFXVersion
    modules("javafx.controls", "javafx.fxml")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
