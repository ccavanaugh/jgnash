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
package jgnash.ui.wizards.file;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.text.StyledEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import jgnash.engine.Account;
import jgnash.engine.AccountTreeXMLFactory;
import jgnash.engine.RootAccount;
import jgnash.ui.components.CheckListCellRenderer;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.ui.util.ToggleSelectionModel;
import jgnash.util.ClassPathUtils;
import jgnash.util.Resource;

/**
 * @author Craig Cavanaugh
 * @version $Id: NewFileFour.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class NewFileFour extends JPanel implements WizardPage {

    private final Resource rb = Resource.get();

    private static final String ROOTPATH = "/jgnash/resource/account";

    private JList accountList;

    private JTree accountTree;

    private JEditorPane helpPane;

    /**
     * Creates new form NewFileFour
     */
    public NewFileFour() {
        layoutMainPanel();

        DefaultListModel model = new DefaultListModel();

        for (RootAccount account : getLocalizedAccountSet()) {
            model.addElement(account);
        }

        accountList.setModel(model);
        accountList.setSelectionModel(new ToggleSelectionModel());
        accountList.setCellRenderer(new CheckListCellRenderer(accountList.getCellRenderer()));

    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("min(85dlu;d), 8dlu, min(65dlu;d):g(1.0)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.ChooseAccounts"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(helpPane, 3);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("f:50dlu:g"));
        builder.append(new JScrollPane(accountList), new JScrollPane(accountTree));
    }

    private void initComponents() {
        accountList = new JList();

        accountList.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                accountListMouseClicked(evt);
            }
        });

        accountTree = new JTree();
        accountTree.setModel(null);

        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("NewFileFour.txt"));
    }

    private void accountListMouseClicked(final MouseEvent evt) {
        int index = accountList.locationToIndex(evt.getPoint());

        if (index > -1) {
            int[] indices = accountList.getSelectedIndices();

            if (Arrays.binarySearch(indices, index) > -1) {
                RootAccount root = (RootAccount) accountList.getModel().getElementAt(index);
                accountTree.setModel(new AccountModel(root));
                expandTree();
                return;
            }
        }

        Object o = accountList.getSelectedValue();
        if (o != null) {
            accountTree.setModel(new AccountModel((RootAccount) o));
            expandTree();
        } else {
            accountTree.setModel(null);
        }

    }

    private void expandTree() {
        // expand the tree so that all nodes are visible
        for (int i = 0; i < accountTree.getRowCount(); i++) {
            accountTree.expandRow(i);
        }
    }

    /**
     * toString must return a valid description for this page that will appear in the task list of the WizardDialog
     * 
     * @return Title of this page
     */
    @Override
    public String toString() {
        return "4. " + rb.getString("Title.ChooseAccounts");
    }

    @Override
    public boolean isPageValid() {
        return true;
    }

    @Override
    public void getSettings(final Map<Enum<?>, Object> map) {
    }

    @Override
    public void putSettings(final Map<Enum<?>, Object> map) {
        List<RootAccount> accounts = new ArrayList<RootAccount>();

        for (Object o : accountList.getSelectedValues()) {
            accounts.add((RootAccount) o);

        }
        map.put(NewFileDialog.Settings.ACCOUNTSET, accounts);
    }

    private static Collection<RootAccount> getLocalizedAccountSet() {
        List<RootAccount> files = new ArrayList<RootAccount>();

        for (String string : getAccountSetList()) {
            InputStream stream = Object.class.getResourceAsStream(string);

            if (stream != null) {
                RootAccount account = AccountTreeXMLFactory.loadAccountTree(stream);
                files.add(account);
            }
        }

        return files;
    }

    private static List<String> getAccountSetList() {
        String path = ClassPathUtils.getLocalizedPath(ROOTPATH);

        List<String> set = new ArrayList<String>();

        if (path != null) {

            BufferedReader r = null;

            try {
                InputStream stream = Object.class.getResourceAsStream(path + "/set.txt");

                if (stream != null) {
                    r = new BufferedReader(new InputStreamReader(stream));

                    String line = r.readLine();

                    while (line != null) {
                        set.add(path + "/" + line);
                        line = r.readLine();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(NewFileFour.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (r != null) {
                        r.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(NewFileFour.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return set;
    }

    private static final class AccountModel extends DefaultTreeModel {

        private static final long serialVersionUID = 754534776718585809L;

        public AccountModel(final RootAccount root) {
            super(null);
            loadAccountTree(root);
        }

        void loadAccountTree(final Account account) {

            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(account);

            setRoot(rootNode);
            loadChildren(rootNode);
            nodeStructureChanged(rootNode);
        }

        private void loadChildren(final DefaultMutableTreeNode parentNode) {
            Account parent = (Account) parentNode.getUserObject();

            for (Account child : parent.getChildren()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                insertNodeInto(childNode, parentNode, parentNode.getChildCount());
                if (child.getChildCount() > 0) {
                    loadChildren(childNode);
                }
            }
        }
    }
}
