/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import jgnash.ui.plaf.NimbusUtils;
import jgnash.util.Resource;

import org.pushingpixels.lafwidget.animation.AnimationConfigurationManager;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.SubstanceSkin;
import org.pushingpixels.substance.api.skin.SkinInfo;

/**
 * Manages the look and feel and metal themes
 * 
 * @author Craig Cavanaugh
 */
@SuppressWarnings("restriction")
public class ThemeManager {

    private final Resource rb = Resource.get();
    private JMenu themesMenu = null;
    private final ArrayList<Object> themeList = new ArrayList<>();
    private ButtonGroup lfButtonGroup = new ButtonGroup();
    private static final String SUBSTANCE_ANIMATIONS = "substanceAnimations";
    private static final String NIMBUS_FONT_SIZE = "nimbusFontSize";
    private static final String STEEL = "javax.swing.plaf.metal.DefaultMetalTheme";
    private static final String OCEAN = "javax.swing.plaf.metal.OceanTheme";
    private static final String DEFAULT_THEME = OCEAN;
    private static final long animationDuration;
    private static final String LF = "lookandfeel3";
    private static final String THEME = "theme3";
    private static final String[] KNOWN = { "com.jgoodies.looks.plastic.PlasticLookAndFeel",
            "com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel",
            "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.sun.java.swing.plaf.mac.MacLookAndFeel",
            "com.nilo.plaf.nimrod.NimRODLookAndFeel", "de.muntjak.tinylookandfeel.TinyLookAndFeel" };

    static {
        animationDuration = AnimationConfigurationManager.getInstance().getTimelineDuration();
    }

    ThemeManager(final JFrame frame) {

        // this line needs to be implemented in order to make JWS work properly
        UIManager.getLookAndFeelDefaults().put("ClassLoader", frame.getClass().getClassLoader());

        try { // This could fail if JGoodies is not available
            com.jgoodies.looks.Options.setPopupDropShadowEnabled(true); // Enabled JGoodies drop shadow         
        } catch (Exception e) {
            Logger.getLogger(ThemeManager.class.getName()).log(Level.FINE, "JGoodies L&F was not found", e);
        }

        Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);

        setLookAndFeel(pref.get(LF, UIManager.getCrossPlatformLookAndFeelClassName()));
    }

    /**
     * Determine is the current look and feel is the OSX Aqua look and feel
     * 
     * @return true if the OSX Aqua look and feel is being used
     */
    public static boolean isLookAndFeelOSX() {
        return UIManager.getLookAndFeel().getName().startsWith("Mac");
    }

    public static boolean isLookAndFeelNimbus() {
        return UIManager.getLookAndFeel().getName().startsWith("Nimbus");
    }

    public static boolean isLookAndFeelJGoodies() {
        return UIManager.getLookAndFeel().getClass().getName().contains("jgoodies");
    }

    public static boolean isLookAndFeelMotif() {
        return UIManager.getLookAndFeel().getClass().getName().contains("motif");
    }

    public static boolean isLookAndFeelSubstance() {
        return UIManager.getLookAndFeel().getClass().getName().contains("Substance");
    }

    private static void setTheme(final String theme) {

        try {
            Class<?> themeClass = Class.forName(theme);
            MetalTheme themeObject = (MetalTheme) themeClass.newInstance();
            MetalLookAndFeel.setCurrentTheme(themeObject);
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Logger.getLogger(ThemeManager.class.getName()).log(Level.SEVERE, "Could not install theme: {0}\n{1}",
                    new Object[] { theme, e.toString() });
        }
    }

    private static void setLookAndFeel(final String lookAndFeel) {
        Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);
        String theme = pref.get(THEME, DEFAULT_THEME);

        try {
            Class<?> lafClass = Class.forName(lookAndFeel);
            Object lafInstance = lafClass.newInstance();

            if (lafInstance instanceof SubstanceSkin) {
                UIManager.put(SubstanceLookAndFeel.SHOW_EXTRA_WIDGETS, Boolean.TRUE);

                if (isSubstanceAnimationsEnabled()) {
                    AnimationConfigurationManager.getInstance().setTimelineDuration(animationDuration);
                } else {
                    AnimationConfigurationManager.getInstance().setTimelineDuration(0);
                }

                SubstanceLookAndFeel.setSkin(lookAndFeel);
            } else if (lafInstance instanceof NimbusLookAndFeel) {
                UIManager.setLookAndFeel((LookAndFeel) lafInstance);
                NimbusUtils.changeFontSize(getNimbusFontSize());
            } else if (lafInstance instanceof MetalLookAndFeel) {
                UIManager.setLookAndFeel((LookAndFeel) lafInstance);
                setTheme(theme);
            } else if (lafInstance instanceof LookAndFeel) {
                UIManager.setLookAndFeel((LookAndFeel) lafInstance);
            }
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            Logger.getLogger(ThemeManager.class.getName()).log(Level.WARNING, null, e);
        }
    }

    /**
     * Loads the menu with the available look and feels for the application
     * 
     * @return l and f menu
     */
    JMenu buildLookAndFeelMenu() {
        String activeLookAndFeelName = UIManager.getLookAndFeel().getName();

        // ButtonGroup buttonGroup = new ButtonGroup();
        JMenu lfMenu = new JMenu();

        lfMenu.setText(rb.getString("Menu.LookAndFeel.Name"));
        lfMenu.setMnemonic(rb.getMnemonic("Menu.LookAndFeel.Mnemonic"));

        lfMenu.add(buildSubstanceMenu());

        List<String> lookAndFeels = new ArrayList<>();

        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (isLookAndFeelAvailable(info.getClassName())) {
                lookAndFeels.add(info.getClassName());
            }
        }

        for (String lookAndFeel : KNOWN) {
            if (isLookAndFeelAvailable(lookAndFeel)) {
                lookAndFeels.add(lookAndFeel);
            }
        }

        Collections.sort(lookAndFeels);

        for (String lookAndFeel : lookAndFeels) {
            try {
                Class<?> lnfClass = Class.forName(lookAndFeel);
                LookAndFeel newLAF = (LookAndFeel) lnfClass.newInstance();

                JRadioButtonMenuItem button = new JRadioButtonMenuItem();

                button.setText(newLAF.getName());
                button.setActionCommand(lookAndFeel);
                button.setName(newLAF.getName());

                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);
                        pref.put(LF, e.getActionCommand());

                        restartUI();
                    }
                });

                lfButtonGroup.add(button);

                lfMenu.add(button);

                if (newLAF.getName().equals(activeLookAndFeelName)) {
                    button.setSelected(true);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Logger.getLogger(ThemeManager.class.getName()).log(Level.WARNING, null, e);
            }
        }

        return lfMenu;
    }

    private void buildThemeList() {
        /* Only works with JSE 5.0 or better, no harm on earlier JVM's */
        addTheme(OCEAN);

        addTheme(STEEL);

        // My themes
        addTheme("jgnash.ui.plaf.theme.CDETheme");
        addTheme("jgnash.ui.plaf.theme.TinyMetalTheme");

        // JGoodies themes
        addTheme("com.jgoodies.looks.plastic.theme.BrownSugar");
        addTheme("com.jgoodies.looks.plastic.theme.DarkStar");
        addTheme("com.jgoodies.looks.plastic.theme.DesertBlue");
        addTheme("com.jgoodies.looks.plastic.theme.DesertBluer");
        addTheme("com.jgoodies.looks.plastic.theme.DesertGreen");
        addTheme("com.jgoodies.looks.plastic.theme.DesertRed");
        addTheme("com.jgoodies.looks.plastic.theme.DesertYellow");
        addTheme("com.jgoodies.looks.plastic.theme.ExperienceBlue");
        addTheme("com.jgoodies.looks.plastic.theme.ExperienceGreen");
        addTheme("com.jgoodies.looks.plastic.theme.ExperienceRoyale");
        addTheme("com.jgoodies.looks.plastic.theme.LightGray");
        addTheme("com.jgoodies.looks.plastic.theme.Silver");
        addTheme("com.jgoodies.looks.plastic.theme.SkyBlue");
        addTheme("com.jgoodies.looks.plastic.theme.SkyBluer");
        addTheme("com.jgoodies.looks.plastic.theme.SkyGreen");
        addTheme("com.jgoodies.looks.plastic.theme.SkyKrupp");
        addTheme("com.jgoodies.looks.plastic.theme.SkyPink");
        addTheme("com.jgoodies.looks.plastic.theme.SkyRed");
        addTheme("com.jgoodies.looks.plastic.theme.SkyYellow");
    }

    @SuppressWarnings({ "rawtypes" })
    private void addTheme(final String theme) {
        try {
            Class c = Class.forName(theme);
            themeList.add(c.newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Logger.getLogger(ThemeManager.class.getName()).log(Level.WARNING, "Could not add theme: " + theme, e);
        }
    }

    JMenu buildThemeMenu() {

        Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);
        String currentTheme = pref.get(THEME, DEFAULT_THEME);

        themesMenu = new JMenu();
        themesMenu.setText(rb.getString("Menu.Themes.Name"));
        themesMenu.setMnemonic(rb.getMnemonic("Menu.Themes.Mnemonic"));

        ButtonGroup themeButtonGroup = new ButtonGroup();
        buildThemeList();

        JRadioButtonMenuItem button;

        for (Object aThemeList : themeList) {
            MetalTheme theme = (MetalTheme) aThemeList;
            button = new JRadioButtonMenuItem();
            button.setText(theme.getName());
            button.setActionCommand(theme.getClass().getName());
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);
                    pref.put(THEME, e.getActionCommand());

                    restartUI();
                }
            });

            themeButtonGroup.add(button);
            themesMenu.add(button);
            if (aThemeList.getClass().getName().equals(currentTheme)) {
                button.setSelected(true);
            }
        }
        refreshThemesState();
        return themesMenu;
    }

    JMenu buildSubstanceMenu() {
        LookAndFeel lf = UIManager.getLookAndFeel();

        JMenu substanceMenu = new JMenu(rb.getString("Menu.SubstanceThemes.Name"));

        for (SkinInfo info : SubstanceLookAndFeel.getAllSkins().values()) {
            JRadioButtonMenuItem button = new JRadioButtonMenuItem();
            button.setText(info.getDisplayName());
            button.setActionCommand(info.getClassName());

            // add the button to the global look and feel
            lfButtonGroup.add(button);

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Preferences pref = Preferences.userNodeForPackage(ThemeManager.class);
                    pref.put(LF, e.getActionCommand());

                    restartUI();
                }
            });

            substanceMenu.add(button);

            // select the button as the active L&F if it is the current skin
            if (lf instanceof SubstanceLookAndFeel) {
                if (SubstanceLookAndFeel.getCurrentSkin().getClass().getName().equals(info.getClassName())) {
                    button.setSelected(true);
                }
            }
        }

        return substanceMenu;
    }

    private void refreshThemesState() {
        if (themesMenu != null) {
            try {
                LookAndFeel laf = UIManager.getLookAndFeel();
                if (laf instanceof MetalLookAndFeel) {
                    themesMenu.setEnabled(true);
                } else {
                    themesMenu.setEnabled(false);
                }
            } catch (Exception e) {
                Logger.getLogger(ThemeManager.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * A utility function that layers on top of the LookAndFeel's
     * isSupportedLookAndFeel() method. Returns true if the LookAndFeel is
     * supported. Returns false if the LookAndFeel is not supported and/or if
     * there is any kind of error checking if the LookAndFeel is supported.
     * <p/>
     * The L&F menu will use this method to determine whether the various L&F
     * options should be active or inactive.
     * 
     * @param laf name of look and feel to search for
     * @return true if found
     */
    @SuppressWarnings("rawtypes")
    private static boolean isLookAndFeelAvailable(final String laf) {
        try {
            Class lnfClass = Class.forName(laf);
            LookAndFeel newLAF = (LookAndFeel) lnfClass.newInstance();

            return newLAF.isSupportedLookAndFeel();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) { // If ANYTHING bad happens, return false
            Logger.getLogger(ThemeManager.class.getName()).log(Level.FINEST, e.getLocalizedMessage(), e);
            return false;
        }
    }

    private void restartUI() {
        themeList.clear();
        themesMenu.removeAll();

        lfButtonGroup = null;
        themesMenu = null;

        UIApplication.restartUI();
    }

    public static void setSubstanceAnimationsEnabled(final boolean enabled) {
        Preferences p = Preferences.userNodeForPackage(ThemeManager.class);
        p.putBoolean(SUBSTANCE_ANIMATIONS, enabled);

        if (enabled) {
            AnimationConfigurationManager.getInstance().setTimelineDuration(animationDuration);
        } else {
            AnimationConfigurationManager.getInstance().setTimelineDuration(0);
        }
    }

    public static boolean isSubstanceAnimationsEnabled() {
        Preferences p = Preferences.userNodeForPackage(ThemeManager.class);
        return p.getBoolean(SUBSTANCE_ANIMATIONS, true);
    }

    public static void setNimbusFontSize(final int size) {
        Preferences p = Preferences.userNodeForPackage(ThemeManager.class);
        p.putInt(NIMBUS_FONT_SIZE, size);
    }

    public static int getNimbusFontSize() {
        Preferences p = Preferences.userNodeForPackage(ThemeManager.class);

        int preferredSize = p.getInt(NIMBUS_FONT_SIZE, 0);

        // if the returned value is zero, determine the default base font size
        // and save it
        if (preferredSize == 0) {

            LookAndFeel old = UIManager.getLookAndFeel();

            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
                preferredSize = NimbusUtils.getBaseFontSize();

                p.putInt(NIMBUS_FONT_SIZE, preferredSize);

                UIManager.setLookAndFeel(old);
            } catch (Exception e) {
                Logger.getLogger(ThemeManager.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        return preferredSize;
    }
}
