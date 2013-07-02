/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.attachment.AttachmentTransferClient;
import jgnash.engine.attachment.AttachmentTransferServer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * File transfer test
 *
 * @author Craig Cavanaugh
 */
public class FileTransferTest {

    @Test
    public void testTransfer() throws InterruptedException {
        String testFile = null;

        try {

            File tempFile = File.createTempFile("test", ".tmp");
            tempFile.deleteOnExit();
            tempFile.delete();

            testFile = tempFile.getAbsolutePath();
        } catch (IOException e1) {
            Logger.getLogger(BinaryXStreamEngineTest.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
        }

        EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, new char[]{}, DataStoreType.BINARY_XSTREAM);

        int port = 5003;

        AttachmentTransferServer fileServer = new AttachmentTransferServer(port);
        AttachmentTransferClient fileClient = new AttachmentTransferClient();

        fileServer.startServer();
        fileClient.connectToServer("localhost", port);

        try {
            File temp = File.createTempFile("tempfile", ".txt");
            temp.deleteOnExit();

            //write it
            BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
            bw.write("This is the temporary file content.");
            bw.close();

            fileClient.requestFile(temp);

            Thread.sleep(2000);

            File transferFile = new File(AttachmentUtils.getAttachmentDirectory().toString() + File.separator + temp.getName());

            assertTrue(transferFile.exists());

            transferFile.deleteOnExit();

            assertEquals(temp.length(), transferFile.length());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileClient.disconnectFromServer();
            fileServer.stopServer();
        }

        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }
}
