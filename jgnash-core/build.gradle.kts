description = "jGnash Core"

var moduleName = "jgnash.core"

val commonsCollectionsVersion: String by project
val commonsCsvVersion: String by project
val commonsLangVersion: String by project
val commonsMathVersion: String by project

val slf4jVersion: String by project
val hibernateVersion: String by project
val hikariVersion: String by project
val h2Version: String by project
val hsqldbVersion: String by project
val xstreamVersion: String by project
val nettyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    implementation(project(":jgnash-resources"))

    // required for HikariCP, override with modular version
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")

    api("org.hibernate:hibernate-entitymanager:$hibernateVersion")
    implementation("org.hibernate:hibernate-hikaricp:$hibernateVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    implementation("com.h2database:h2:$h2Version")
    implementation("org.hsqldb:hsqldb:$hsqldbVersion")

    implementation("com.thoughtworks.xstream:xstream:$xstreamVersion") {
        exclude(module = "xmlpull")
        exclude(module = "xpp3_min")
    }

    implementation("com.thoughtworks.xstream:xstream-hibernate:$xstreamVersion") {
        exclude(module = "xmlpull")
        exclude(module = "xpp3_min")
    }

    implementation("io.netty:netty-codec:$nettyVersion")

    implementation("org.apache.commons:commons-collections4:$commonsCollectionsVersion")
    implementation("org.apache.commons:commons-csv:$commonsCsvVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("org.apache.commons:commons-math3:$commonsMathVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
