/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.register;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.ImageDialog;
import jgnash.ui.components.YesNoDialog;
import jgnash.ui.util.IconUtils;
import jgnash.util.FileUtils;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Manages transaction attachments
 *
 * @author Craig Cavanaugh
 */
class AttachmentPanel extends JPanel implements ActionListener {

    private static final String LAST_DIR = "LastDir";
    private final JButton viewAttachmentButton;
    private final JButton attachmentButton;
    private final JButton deleteButton;
    private final ResourceBundle rb = ResourceUtils.getBundle();
    private Path attachment = null;
    private boolean moveAttachment = false;

    AttachmentPanel() {
        attachmentButton = new JButton(IconUtils.getIcon("/jgnash/resource/mail-attachment.png"));
        deleteButton = new JButton(IconUtils.getIcon("/jgnash/resource/edit-delete.png"));
        viewAttachmentButton = new JButton(IconUtils.getIcon("/jgnash/resource/zoom-fit-best.png"));

        attachmentButton.setToolTipText(rb.getString("ToolTip.AddAttachment"));
        deleteButton.setToolTipText(rb.getString("ToolTip.DeleteAttachment"));
        viewAttachmentButton.setToolTipText(rb.getString("ToolTip.ViewAttachment"));

        FormLayout layout = new FormLayout("m, $rgap, m, $rgap, m", "f:d");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.append(attachmentButton, viewAttachmentButton, deleteButton);

        registerListeners();

        updateControlStates();
    }

    private void registerListeners() {
        attachmentButton.addActionListener(this);
        deleteButton.addActionListener(this);
        viewAttachmentButton.addActionListener(this);
    }

    Transaction modifyTransaction(final Transaction transaction) {
        // preserve any prior attachments, push file request into the background
        new SwingWorker<Path, Void>() {

            @Override
            protected Path doInBackground() throws Exception {
                if (transaction.getAttachment() != null && !transaction.getAttachment().isEmpty()) {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    final Future<Path> pathFuture = engine.getAttachment(transaction.getAttachment());
                    return pathFuture.get();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    attachment = get();
                    updateControlStates();
                } catch (final ExecutionException | InterruptedException e) {
                    Logger.getLogger(AttachmentPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }.execute();

        return transaction;
    }

    void clear() {
        moveAttachment = false;
        attachment = null;

        updateControlStates();
    }

    /**
     * Chain builder for a new transaction
     *
     * @param transaction Transaction to work with
     * @return chained return of the passed transaction
     */
    Transaction buildTransaction(final Transaction transaction) {
        if (attachment != null && attachment.getFileName() != null) {
            final Path path = attachment.getFileName();

            if (moveAttachment) {   // move the attachment first
                if (moveAttachment()) {
                    transaction.setAttachment(path.toString());
                } else {
                    transaction.setAttachment(null);

                    final String message = ResourceUtils.getString("Message.Error.TransferAttachment", path.toString());

                    StaticUIMethods.displayError(message);
                }
            }
        } else {
            transaction.setAttachment(null);
        }

        updateControlStates();

        return transaction;
    }

    private boolean moveAttachment() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return engine.addAttachment(attachment, false);
    }

    private void attachmentAction() {
        final Preferences pref = Preferences.userNodeForPackage(AbstractBankTransactionPanel.class);
        final String baseFile = EngineFactory.getActiveDatabase();

        final String[] fileSuffixes = ImageIO.getReaderFileSuffixes();

        StringBuilder description = new StringBuilder(rb.getString("Title.ImageFiles")).append(" (");

        for (int i = 0; i < fileSuffixes.length; i++) {
            description.append("*.");
            description.append(fileSuffixes[i]);
            if (i < fileSuffixes.length - 1) {
                description.append(", ");
            }
        }

        description.append(")");

        FileFilter fileFilter = new FileNameExtensionFilter(description.toString(), fileSuffixes);

        final JFileChooser chooser = new JFileChooser(pref.get(LAST_DIR, null));
        chooser.addChoosableFileFilter(fileFilter);
        chooser.setFileFilter(fileFilter);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);

        if (attachment != null) {
            chooser.setSelectedFile(attachment.toFile());
        }

        if (chooser.showOpenDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(LAST_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            File selectedFile = chooser.getSelectedFile();

            if (selectedFile != null) {

                boolean result = true;

                final Path attachmentDirectory = AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile));

                if (baseFile.startsWith(EngineFactory.REMOTE_PREFIX)) { // working remotely
                    moveAttachment = true;
                } else if (attachmentDirectory != null && !attachmentDirectory.toString().equals(selectedFile.getParent())) {

                    String message = ResourceUtils.getString("Message.Warn.MoveFile", selectedFile.toString(),
                            attachmentDirectory.toString());

                    result = YesNoDialog.showYesNoDialog(UIApplication.getFrame(), new JLabel(message), rb.getString("Title.MoveFile"));

                    if (result) {
                        moveAttachment = true;

                        Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)) +
                                FileUtils.SEPARATOR + selectedFile.getName());

                        if (newPath.toFile().exists()) {
                            message = ResourceUtils.getString("Message.Warn.SameFile", selectedFile.toString(),
                                    attachmentDirectory.toString());

                            StaticUIMethods.displayWarning(message);
                            moveAttachment = false;
                            result = false;
                        }
                    }
                }

                if (result) {
                    attachment = selectedFile.toPath();
                }
            }
        }
    }

    private void showImageAction() {
        if (attachment != null) {
            if (Files.exists(attachment)) {
                ImageDialog.showImage(attachment.toFile());
            } else {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.MissingAttachment",
                        attachment.toString()));
            }
        }
    }

    private void updateControlStates() {
        attachmentButton.setEnabled(attachment == null);
        deleteButton.setEnabled(attachment != null);
        viewAttachmentButton.setEnabled(attachment != null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == attachmentButton) {
            attachmentAction();
        } else if (e.getSource() == deleteButton) {
            attachment = null;

        } else if (e.getSource() == viewAttachmentButton) {
            showImageAction();
        }

        updateControlStates();
    }
}
