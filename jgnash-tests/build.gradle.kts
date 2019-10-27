description = "jGnash Core Test Classes"

var moduleName = "jgnash.tests"

val nettyVersion: String by project
val pdfBoxVersion: String by project
val xstreamVersion: String by project
val commonsMathVersion: String by project

dependencies {
    testImplementation(project(":jgnash-resources"))
    testImplementation(project(":jgnash-core"))
    testImplementation(project(":jgnash-bayes"))
    testImplementation(project(":jgnash-convert"))
    testImplementation(project(":jgnash-report-core"))

    testImplementation("io.netty:netty-codec:$nettyVersion")
    testImplementation("com.thoughtworks.xstream:xstream:$xstreamVersion")

    testImplementation("org.apache.commons:commons-math3:$commonsMathVersion")
    testImplementation("org.apache.pdfbox:pdfbox:$pdfBoxVersion")
    testImplementation("org.apache.pdfbox:pdfbox-tools:$pdfBoxVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}