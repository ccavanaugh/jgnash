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
package jgnash.ui.budget;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.ui.UIApplication;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.components.YesNoDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * BudgetManagerDialog is for creating and deleting budgets
 *
 * @author Craig Cavanaugh
 * @version $Id: CurrencyModifyDialog.java 2493 2011-01-02 16:08:57Z ccavanaugh $
 */
public final class BudgetManagerDialog extends JDialog implements ActionListener, MessageListener {

    private final Resource rb = Resource.get();

    private JButton closeButton;

    private JButton newAutoButton;

    private JButton newButton;

    private JButton duplicateButton;

    private JButton deleteButton;

    private JButton renameButton;

    private JList budgetList;

    public static void showDialog() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                BudgetManagerDialog d = new BudgetManagerDialog();
                DialogUtils.addBoundsListener(d);
                d.setVisible(true);
            }
        });
    }

    private BudgetManagerDialog() {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.BudgetManager"));
        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();
    }

    private void initComponents() {
        closeButton = new JButton(rb.getString("Button.Close"));
        deleteButton = new JButton(rb.getString("Button.Delete"));
        duplicateButton = new JButton(rb.getString("Button.Duplicate"));
        newAutoButton = new JButton(rb.getString("Button.NewHist"));
        newButton = new JButton(rb.getString("Button.NewEmpty"));
        renameButton = new JButton(rb.getString("Button.Rename"));

        budgetList = new JList();

        buildBudgetModel();

        closeButton.addActionListener(this);
        deleteButton.addActionListener(this);
        duplicateButton.addActionListener(this);
        newAutoButton.addActionListener(this);
        newButton.addActionListener(this);
        renameButton.addActionListener(this);

        MessageBus.getInstance().registerListener(this, MessageChannel.BUDGET);
    }

    private void layoutMainPanel() {
        initComponents();

        // build the button stack
        ButtonStackBuilder buttonStackBuilder = new ButtonStackBuilder();
        buttonStackBuilder.addButton(newAutoButton, newButton, duplicateButton, renameButton);
        buttonStackBuilder.addUnrelatedGap();
        buttonStackBuilder.addButton(deleteButton);

        FormLayout layout = new FormLayout("p:g, $lcgap, f:p", "f:max(35dlu;p):g, $ugap, p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.add(new JScrollPane(budgetList), cc.xy(1, 1));
        builder.add(buttonStackBuilder.getPanel(), cc.xy(3, 1));
        builder.add(ButtonBarFactory.buildCloseBar(closeButton), cc.xyw(1, 3, 3));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private void buildBudgetModel() {
        final SortedListModel<BudgetObject> model = new SortedListModel<BudgetObject>();

        Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (Budget budget : e.getBudgetList()) {
            model.addElement(new BudgetObject(budget));
        }
        
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                budgetList.setModel(model);
            }
        });
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (e.getSource() == newButton) {
                    createNewBudget();
                } else if (e.getSource() == deleteButton) {
                    deleteBudget();
                } else if (e.getSource() == duplicateButton) {
                    cloneBudget();
                } else if (e.getSource() == renameButton) {
                    renameBudget();
                } else if (e.getSource() == newAutoButton) {
                    createNewAutoBudget();
                }
            }
        });
    }

    private void renameBudget() {
        for (Object o : budgetList.getSelectedValues()) {
            RenameBudgetDialog.showDialog(((BudgetObject) o).getBudget(), BudgetManagerDialog.this);
        }
    }

    private void createNewAutoBudget() {
        BudgetWizardDialog.showDialog();
    }

    private void createNewBudget() {

        Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Budget newBudget = new Budget();

        String name = rb.getString("Word.NewBudget");

        int count = 2;

        while (true) {
            boolean nameIsUnique = true;

            for (Budget budget : e.getBudgetList()) {
                if (budget.getName().equals(name)) {
                    name = rb.getString("Word.NewBudget") + " " + count;
                    count++;
                    nameIsUnique = false;
                }
            }

            if (nameIsUnique) {
                break;
            }
        }

        newBudget.setName(name);
        newBudget.setDescription(rb.getString("Word.NewBudget"));

        e.addBudget(newBudget);
    }

    private void deleteBudget() {
        Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Object[] values = budgetList.getSelectedValues();

        if (values.length > 0) {

            String message = values.length == 1 ? rb.getString("Message.ConfirmBudgetDelete") : rb.getString("Message.ConfirmMultipleBudgetDelete");

            if (YesNoDialog.showYesNoDialog(UIApplication.getFrame(), new JLabel(rb.getString(message)), rb.getString("Title.Confirm"))) {
                for (Object value : values) {
                    e.removeBudget(((BudgetObject) value).getBudget());
                }
            }
        }
    }

    private void cloneBudget() {
        Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (Object value : budgetList.getSelectedValues()) {

            Budget newBudget;
            try {
                newBudget = (Budget) ((BudgetObject) value).getBudget().clone();
                e.addBudget(newBudget);
            } catch (CloneNotSupportedException e1) {
                Logger.getLogger(BudgetManagerDialog.class.getName()).log(Level.SEVERE, e1.toString(), e1);
            }
        }

    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getEvent() == ChannelEvent.BUDGET_ADD || event.getEvent() == ChannelEvent.BUDGET_REMOVE || event.getEvent() == ChannelEvent.BUDGET_UPDATE) {
            buildBudgetModel();
        }
    }

    private static final class BudgetObject implements Comparable<BudgetObject> {
        private Budget budget;

        public BudgetObject(final Budget budget) {
            this.setBudget(budget);
        }

        @Override
        public String toString() {
            return getBudget().getName();
        }

        @Override
        public int compareTo(final BudgetObject o) {
            return getBudget().compareTo(o.getBudget());
        }

        @Override
        public boolean equals(final Object o) {
            boolean equal = false;

            if (o instanceof BudgetObject) {
                equal = getBudget().equals(((BudgetObject) o).getBudget());
            }

            return equal;
        }

        public Budget getBudget() {
            return budget;
        }

        public void setBudget(final Budget budget) {
            this.budget = budget;
        }

        @Override
        public int hashCode() {
            return getBudget().hashCode();
        }
    }
}
