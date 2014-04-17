/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.util.EncryptionManager;

import io.netty.util.ResourceLeakDetector;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * File transfer test
 *
 * @author Craig Cavanaugh
 */
public class FileTransferTest {

    @Test
    public void encryptedNetworkedTest() throws Exception {

        final char[] password = new char[]{'p','a','s','s','w','o','r','d'};
        final int port = 5300;

        System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "true");
        System.setProperty("ssl", "true");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        String testFile = null;

        try {
            File temp = Files.createTempFile("jpa-test-e", "." + JpaH2DataStore.FILE_EXT).toFile();
            Assert.assertTrue(temp.delete());
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (IOException e1) {
            System.err.println(e1.toString());
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, password, DataStoreType.H2_DATABASE);
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        new Thread() {

            @Override
            public void run() {
                System.out.println("starting server");
                networkServer.startServer(serverFile, port, password);
            }
        }.start();

        Thread.sleep(4000);

        try {
            Engine e = EngineFactory.bootClientEngine("localhost", port, password, EngineFactory.DEFAULT);

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


            // Create a new temp file in the directory
            tempAttachment = Files.createTempFile(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile)), "tempfile2-", ".txt");
            tempAttachment.toFile().deleteOnExit();

            //write it
            BufferedWriter bw = Files.newBufferedWriter(tempAttachment, Charset.defaultCharset());
            bw.write("This is the temporary file content 2.");
            bw.close();

            Future<Path> pathFuture = e.getAttachment(tempAttachment.getFileName().toString());

            Path remoteTemp = pathFuture.get();

            assertTrue(Files.exists(remoteTemp));
            assertNotEquals(remoteTemp.toString(), tempAttachment.toString());

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void networkedTest() throws Exception {
        final char[] password = new char[]{};
        final int port = 5400;

        System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "false");
        System.setProperty("ssl", "false");

        String testFile = null;

        try {
            File temp = Files.createTempFile("jpa-test", "." + JpaHsqlDataStore.FILE_EXT).toFile();
            Assert.assertTrue(temp.delete());
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (IOException e1) {
            System.err.println(e1.toString());
            fail();
        }

        // Start an engine and close so we have a populated file
        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, password, DataStoreType.HSQL_DATABASE);
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        final JpaNetworkServer networkServer = new JpaNetworkServer();

        final String serverFile = testFile;

        new Thread() {

            @Override
            public void run() {
                System.out.println("starting server");
                networkServer.startServer(serverFile, port, password);
            }
        }.start();

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


            // Create a new temp file in the directory
            tempAttachment = Files.createTempFile(AttachmentUtils.getAttachmentDirectory(Paths.get(testFile)), "tempfile2-", ".txt");
            tempAttachment.toFile().deleteOnExit();

            //write it
            BufferedWriter bw = Files.newBufferedWriter(tempAttachment, Charset.defaultCharset());
            bw.write("This is the temporary file content 2.");
            bw.close();

            Future<Path> pathFuture = e.getAttachment(tempAttachment.getFileName().toString());

            Path remoteTemp = pathFuture.get();

            assertTrue(Files.exists(remoteTemp));
            assertNotEquals(remoteTemp.toString(), tempAttachment.toString());

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }
}
