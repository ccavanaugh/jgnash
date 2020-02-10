description = "mt940 Plugin"

val javaFXVersion: String by project    // extract JavaFX version from gradle.properties

val junitVersion: String by project
val junitExtensionsVersion: String by project
val awaitilityVersion: String by project

plugins {
    id("org.openjfx.javafxplugin")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("io.github.glytching:junit-extensions:$junitExtensionsVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")

    implementation(project(":jgnash-resources"))
    implementation(project(":jgnash-core"))
    implementation(project(":jgnash-fx"))
    implementation(project(":jgnash-plugin"))
    implementation(project(":jgnash-convert"))
}

javafx {
    version = javaFXVersion
    modules("javafx.controls")
}

tasks.test {
    useJUnitPlatform()

    // we want display the following test events
    testLogging {
        events("PASSED", "STARTED", "FAILED", "SKIPPED")
        showStandardStreams = true
    }
}

tasks.jar {
    // Keep jar clean:
    exclude ("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF")

    // required by the plugin interface
    manifest {
        attributes(mapOf("Plugin-Activator" to "net.bzzt.swift.mt940.Mt940Plugin", "Plugin-Version" to "2.25"))
    }
}
