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
package jgnash.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JPanel;

import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import org.jdesktop.swingx.util.GraphicsUtilities;

/**
 * Dialog for displaying an Image
 *
 * @author Craig Cavanaugh
 */
public class ImageDialog extends JDialog {

    final ImagePanel imagePanel;

    public static void showImage(final File file) {
        ImageDialog dialog = new ImageDialog();
        dialog.setImage(file);

        DialogUtils.addBoundsListener(dialog);

        dialog.setVisible(true);
    }

    ImageDialog() {
        setTitle(Resource.get().getString("Title.ViewImage"));
        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));

        imagePanel = new ImagePanel();

        getContentPane().add(imagePanel);
        setMinimumSize(new Dimension(250, 250));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagePanel.resizeImage();
            }
        });

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void setImage(final File file) {
        imagePanel.loadImage(file);

        if (imagePanel.image != null) {
            setMaximumSize(new Dimension(imagePanel.image.getWidth(), imagePanel.image.getHeight()));
        }
    }

    private class ImagePanel extends JPanel {
        public static final int MARGIN = 5;

        private BufferedImage image;
        private BufferedImage scaledImage;

        void loadImage(final File file) {
            try {
                image = ImageIO.read(file);
            } catch (final IOException e) {
                Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }

        void resizeImage() {
            buildScaledImage();
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            if (getWidth() != 0 || getHeight() != 0 && scaledImage != null) {

                int x = (getWidth() - scaledImage.getWidth()) / 2;
                int y = (getHeight() - scaledImage.getHeight()) / 2;

                g.drawImage(scaledImage, x, y, this);
            }
        }

        private void buildScaledImage() {
            if (image != null) {

                try {
                    scaledImage = resizeImage(image, this.getWidth(), this.getHeight());
                } catch (final IOException e) {
                    Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                    scaledImage = image;
                }
            }
        }

        private BufferedImage resizeImage(final BufferedImage originalImage, final int targetWidth, final int targetHeight) throws IOException {

            if (targetHeight == 0 || targetWidth == 0) {
                return originalImage;
            }

            float xRatio = (float) targetWidth / (float) originalImage.getWidth();
            float yRatio = (float) targetHeight / (float) originalImage.getHeight();

            float ratio = Math.min(xRatio, yRatio);

            int _width, _height;

            _width = (int) ((float) originalImage.getWidth() * ratio) - MARGIN * 2;
            _height = (int) ((float) originalImage.getHeight() * ratio) - MARGIN * 2;

            if (_width >= originalImage.getWidth() || _height >= originalImage.getHeight()) {
                Graphics2D g2 = null;

                try {
                    BufferedImage resizeImage = new BufferedImage(_width, _height, originalImage.getType());
                    g2 = resizeImage.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(originalImage, 0, 0, _width, _height, null);
                    g2.dispose();
                    return resizeImage;
                } finally {
                    if (g2 != null) {
                        g2.dispose();
                    }
                }
            } else {
                return GraphicsUtilities.createThumbnail(originalImage, _width, _height);
            }
        }

    }
}
