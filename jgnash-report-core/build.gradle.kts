description = "jGnash Report Core"

val moduleName = "jgnash.report"

val apachePoiVersion: String by project
val pdfBoxVersion: String by project
val commonsLangVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":jgnash-resources"))
    implementation(project(":jgnash-core"))

    implementation("org.apache.poi:poi-ooxml:$apachePoiVersion") {
        exclude(module = "stax-api")
        exclude(module = "xml-apis")
    }

    implementation("org.apache.pdfbox:pdfbox:$pdfBoxVersion")
    implementation("org.apache.pdfbox:pdfbox-tools:$pdfBoxVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
