package jgnash;

import jgnash.util.OS;
import jgnash.util.Version;
import org.junit.jupiter.api.Test;

import static jgnash.util.Version.getAppVersion;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        if (OS.getJavaVersion() == 1.8f) {  // don't test if not on Java 8
            System.out.println("Java Release: " + OS.getJavaRelease());
            assertTrue(OS.getJavaRelease() > 0);
        }
    }
}
