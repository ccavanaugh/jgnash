description = "jGnash Convert"

val moduleName = "jgnash.convert"
val commonsCsvVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":jgnash-core"))
    api(project(":jgnash-bayes"))

    implementation("org.apache.commons:commons-csv:$commonsCsvVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}