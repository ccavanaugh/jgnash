description = "jGnash Convert"

val moduleName = "jgnash.convert"

val commonsCsvVersion: String by project

dependencies {
    implementation(project(":jgnash-core"))
    implementation(project(":jgnash-bayes"))

    implementation("org.apache.commons:commons-csv:$commonsCsvVersion")

    testImplementation(project(":jgnash-core-test"))
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}