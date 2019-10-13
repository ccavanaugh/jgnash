description = "jGnash Core Test Classes"

val moduleName = "jgnash.core.test"

val junitVersion: String by project
val junitExtensionsVersion: String by project
val awaitilityVersion: String by project
val commonsTextVersion: String by project

dependencies {
    implementation(project(":jgnash-core"))

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    implementation("io.github.glytching:junit-extensions:$junitExtensionsVersion")

    implementation("org.awaitility:awaitility:$awaitilityVersion")

    implementation("org.apache.commons:commons-text:$commonsTextVersion")
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}
