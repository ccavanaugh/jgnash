description = "jGnash JavaFx"

val javaFXVersion: String by project    // extract JavaFX version from gradle.properties
val picocliVersion: String by project
val testFxVersion: String by project
val monocleVersion: String by project

plugins {
    id("org.openjfx.javafxplugin")
    application // creates a task to run the full application
}

application {
    mainClassName = "jGnashFx"
}

dependencies {
    compile(project(":jgnash-core"))
    compile(project(":jgnash-convert"))
    compile(project(":jgnash-report-core"))
    compile(project(":jgnash-plugin"))

    compile("info.picocli:picocli:$picocliVersion")

    // Hack to include all javafx platforms in the classpath
    // The platform specific libraries are excluded when the distribution is assembled
    compile("org.openjfx:javafx-base:$javaFXVersion")
    compile("org.openjfx:javafx-fxml:$javaFXVersion")
    compile("org.openjfx:javafx-controls:$javaFXVersion")
    compile("org.openjfx:javafx-graphics:$javaFXVersion")
    compile("org.openjfx:javafx-media:$javaFXVersion")
    compile("org.openjfx:javafx-swing:$javaFXVersion")
    compile("org.openjfx:javafx-web:$javaFXVersion")

    compile("org.openjfx:javafx-base:$javaFXVersion:linux")
    compile("org.openjfx:javafx-fxml:$javaFXVersion:linux")
    compile("org.openjfx:javafx-controls:$javaFXVersion:linux")
    compile("org.openjfx:javafx-graphics:$javaFXVersion:linux")
    compile("org.openjfx:javafx-media:$javaFXVersion:linux")
    compile("org.openjfx:javafx-swing:$javaFXVersion:linux")
    compile("org.openjfx:javafx-web:$javaFXVersion:linux")

    compile("org.openjfx:javafx-base:$javaFXVersion:win")
    compile("org.openjfx:javafx-fxml:$javaFXVersion:win")
    compile("org.openjfx:javafx-controls:$javaFXVersion:win")
    compile("org.openjfx:javafx-graphics:$javaFXVersion:win")
    compile("org.openjfx:javafx-media:$javaFXVersion:win")
    compile("org.openjfx:javafx-swing:$javaFXVersion:win")
    compile("org.openjfx:javafx-web:$javaFXVersion:win")

    compile("org.openjfx:javafx-base:$javaFXVersion:mac")
    compile("org.openjfx:javafx-fxml:$javaFXVersion:mac")
    compile("org.openjfx:javafx-controls:$javaFXVersion:mac")
    compile("org.openjfx:javafx-graphics:$javaFXVersion:mac")
    compile("org.openjfx:javafx-media:$javaFXVersion:mac")
    compile("org.openjfx:javafx-swing:$javaFXVersion:mac")
    compile("org.openjfx:javafx-web:$javaFXVersion:mac")
    // end hack

    // required of Unit testing JavaFX
    testImplementation("org.testfx:testfx-junit5:$testFxVersion")
    testImplementation("org.testfx:openjfx-monocle:$monocleVersion")
}

javafx {
    version = javaFXVersion
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing",
            "javafx.graphics", "javafx.media")
}

tasks.withType<CreateStartScripts> {
    enabled = false // disable creation of the start scripts
}

tasks.distZip {

    // build the mt940 plugin prior to creating the zip file without creating a circular loop
    dependsOn(":mt940:jar")

    // add the mt940 plugin
    into("jGnash-${archiveVersion.get()}") {
        from("../mt940/build/libs")
        include("*")
        into("jGnash-${archiveVersion.get()}/plugins")
    }

    into("jGnash-${archiveVersion.get()}") {
        from(".")
        include("scripts/*")
    }
}

distributions {
    main {
        baseName = "jGnash"

        contents {
            exclude("**/*-linux*")  // excludes linux specific JavaFx modules from cross platform zip
            exclude("**/*-win*")    // excludes windows specific JavaFx modules from cross platform zip
            exclude("**/*-mac*")    // excludes mac specific JavaFx modules from cross platform zip
        }
    }
}

/**
 * Returns a proper Class-Path entry for the manifest file
 * @return classpath relative to the installation root point to the jars in the lib directory
 */
fun generateManifestClassPath(): String {
    val path = StringBuilder()

    configurations.runtimeClasspath.get().files.forEach {
        path.append("lib/")
        path.append(it.name)
        path.append(" ")
    }

    return path.toString()
}

tasks.jar {
    // Keep jar clean:
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF")

    manifest {
        attributes(mapOf("Main-Class" to "jGnashFx", "Class-Path" to generateManifestClassPath()))
    }
}
