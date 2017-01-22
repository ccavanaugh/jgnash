package jgnash.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to encapsulate file locking
 *
 * @author Craig Cavanaugh
 */
public class FileLocker {
    private FileLock fileLock = null;
    private FileChannel lockChannel = null;

    public boolean acquireLock(final Path path) {
        boolean result = false;

        try {
            lockChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            fileLock = lockChannel.tryLock();

            result = fileLock.isValid();
        } catch (final IOException | OverlappingFileLockException ex) {
            Logger.getLogger(FileLock .class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }

        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean release() {
        boolean result = false;

        try {
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }

            if (lockChannel != null) {
                lockChannel.close();
                lockChannel = null;
            }

            result = true;
        } catch (final IOException ex) {
            Logger.getLogger(FileLock .class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }

        return result;
    }
}
