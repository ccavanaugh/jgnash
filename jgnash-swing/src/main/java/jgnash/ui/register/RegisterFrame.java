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
package jgnash.ui.register;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * A Dialog that displays a single account register. Size and position is remembered
 *
 * @author Craig Cavanaugh
 */
public final class RegisterFrame extends JFrame implements MessageListener {

    private static final EventListenerList listenerList = new EventListenerList();

    private static final List<RegisterFrame> dialogList = new ArrayList<>();

    private Account account = null;

    private AbstractRegisterPanel panel = null;

    private RegisterFrame(final Account account) {
        super();

        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));

        this.account = account;

        updateTitle();

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT);

        panel = RegisterFactory.createRegisterPanel(account);
        getContentPane().add(panel, BorderLayout.CENTER);

        pack(); // pack and layout

        setMinimumSize(getSize());

        // set a default size and position
        Dimension d = UIApplication.getFrame().getSize();
        setSize(new Dimension((int) (d.getWidth() * 0.7), (int) (d.getHeight() * 0.7)));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // remember size and position
        DialogUtils.addBoundsListener(this, Integer.toString(account.hashCode()));

        dialogList.add(this);

        // restore panel layout after the dialogs previous position has been restored
        panel.restoreColumnLayout();

        // CTRL-F4 closes the window
        Action closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(RegisterFrame.this, WindowEvent.WINDOW_CLOSING));
            }
        };

        getRootPane().getActionMap().put("close", closeAction);

        getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK, false), "close");

        fireOpenEvent();
    }

    private RegisterFrame(final Account account, final Transaction t) {
        this(account);

        if (t != null) {
            setSelectedTransaction(t);
        }
    }

    @Override
    public void dispose() {
        dialogList.remove(this);
        fireCloseEvent();
        super.dispose();
    }

    public static void addRegisterListener(final RegisterListener l) {
        listenerList.add(RegisterListener.class, l);
    }

    private void fireOpenEvent() {
        fireEvent(RegisterEvent.Action.OPEN);
    }

    private void fireCloseEvent() {
        fireEvent(RegisterEvent.Action.CLOSE);
    }

    private void fireEvent(final RegisterEvent.Action action) {
        RegisterEvent e = null;

        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RegisterListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new RegisterEvent(this, action);
                }
                ((RegisterListener) listeners[i + 1]).registerEvent(e);
            }
        }
    }

    private void updateTitle() {
        setTitle("jGnash - " + account.getPathName());
    }

    @Override
    public String toString() {
        return account.getName();
    }

    void setSelectedTransaction(final Transaction t) {
        panel.setSelectedTransaction(t);
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.ACCOUNT).equals(account)) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case ACCOUNT_MODIFY: // name change, moved, etc.
                            updateTitle();
                            break;
                        case ACCOUNT_REMOVE: // we were deleted!
                            dispatchEvent(new WindowEvent(RegisterFrame.this, WindowEvent.WINDOW_CLOSING));
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void toFront() {
        for (Window w : Window.getOwnerlessWindows()) {
            if (w != this) {
                w.toBack();
            }
        }
        
        setAutoRequestFocus(true);
        super.toFront();
    }

    public static void showDialog(final Account account) {
        showDialog(account, null);
    }

    static void showDialog(final Account account, final Transaction t) {

        for (final RegisterFrame d : dialogList) {
            if (account.equals(d.account)) {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (t != null) {
                            d.setSelectedTransaction(t);
                        }

                        if (d.getExtendedState() == ICONIFIED) {
                            d.setExtendedState(NORMAL);
                        }
                                               
                        d.toFront();
                    }
                });
                return;
            }
        }

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new RegisterFrame(account, t).setVisible(true);
            }
        });
    }
}
