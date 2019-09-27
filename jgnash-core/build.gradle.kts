description = "jGnash Core"

var moduleName = "jgnash.core"

val commonsLangVersion: String by project
val slf4jVersion: String by project
val hibernateVersion: String by project
val hikariVersion: String by project
val h2Version: String by project
val hsqldbVersion: String by project
val xstreamVersion: String by project
val nettyVersion: String by project
val commonsCollectionsVersion: String by project

dependencies {
    compile(project(":jgnash-resources"))

    // required for HikariCP, override with modular version
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")

    compile("org.hibernate:hibernate-entitymanager:$hibernateVersion")
    compile("org.hibernate:hibernate-hikaricp:$hibernateVersion")
    compile("com.zaxxer:HikariCP:$hikariVersion")

    compile("com.h2database:h2:$h2Version")
    compile("org.hsqldb:hsqldb:$hsqldbVersion")

    implementation("com.thoughtworks.xstream:xstream:$xstreamVersion") {
        exclude(module = "xmlpull")
        exclude(module = "xpp3_min")
    }

    implementation("com.thoughtworks.xstream:xstream-hibernate:$xstreamVersion") {
        exclude(module = "xmlpull")
        exclude(module = "xpp3_min")
    }

    implementation("io.netty:netty-codec:$nettyVersion")

    compile("org.apache.commons:commons-collections4:$commonsCollectionsVersion")
    compile("org.apache.commons:commons-lang3:$commonsLangVersion")

    // pulls in abstract test class that is reused by multiple modules
    testImplementation(project(":jgnash-core-test"))
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
