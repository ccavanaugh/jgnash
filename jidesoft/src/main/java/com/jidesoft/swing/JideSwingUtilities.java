/*
 * @(#)JideSwingUtilities.java
 *
 * Copyright 2002 JIDE Software. All rights reserved.
 */
package com.jidesoft.swing;


import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A utilities class for Swing.
 */
class JideSwingUtilities implements SwingConstants {

    private static ChangeListener _viewportSyncListener;

    private static ChangeListener getViewportSynchronizationChangeListener() {
        if (_viewportSyncListener == null) {
            _viewportSyncListener = new ViewportSynchronizationChangeListener();
        }
        return _viewportSyncListener;
    }

    private static class ViewportSynchronizationChangeListener implements ChangeListener {

        @SuppressWarnings("rawtypes")
        @Override
        public void stateChanged(ChangeEvent e) {
            if (!(e.getSource() instanceof JViewport)) {
                return;
            }

            JViewport masterViewport = (JViewport) e.getSource();
            Object property = masterViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT);

            if (!(property instanceof Map)) {
                return;
            }

            Dimension size = masterViewport.getSize();
            if (size.width == 0 || size.height == 0) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<JViewport, Integer> slaveViewportMap = (Map) property;

            Map<JViewport, Integer> allViewportToSync = new HashMap<>(slaveViewportMap);

            do {
                Map<JViewport, Integer> viewportToAdd = new HashMap<>();
                for (JViewport slaveViewport : allViewportToSync.keySet()) {
                    Object slaveProperty = slaveViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT);
                    if (!(slaveProperty instanceof Map)) {
                        continue;
                    }
                    int orientation = allViewportToSync.get(slaveViewport);

                    @SuppressWarnings("unchecked")
                    Map<JViewport, Integer> viewportMap = (Map) slaveProperty;

                    viewportMap.keySet().stream()
                            .filter(viewport -> viewport != masterViewport && !allViewportToSync.containsKey(viewport) && viewportMap.get(viewport) == orientation)
                            .forEach(viewport -> viewportToAdd.put(viewport, viewportMap.get(viewport)));
                }
                if (viewportToAdd.isEmpty()) {
                    break;
                }
                allViewportToSync.putAll(viewportToAdd);
            } while (true);

            for (JViewport slaveViewport : allViewportToSync.keySet()) {
                slaveViewport.removeChangeListener(getViewportSynchronizationChangeListener());
                int orientation = allViewportToSync.get(slaveViewport);
                if (orientation == SwingConstants.HORIZONTAL) {
                    Point v1 = masterViewport.getViewPosition();
                    Point v2 = slaveViewport.getViewPosition();
                    if (v1.x != v2.x) {
                        slaveViewport.setViewPosition(new Point(v1.x, v2.y));
                    }
                } else if (orientation == SwingConstants.VERTICAL) {
                    Point v1 = masterViewport.getViewPosition();
                    Point v2 = slaveViewport.getViewPosition();
                    if (v1.y != v2.y) {
                        slaveViewport.setViewPosition(new Point(v2.x, v1.y));
                    }
                }
                slaveViewport.addChangeListener(getViewportSynchronizationChangeListener());
            }
        }
    }

    /**
     * Synchronizes the two viewports. The view position changes in the master view, the slave view's view position will
     * change too. Generally speaking, if you want the two viewports to synchronize vertically, they should have the
     * same height. If horizontally, the same width.
     * <p>
     * It's OK if you call this method with the same master viewport and slave viewport duplicate times. It won't cause
     * multiple events fired.
     *
     * @param masterViewport the master viewport
     * @param slaveViewport  the slave viewport
     * @param orientation    the orientation. It could be either SwingConstants.HORIZONTAL or SwingConstants.VERTICAL.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void synchronizeView(final JViewport masterViewport, final JViewport slaveViewport, final int orientation) {
        if (masterViewport == null || slaveViewport == null) {
            return;
        }
        ChangeListener[] changeListeners = masterViewport.getChangeListeners();
        int i = 0;
        for (; i < changeListeners.length; i++) {
            if (changeListeners[i] == getViewportSynchronizationChangeListener()) {
                break;
            }
        }
        if (i >= changeListeners.length) {
            masterViewport.addChangeListener(getViewportSynchronizationChangeListener());
        }

        Object property = masterViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT);
        if (!(property instanceof Map)) {
            property = new HashMap<JViewport, Integer>();
        }
        Map<JViewport, Integer> slaveViewportMap = (Map) property;
        slaveViewportMap.put(slaveViewport, orientation);
        masterViewport.putClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT, slaveViewportMap);

        property = slaveViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_MASTER_VIEWPORT);
        if (!(property instanceof Map)) {
            property = new HashMap<JViewport, Integer>();
        }
        Map<JViewport, Integer> masterViewportMap = (Map) property;
        masterViewportMap.put(masterViewport, orientation);
        slaveViewport.putClientProperty(JideScrollPane.CLIENT_PROPERTY_MASTER_VIEWPORT, masterViewportMap);
    }

    /**
     * Un-synchronizes the two viewport.
     *
     * @param masterViewport the master viewport
     * @param slaveViewport  the slave viewport
     */
    @SuppressWarnings("rawtypes")
    public static void unsynchronizeView(final JViewport masterViewport, final JViewport slaveViewport) {
        if (masterViewport == null || slaveViewport == null) {
            return;
        }
        Object property = masterViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT);
        if (property instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<JViewport, Integer> slaveViewportMap = (Map) property;
            slaveViewportMap.remove(slaveViewport);
            if (slaveViewportMap.isEmpty()) {
                slaveViewportMap = null;
                masterViewport.removeChangeListener(getViewportSynchronizationChangeListener());
            }
            masterViewport.putClientProperty(JideScrollPane.CLIENT_PROPERTY_SLAVE_VIEWPORT, slaveViewportMap);
        }

        property = slaveViewport.getClientProperty(JideScrollPane.CLIENT_PROPERTY_MASTER_VIEWPORT);
        if (property instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<JViewport, Integer> masterViewportMap = (Map) property;
            masterViewportMap.remove(masterViewport);
            if (masterViewportMap.isEmpty()) {
                masterViewportMap = null;
            }
            slaveViewport.putClientProperty(JideScrollPane.CLIENT_PROPERTY_MASTER_VIEWPORT, masterViewportMap);
        }
    }
}
