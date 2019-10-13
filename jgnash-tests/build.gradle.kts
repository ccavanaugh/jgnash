description = "jGnash Core Test Classes"

var moduleName = "jgnash.tests"

dependencies {
    testImplementation(project(":jgnash-core"))
    testImplementation(project(":jgnash-convert"))
    testImplementation(project(":jgnash-report-core"))
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}