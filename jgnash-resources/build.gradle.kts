import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem.current
import java.util.*

description = "jGnash Resources"

val moduleName = "jgnash.resources"
val osName = current()!!
val javaVersion = Jvm.current()!!
val timeStamp = Date()

tasks.withType<ProcessResources> {
    filesMatching("**/constants.properties") {
        expand(hashMapOf("version" to version, "javaVersion" to javaVersion,
                "timeStamp" to timeStamp, "osName" to osName))
    }

    filesMatching("**/notice.html") {
        expand(hashMapOf("version" to version, "javaVersion" to javaVersion,
                "timeStamp" to timeStamp, "osName" to osName))
    }
}

tasks.jar {
    manifest.attributes["Automatic-Module-Name"] = moduleName
}




