description = "jGnash Convert"

val moduleName = "jgnash.convert"
val commonsCsvVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":jgnash-resources"))
    implementation(project(":jgnash-core"))
    implementation(project(":jgnash-bayes"))

    implementation("org.apache.commons:commons-csv:$commonsCsvVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}