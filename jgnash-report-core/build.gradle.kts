description = "jGnash Report Core"

val moduleName = "jgnash.report"

val apachePoiVersion: String by project
val pdfBoxVersion: String by project
val commonsLangVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":jgnash-core"))

    api("org.apache.poi:poi-ooxml:$apachePoiVersion") {
        exclude(module = "stax-api")
        exclude(module = "xml-apis")
    }

    api("org.apache.pdfbox:pdfbox:$pdfBoxVersion")
    api("org.apache.pdfbox:pdfbox-tools:$pdfBoxVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
