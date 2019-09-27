import org.gradle.internal.jvm.Jvm
import java.util.*

description = "jGnash Resources"

val moduleName = "jgnash.report"

val javaVersion = Jvm.current()!!
val timeStamp = Date()

tasks.withType<ProcessResources> {
    filesMatching("**/constants.properties") {
        expand(hashMapOf("version" to version, "javaVersion" to javaVersion, "timeStamp" to timeStamp))
    }

    filesMatching("**/notice.html") {
        expand(hashMapOf("version" to version, "javaVersion" to javaVersion, "timeStamp" to timeStamp))
    }
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}




