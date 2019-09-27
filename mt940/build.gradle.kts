description = "mt940 Plugin"

val javaFXVersion: String by project    // extract JavaFX version from gradle.properties

plugins {
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":jgnash-core"))
    implementation(project(":jgnash-fx"))
    implementation(project(":jgnash-plugin"))
    implementation(project(":jgnash-convert"))
}

javafx {
    version = javaFXVersion
    modules("javafx.controls")
}

tasks.jar {
    // Keep jar clean:
    exclude ("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF")

    // required by the plugin interface
    manifest {
        attributes(mapOf("Plugin-Activator" to "net.bzzt.swift.mt940.Mt940Plugin", "Plugin-Version" to "2.25"))
    }
}
