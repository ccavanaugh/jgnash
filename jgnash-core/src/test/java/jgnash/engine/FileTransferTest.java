/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import io.netty.util.ResourceLeakDetector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.jpa.SqlUtils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * File transfer test.
 *
 * @author Craig Cavanaugh
 */
public class FileTransferTest {

    @Test
    public void encryptedNetworkedTest() throws Exception {

        final char[] password = new char[]{'p','a','s','s','w','o','r','d'};

        //System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "true");
        //System.setProperty("ssl", "true");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        String testFile = null;

        try {
            File temp = Files.createTempFile("jpa-test-e", "." + JpaH2DataStore.FILE_EXT).toFile();
            Assert.assertTrue(temp.delete());
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (final IOException e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, new char[]{}, DataStoreType.H2_DATABASE);
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        // Change the password
        SqlUtils.changePassword(testFile, new char[]{}, password);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        System.out.println("starting server");
        new StartServerThread(networkServer, serverFile, JpaNetworkServer.DEFAULT_PORT, password).start();

        Thread.sleep(4000);

        try {
            Engine e = EngineFactory.bootClientEngine("localhost", JpaNetworkServer.DEFAULT_PORT, password, EngineFactory.DEFAULT);

            Account account = new Account(AccountType.CASH, e.getDefaultCurrency());
            account.setName("test");
            e.addAccount(e.getRootAccount(), account);

            Path tempAttachment = Paths.get(Object.class.getResource("/jgnash-logo.png").toURI());
            assertTrue(Files.exists(tempAttachment));

            assertTrue(e.addAttachment(tempAttachment, true));  // push a copy of the attachment

            Thread.sleep(4000); // wait for the transfer to finish, it may have been pushed into the background

            Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile)) + File.separator + tempAttachment.getFileName());
            newPath.toFile().deleteOnExit();

            // Verify copy has occurred
            assertEquals(tempAttachment.toFile().length(), newPath.toFile().length()); // same length?
            assertNotEquals(tempAttachment.toString(), newPath.toString()); // different files?

            final Path attachmentPath = AttachmentUtils.getAttachmentDirectory(Paths.get(testFile));
            assertNotNull(attachmentPath);

            // Create a new temp file in the directory
            tempAttachment = Files.createTempFile(attachmentPath, "tempfile2-", ".txt");
            tempAttachment.toFile().deleteOnExit();

            //write it
            try (BufferedWriter bw = Files.newBufferedWriter(tempAttachment, Charset.defaultCharset())) {
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
    public void networkedTest() throws Exception {
        final char[] password = new char[]{};
        final int port = JpaNetworkServer.DEFAULT_PORT + 100;

        //System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "false");
        //System.setProperty("ssl", "false");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        String testFile = null;

        try {
            File temp = Files.createTempFile("jpa-test", "." + JpaHsqlDataStore.FILE_EXT).toFile();
            Assert.assertTrue(temp.delete());
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (final IOException e) {
            Logger.getLogger(FileTransferTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, password, DataStoreType.HSQL_DATABASE);
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        Logger.getLogger(FileTransferTest.class.getName()).info("Starting Server");
        new StartServerThread(networkServer, serverFile, port, password).start();

        Thread.sleep(4000);

        try {
            Engine e = EngineFactory.bootClientEngine("localhost", port, password, EngineFactory.DEFAULT);

            Account account = new Account(AccountType.CASH, e.getDefaultCurrency());
            account.setName("test");
            e.addAccount(e.getRootAccount(), account);

            Path tempAttachment = Paths.get(Object.class.getResource("/jgnash-logo.png").toURI());
            assertTrue(Files.exists(tempAttachment));

            assertTrue(e.addAttachment(tempAttachment, true));  // push a copy of the attachment

            Thread.sleep(4000); // wait for transfer to finish

            Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile)) + File.separator + tempAttachment.getFileName());
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

        StartServerThread(JpaNetworkServer networkServer, String serverFile, int port, char[] password) {
            this.networkServer = networkServer;
            this.serverFile = serverFile;
            this.port = port;
            this.password = password;
        }

        @Override
        public void run() {
            networkServer.startServer(serverFile, port, password);
        }
    }
}
