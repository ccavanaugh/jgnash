description = "jGnash Test Plugin"

val javaFXVersion: String by project    // extract JavaFX version from gradle.properties

plugins {
  id("org.openjfx.javafxplugin")
}

dependencies {
  implementation(project(":jgnash-core"))
  implementation(project(":jgnash-plugin"))
  implementation(project(":jgnash-fx"))
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
    attributes(mapOf("Plugin-Activator" to "jgnash.uifx.plugin.TestFxPlugin", "Plugin-Version" to "2.25"))
  }
}
