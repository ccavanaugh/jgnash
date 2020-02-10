plugins {
    id("com.github.ben-manes.versions")
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        mavenLocal()
    }

    apply(plugin = "java")
}

subprojects {
    group = "jgnash"
    version = "3.5.0"
}