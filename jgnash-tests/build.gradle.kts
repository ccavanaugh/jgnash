description = "jGnash Core Test Classes"

var moduleName = "jgnash.tests"

val nettyVersion: String by project
val pdfBoxVersion: String by project
val xstreamVersion: String by project
val commonsMathVersion: String by project
val commonsCollectionsVersion: String by project
val commonsTextVersion: String by project

val junitVersion: String by project
val junitExtensionsVersion: String by project
val awaitilityVersion: String by project

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("io.github.glytching:junit-extensions:$junitExtensionsVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.apache.commons:commons-text:$commonsTextVersion")

    testImplementation(project(":jgnash-resources"))
    testImplementation(project(":jgnash-core"))
    testImplementation(project(":jgnash-bayes"))
    testImplementation(project(":jgnash-convert"))
    testImplementation(project(":jgnash-report-core"))

    testImplementation("io.netty:netty-codec:$nettyVersion")
    testImplementation("com.thoughtworks.xstream:xstream:$xstreamVersion")

    testImplementation("org.apache.commons:commons-collections4:$commonsCollectionsVersion")
    testImplementation("org.apache.commons:commons-math3:$commonsMathVersion")
    testImplementation("org.apache.pdfbox:pdfbox:$pdfBoxVersion")
    testImplementation("org.apache.pdfbox:pdfbox-tools:$pdfBoxVersion")
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
    manifest.attributes["Automatic-Module-Name"] = moduleName
}