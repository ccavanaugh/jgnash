package jgnash;

import jgnash.resource.util.OS;
import jgnash.resource.util.Version;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static jgnash.resource.util.OS.JAVA_VERSION;
import static jgnash.resource.util.Version.getAppVersion;

/**
 * Test methods
 *
 * @author Craig Cavanaugh
 */
class VersionTest {

    @Test
    void gitHub() {
        assertTrue(Version.getLatestGitHubRelease().isPresent());
    }

    @Test
    void testVersion() {
        System.out.println(getAppVersion());

        assertFalse(Version.isReleaseCurrent("2.20"));
        assertFalse(Version.isReleaseCurrent("2.20.0"));
        assertTrue(Version.isReleaseCurrent("10.40.40"));
    }

    @Test
    void testJavaVersion() {

        System.out.println("Java Version: " + OS.getJavaVersion());

        assertTrue(OS.getJavaVersion() > 0);
    }

    @Test
    void testJavaRelease() {

        System.out.println("Java Version: " + OS.getJavaVersion());

        if (Precision.equals(OS.getJavaVersion(), 1.8f)) {  // don't test if not on Java 8
            System.out.println("Java Release: " + OS.getJavaRelease());
            assertTrue(OS.getJavaRelease() > 0);

            // test for early access versions
            System.setProperty(JAVA_VERSION, "1.8.0_202-ea");
            assertEquals(202, OS.getJavaRelease());
        }
    }
}
