/*
 * @(#)${NAME}.java
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import javax.swing.ScrollPaneConstants;

/**
 * Constants used with the JideScrollPane component.
 */
interface JideScrollPaneConstants extends ScrollPaneConstants {
    /**
     * Identifies the area along the left side of the viewport between the
     * upper right corner and the lower right corner.
     */
    String ROW_FOOTER = "ROW_FOOTER";
    /**
     * Identifies the area at the bottom where the viewport is between the
     * lower left corner and the lower right corner.
     */
    String COLUMN_FOOTER = "COLUMN_FOOTER";

    String HORIZONTAL_LEFT = "HORIZONTAL_LEFT";      //NOI18N
    String HORIZONTAL_RIGHT = "HORIZONTAL_RIGHT";    //NOI18N
    String HORIZONTAL_LEADING = "HORIZONTAL_LEADING";      //NOI18N
    String HORIZONTAL_TRAILING = "HORIZONTAL_TRAILING";      //NOI18N
    String VERTICAL_TOP = "VERTICAL_TOP";            //NOI18N
    String VERTICAL_BOTTOM = "VERTICAL_BOTTOM";      //NOI18N
    String SUB_UPPER_LEFT  = "SUB_UPPER_LEFT";       //NOI18N
    String SUB_UPPER_RIGHT = "SUB_UPPER_RIGHT";      //NOI18N
}
