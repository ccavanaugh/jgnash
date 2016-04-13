package jgnash;

import jgnash.util.Version;
import org.junit.Test;

import static jgnash.util.Version.getAppVersion;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test methods
 *
 * @author Craig Cavanaugh
 */
public class VersionTest {

    @Test
    public void testVersion() {
        System.out.println(getAppVersion());

        assertFalse(Version.isReleaseCurrent("2.20"));
        assertFalse(Version.isReleaseCurrent("2.20.0"));
        assertTrue(Version.isReleaseCurrent("10.40.40"));
    }
}
