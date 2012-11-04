/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.plaf.theme;

import java.awt.Font;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * Metal theme that tries to look like the CDE/Motif look and feel
 *
 * @author Craig Cavanaugh
 *
 */
public class CDETheme extends DefaultMetalTheme {

    @Override
    public void addCustomEntriesToTable(UIDefaults table) {
        table.put("Tree.background", darkBlue);
        table.put("Tree.textBackground", darkBlue);
        table.put("Tree.selectionBorderColor", redBorder);
        table.put("Tree.drawsFocusBorderAroundIcon", Boolean.TRUE);
        table.put("Tree.rightChildIndent", 13);
        table.put("Tree.leftChildIndent", 7);
        table.put("Tree.rowHeight", 18);
        table.put("Tree.hash", getBlack());
        table.put("Tree.selectionBackground", primary3);

        table.put("TabbedPane.light", getSecondary2());
        table.put("TabbedPane.focus", redBorder);
        table.put("TabbedPane.background", darkBlue);

        table.put("ComboBox.buttonDarkShadow", getBlack());
        table.put("ComboBox.selectionBackground", getBlack());
        table.put("ComboBox.selectionForeground", getPrimary3());
    }

    // primary colors
    private final ColorUIResource primary1 = new ColorUIResource(32, 32, 32);

    private final ColorUIResource primary2 = new ColorUIResource(147, 151, 165);

    private final ColorUIResource primary3 = new ColorUIResource(255, 247, 233); // good

    // secondary colors
    private final ColorUIResource secondary1 = new ColorUIResource(102, 105, 115);

    private final ColorUIResource secondary2 = new ColorUIResource(220, 222, 229);

    private final ColorUIResource secondary3 = new ColorUIResource(174, 178, 195); // good

    // custom colors
    private final ColorUIResource darkBlue = new ColorUIResource(147, 151, 165);

    private final ColorUIResource redBorder = new ColorUIResource(178, 77, 122);

    private final ColorUIResource controlShadow = new ColorUIResource(99, 101, 111);

    // methods
    @Override
    public String getName() {
        return "CDE/Motif Metal Theme";
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return primary1;
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return primary2;
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return primary3;
    }

    @Override
    protected ColorUIResource getSecondary1() {
        return secondary1;
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return secondary2;
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return secondary3;
    }

    @Override
    public ColorUIResource getWindowBackground() {
        return secondary3;
    }

    @Override
    public ColorUIResource getControlShadow() {
        return controlShadow;
    }

    // font manipulation
    final FontUIResource menuText = new FontUIResource("Dialog", Font.PLAIN, 12);

    @Override
    public FontUIResource getMenuTextFont() {
        return menuText;
    }

    @Override
    public FontUIResource getControlTextFont() {
        return menuText;
    }
    /*
    Tree.font = javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
    Tree.selectionBackground = javax.swing.plaf.ColorUIResource[r=255,g=247,b=233]
    Tree.ancestorInputMap = javax.swing.plaf.InputMapUIResource@4b82d2
    Tree.iconShadow = javax.swing.plaf.ColorUIResource[r=99,g=101,b=111]
    Tree.selectionBorderColor = javax.swing.plaf.ColorUIResource[r=178,g=77,b=122]
    Tree.iconForeground = javax.swing.plaf.ColorUIResource[r=99,g=101,b=111]
    Tree.collapsedIcon = com.sun.java.swing.plaf.motif.MotifTreeUI$MotifCollapsedIcon@1541147
    Tree.scrollsOnExpand = true
    Tree.selectionForeground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    Tree.background = javax.swing.plaf.ColorUIResource[r=147,g=151,b=165]
    Tree.editorBorderSelectionColor = javax.swing.plaf.ColorUIResource[r=178,g=77,b=122]
    Tree.changeSelectionWithFocus = true
    Tree.editorBorder = com.sun.java.swing.plaf.motif.MotifBorders$FocusBorder@1867df9
    Tree.leftChildIndent = 7
    Tree.openIcon = javax.swing.plaf.IconUIResource@107108e
    Tree.rowHeight = 18
    Tree.focusInputMap = javax.swing.plaf.InputMapUIResource@cfe049
    TreeUI = com.sun.java.swing.plaf.motif.MotifTreeUI
    Tree.textForeground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    Tree.expandedIcon = com.sun.java.swing.plaf.motif.MotifTreeUI$MotifExpandedIcon@15bfdbd
    Tree.iconHighlight = javax.swing.plaf.ColorUIResource[r=220,g=222,b=229]
    Tree.leafIcon = javax.swing.plaf.IconUIResource@6f8b2b
    Tree.focusInputMap.RightToLeft = javax.swing.plaf.InputMapUIResource@119e583
    Tree.hash = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    Tree.textBackground = javax.swing.plaf.ColorUIResource[r=147,g=151,b=165]
    Tree.closedIcon = javax.swing.plaf.IconUIResource@12b19c5
    Tree.rightChildIndent = 13
    Tree.drawsFocusBorderAroundIcon = true
    Tree.iconBackground = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    Tree.foreground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
     */
    /*
    TabbedPaneUI = com.sun.java.swing.plaf.motif.MotifTabbedPaneUI
    TabbedPane.light = javax.swing.plaf.ColorUIResource[r=220,g=222,b=229]
    TabbedPane.background = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    TabbedPane.tabAreaInsets = javax.swing.plaf.InsetsUIResource[top=4,left=2,bottom=0,right=8]
    TabbedPane.tabInsets = javax.swing.plaf.InsetsUIResource[top=3,left=4,bottom=3,right=4]
    TabbedPane.foreground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    TabbedPane.darkShadow = javax.swing.plaf.ColorUIResource[r=99,g=101,b=111]
    TabbedPane.ancestorInputMap = javax.swing.plaf.InputMapUIResource@1541147
    TabbedPane.focusInputMap = javax.swing.plaf.InputMapUIResource@1867df9
    TabbedPane.actionMap = javax.swing.plaf.ActionMapUIResource@107108e
    TabbedPane.unselectedTabShadow = javax.swing.plaf.ColorUIResource[r=102,g=105,b=115]
    TabbedPane.unselectedTabBackground = javax.swing.plaf.ColorUIResource[r=147,g=151,b=165]
    TabbedPane.shadow = javax.swing.plaf.ColorUIResource[r=99,g=101,b=111]
    TabbedPane.highlight = javax.swing.plaf.ColorUIResource[r=220,g=222,b=229]
    TabbedPane.contentBorderInsets = javax.swing.plaf.InsetsUIResource[top=2,left=2,bottom=2,right=2]
    TabbedPane.textIconGap = 4
    TabbedPane.unselectedTabForeground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    TabbedPane.font = javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
    TabbedPane.unselectedTabHighlight = javax.swing.plaf.ColorUIResource[r=210,g=215,b=235]
    TabbedPane.selectedTabPadInsets = javax.swing.plaf.InsetsUIResource[top=3,left=0,bottom=1,right=0]
    TabbedPane.tabRunOverlay = 2
    TabbedPane.focus = javax.swing.plaf.ColorUIResource[r=178,g=77,b=122]
     */
    /*
    ComboBox.disabledBackground = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    ComboBox.foreground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    ComboBox.controlForeground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    ComboBox.ancestorInputMap = javax.swing.plaf.InputMapUIResource@1541147
    ComboBox.selectionBackground = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    ComboBox.selectionForeground = javax.swing.plaf.ColorUIResource[r=255,g=247,b=233]
    ComboBox.buttonShadow = javax.swing.plaf.ColorUIResource[r=99,g=101,b=111]
    ComboBoxUI = com.sun.java.swing.plaf.motif.MotifComboBoxUI
    ComboBox.buttonBackground = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    ComboBox.control = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    ComboBox.buttonHighlight = javax.swing.plaf.ColorUIResource[r=220,g=222,b=229]
    ComboBox.disabledForeground = javax.swing.plaf.ColorUIResource[r=128,g=128,b=128]
    ComboBox.buttonDarkShadow = javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
    ComboBox.border = javax.swing.plaf.BorderUIResource$CompoundBorderUIResource@15bfdbd
    ComboBox.background = javax.swing.plaf.ColorUIResource[r=174,g=178,b=195]
    ComboBox.font = javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12]
     */
}
