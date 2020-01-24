/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.jpa.SqlUtils;
import jgnash.util.FileUtils;

import org.junit.jupiter.api.Test;

import io.netty.util.ResourceLeakDetector;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * File transfer test.
 *
 * @author Craig Cavanaugh
 */
class FileTransferTest {

    @Test
    void encryptedNetworkedTest() {

        final int port = JpaNetworkServer.DEFAULT_PORT + 120;

        final char[] password = new char[]{'p','a','s','s','w','o','r','d'};

        //System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "true");
        //System.setProperty("ssl", "true");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        String testFile = null;

        try {
            final Path temp = Files.createTempFile("jpa-test-e", JpaH2DataStore.H2_FILE_EXT);
            Files.delete(temp);

            temp.toFile().deleteOnExit();
            testFile = temp.toString();

            assertNotNull(testFile);
        } catch (final IOException e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.H2_DATABASE);

        EngineFactory.getEngine(EngineFactory.DEFAULT).setCreateBackups(false); // disable for test

        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        // Change the password
        SqlUtils.changePassword(testFile, EngineFactory.EMPTY_PASSWORD, password);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        Logger.getLogger(FileTransferTest.class.getName()).info("Starting Server");

        StartServerThread startServerThread = new StartServerThread(networkServer, serverFile,
                port, password);

        startServerThread.start();

        // wait until the server is up and running
        await().atMost(20, TimeUnit.SECONDS).untilTrue(startServerThread.running);

        try {
            Engine e = EngineFactory.bootClientEngine(EngineFactory.LOCALHOST, port,
                    password, EngineFactory.DEFAULT);

            Account account = new Account(AccountType.CASH, e.getDefaultCurrency());
            account.setName("test");
            e.addAccount(e.getRootAccount(), account);

            Path tempAttachment = Paths.get(FileTransferTest.class.getResource("/jgnash-logo.png").toURI());
            assertTrue(Files.exists(tempAttachment));

            assertTrue(e.addAttachment(tempAttachment, true));  // push a copy of the attachment

            Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile))
                    + FileUtils.SEPARATOR + tempAttachment.getFileName());

            newPath.toFile().deleteOnExit();

            // wait for transfer to finish
            await().atMost(10, TimeUnit.SECONDS).until(() -> Files.exists(newPath));

            // Verify copy has occurred
            assertEquals(tempAttachment.toFile().length(), newPath.toFile().length()); // same length?
            assertNotEquals(tempAttachment.toString(), newPath.toString()); // different files?

            final Path attachmentPath = AttachmentUtils.getAttachmentDirectory(Paths.get(testFile));
            assertNotNull(attachmentPath);

            // Create a new temp file in the directory
            tempAttachment = Files.createTempFile(attachmentPath, "tempfile2-", ".txt");
            tempAttachment.toFile().deleteOnExit();

            //write it
            try (final BufferedWriter bw = Files.newBufferedWriter(tempAttachment, Charset.defaultCharset())) {
                bw.write("This is the temporary file content 2.");
            }

            Future<Path> pathFuture = e.getAttachment(tempAttachment.getFileName().toString());

            Path remoteTemp = pathFuture.get();

            assertTrue(Files.exists(remoteTemp));
            assertNotEquals(remoteTemp.toString(), tempAttachment.toString());

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (final Exception e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

    }

    @Test
    void networkedTest() {
        final int port = JpaNetworkServer.DEFAULT_PORT + 110;

        //System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "false");
        //System.setProperty("ssl", "false");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        String testFile = null;

        try {
            Path temp = Files.createTempFile("jpa-test", JpaHsqlDataStore.FILE_EXT);
            Files.delete(temp);

            temp.toFile().deleteOnExit();

            testFile = temp.toString();

            assertNotNull(testFile);
        } catch (final IOException e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.HSQL_DATABASE);

        EngineFactory.getEngine(EngineFactory.DEFAULT).setCreateBackups(false); // disable for test

        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        Logger.getLogger(FileTransferTest.class.getName()).info("Starting Server");

        StartServerThread startServerThread = new StartServerThread(networkServer, serverFile, port,
                EngineFactory.EMPTY_PASSWORD);

        startServerThread.start();

        // wait until the server is up and running
        await().atMost(20, TimeUnit.SECONDS).untilTrue(startServerThread.running);

        try {
            Engine e = EngineFactory.bootClientEngine(EngineFactory.LOCALHOST, port, EngineFactory.EMPTY_PASSWORD,
                    EngineFactory.DEFAULT);

            Account account = new Account(AccountType.CASH, e.getDefaultCurrency());
            account.setName("test");
            e.addAccount(e.getRootAccount(), account);

            Path tempAttachment = Paths.get(FileTransferTest.class.getResource("/jgnash-logo.png").toURI());
            assertTrue(Files.exists(tempAttachment));

            assertTrue(e.addAttachment(tempAttachment, true));  // push a copy of the attachment

            final Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile))
                    + FileUtils.SEPARATOR + tempAttachment.getFileName());

            // wait for transfer to finish
            await().atMost(10, TimeUnit.SECONDS).until(() -> Files.exists(newPath));

            newPath.toFile().deleteOnExit();

            // Verify copy has occurred
            assertEquals(tempAttachment.toFile().length(), newPath.toFile().length()); // same length?
            assertNotEquals(tempAttachment.toString(), newPath.toString()); // different files?

            // Test that move is working
            Path moveFile = Files.createTempFile("jgnash", "test");
            
            try (final BufferedWriter bw = Files.newBufferedWriter(moveFile, Charset.defaultCharset())) {
            	 bw.write("This is the temporary file content 3.");
            }
                                 
            assertTrue(e.addAttachment(moveFile, false));
            assertFalse(Files.exists(moveFile));

            final Path attachmentPath = AttachmentUtils.getAttachmentDirectory(Paths.get(testFile));
            assertNotNull(attachmentPath);

            // Create a new temp file in the directory
            tempAttachment = Files.createTempFile(attachmentPath, "tempfile2-", ".txt");
            tempAttachment.toFile().deleteOnExit();
            
            try (final BufferedWriter bw = Files.newBufferedWriter(tempAttachment, Charset.defaultCharset())) {
            	 bw.write("This is the temporary file content 2.");
            }         

            Future<Path> pathFuture = e.getAttachment(tempAttachment.getFileName().toString());

            Path remoteTemp = pathFuture.get();

            assertTrue(Files.exists(remoteTemp));
            assertNotEquals(remoteTemp.toString(), tempAttachment.toString());

            // test attachment removal
            assertTrue(e.removeAttachment(moveFile.getFileName().toString()));
            assertFalse(Files.exists(moveFile));

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (Exception e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

    }

    private static class StartServerThread extends Thread {

        private final JpaNetworkServer networkServer;
        private final String serverFile;
        private final int port;
        private final char[] password;

        final AtomicBoolean running = new AtomicBoolean(false);

        StartServerThread(JpaNetworkServer networkServer, String serverFile, int port, char[] password) {
            this.networkServer = networkServer;
            this.serverFile = serverFile;
            this.port = port;
            this.password = password;
        }

        @Override
        public void run() {
            networkServer.startServer(serverFile, port, password, () -> running.set(true));
        }
    }
}
