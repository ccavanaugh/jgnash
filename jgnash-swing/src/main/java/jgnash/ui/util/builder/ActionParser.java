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
package jgnash.ui.util.builder;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jgnash.ui.components.RollOverButton;
import jgnash.util.Resource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses an XML file and builds JMenuBar and JToolBar UI objects
 * <p/>
 * Ideas borrowed from: http://www.javadesktop.org/articles/actions/index.html
 *
 * @author Craig Cavanaugh
 */
public final class ActionParser extends DefaultHandler {

    // Elements in the action-set.dtd
    private final static String ACTION_ELEMENT = "action";

    private final static String ACTION_NODE_ELEMENT = "action-node";

    private final static String EMPTY_ELEMENT = "empty";

    private final static String ACCEL_ATTRIBUTE = "accel";

    private final static String DESC_ATTRIBUTE = "desc";

    private final static String ICON_ATTRIBUTE = "icon";

    private final static String ID_ATTRIBUTE = "id";

    public final static String IDREF_ATTRIBUTE = "idref";

    private final static String MNEMONIC_ATTRIBUTE = "mnemonic";

    private final static String NAME_ATTRIBUTE = "name";

    private final static String SMICON_ATTRIBUTE = "smicon";

    private final static String TYPE_ATTRIBUTE = "type";

    private final static String METHOD_ATTRIBUTE = "method";

    private final static String GROUP_ATTRIBUTE = "group";

    // Indexes into the ActionAttributes array.
    private final static int ACCEL_INDEX = 0;

    private final static int DESC_INDEX = 1;

    private final static int ICON_INDEX = 2;

    private final static int ID_INDEX = 3;

    private final static int MNEMONIC_INDEX = 4;

    private final static int NAME_INDEX = 5;

    private final static int SMICON_INDEX = 6;

    private final static int TYPE_INDEX = 7;

    private final static int METHOD_INDEX = 8;

    /**
     * A list of all defined ActionAttributes
     */
    private final ArrayList<ActionAttributes> actionList = new ArrayList<>();

    private final HashMap<String, ActionNode> actionTrees = new HashMap<>();

    private final HashMap<String, Action> actionMap = new HashMap<>();

    /**
     * A map of all generated JMenuItems
     */
    private final HashMap<String, JMenuItem> menuItemMap = new HashMap<>();

    private ActionNode currentNode = null;

    private final HashMap<String, ButtonGroup> buttonGroups = new HashMap<>();

    /**
     * Object to invoke methods on
     */
    private final Object target;

    private final Resource rb;

    private static final Logger log = Logger.getLogger(ActionParser.class.getName());

    public ActionParser(final Object target, final Resource rb) {
        this.target = target;
        this.rb = rb;
    }

    /**
     * Looks for classes that implement <code>javax.swing.Action</code> and are annotated with
     * <code>jgnash.ui.util.builder.Action</code> and pre-loads them into the action map
     *
     * @param packageName base package name to search
     */
    public void preLoadActions(final String packageName) {
        try {
            ArrayList<Class<?>> classes = ActionParser.getClasses(packageName);

            for (Class<?> aClass : classes) {

                Annotation annotation = aClass.getAnnotation(jgnash.ui.util.builder.Action.class);

                if (annotation instanceof jgnash.ui.util.builder.Action) {
                    jgnash.ui.util.builder.Action action = (jgnash.ui.util.builder.Action) annotation;
                    preLoadAction(action.value(), (Action) aClass.newInstance());
                }
            }

        } catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
            log.log(Level.SEVERE, e.toString(), e);
        }
    }

    /**
     * Adds the set of actions and action-lists from an action-set document into the ActionManager.
     *
     * @param stream InputStream containing an actionSet document
     */
    void loadFile(final InputStream stream) {
        SAXParserFactory parserfactory = SAXParserFactory.newInstance();
        parserfactory.setValidating(false);

        try {
            SAXParser parser = parserfactory.newSAXParser();
            parser.parse(stream, this);

            createActions(); // create reflective actions
        } catch (SAXException | ParserConfigurationException | IOException se) {
            log.severe(se.toString());
        }
    }

    public void loadFile(final String fileName) {        
        try (InputStream s = getClass().getResourceAsStream(fileName)) {
            loadFile(s);
        } catch (IOException e) {
           log.log(Level.SEVERE, null, e);
        }                
    }

    public void preLoadAction(final String id, final Action action) {
        action.putValue(ID_ATTRIBUTE, id); // add the id value
        actionMap.put(id, action);
    }

    public Action getAction(final String id) {
        return actionMap.get(id);
    }

    public JMenuItem getJMenuItem(final String idref) {
        Object o = menuItemMap.get(idref);
        if (o != null) {
            return (JMenuItem) o;
        }
        return null;
    }

    public JMenuBar createMenuBar(final String id) {
        JMenuBar menuBar = new JMenuBar();

        Object o = actionTrees.get(id);

        if (o != null) {
            ActionNode node = (ActionNode) o;
            for (int i = 0; i < node.size(); i++) {
                JMenuItem item = createMenuItem(node.getChildAt(i));

                if (item instanceof JMenu) {
                    menuBar.add((JMenu) item);
                } else {
                    log.log(Level.WARNING, "{0} invalid", item.toString());
                }
            }
        } else {
            log.log(Level.WARNING, "{0} not found", id);
        }
        return menuBar;
    }

    public JToolBar createToolBar(final String id) {
        JToolBar toolBar = new JToolBar();

        ActionNode node = actionTrees.get(id);

        for (int i = 0; i < node.size(); i++) {

            if (node.id == null && node.idref == null) {
                toolBar.addSeparator();
            } else {
                JButton button = createButton(node.getChildAt(i));

                if (button != null) {
                    toolBar.add(button);
                } else {
                    log.log(Level.WARNING, "Was not able to create button: {0}", id);
                }
            }
        }
        return toolBar;
    }

    private JButton createButton(final ActionNode node) {
        Action a = actionMap.get(node.idref);
        return new RollOverButton(a);
    }

    private JMenuItem createMenuItem(final ActionNode node) {

        JMenuItem menu;

        Action a = actionMap.get(node.idref);

        if (node.size() > 0) {
            menu = new JMenu(a);
            for (int i = 0; i < node.size(); i++) {
                ActionNode n = node.getChildAt(i);
                if (n.id == null && n.idref == null) { // detect a separator
                    ((JMenu) menu).addSeparator();
                } else {
                    JMenuItem item = createMenuItem(n);
                    menu.add(item);
                }
            }
        } else {
            if (node.type == null || node.type.isEmpty()) {
                menu = new JMenuItem(a);
            } else {
                switch (node.type) {
                    case "single":
                        menu = new JCheckBoxMenuItem(a);
                        break;
                    case "toggle":
                        menu = new JRadioButtonMenuItem(a);

                        if (node.group != null) { // create a group
                            ButtonGroup bGroup;
                            if (buttonGroups.get(node.group) != null) {
                                bGroup = buttonGroups.get(node.group);
                            } else {
                                bGroup = new ButtonGroup();
                                buttonGroups.put(node.group, bGroup);
                            }
                            bGroup.add(menu);
                        }
                        break;
                    default:
                        menu = new JMenuItem(a);
                        break;
                }
            }
        }
        menuItemMap.put(node.idref, menu);

        // store the idref in the JMenuItem
        menu.putClientProperty(IDREF_ATTRIBUTE, node.idref);

        return menu;
    }

    /**
     * Generates ReflectiveActions
     */
    void createActions() {

        for (ActionAttributes aa : actionList) {
            // look for a preloaded action
            Action action = actionMap.get(aa.getValue(ID_INDEX));

            if (action == null) { // create a new reflective action
                action = new ReflectiveAction(rb.getString(aa.getValue(NAME_INDEX)), aa.getValue(METHOD_INDEX), target);
            } else {
                if (aa.getValue(NAME_INDEX) != null) {
                    action.putValue(Action.NAME, rb.getString(aa.getValue(NAME_INDEX)));
                }
            }

            String accel = aa.getValue(ACCEL_INDEX);

            if (accel != null && accel.trim().length() > 0) {
                KeyStroke stroke = rb.getKeyStroke(accel);
                if (stroke != null) {
                    action.putValue(Action.ACCELERATOR_KEY, stroke);
                } else {
                    log.log(Level.WARNING, "Bad KeyStroke: {0}", accel);
                }
            }

            if (aa.getValue(ICON_INDEX) != null) {
                try {
                    Icon icon = Resource.getIcon(aa.getValue(ICON_INDEX));
                    action.putValue(Action.SMALL_ICON, icon);
                } catch (Exception e) {
                    log.log(Level.WARNING, aa.getValue(ICON_INDEX) + " not found", e);
                }
            }

            if (aa.getValue(DESC_INDEX) != null) {
                action.putValue(Action.SHORT_DESCRIPTION, rb.getString(aa.getValue(DESC_INDEX)));
            }

            // validate length of mnemonic before trying
            if (aa.getValue(MNEMONIC_INDEX) != null && rb.getString(aa.getValue(MNEMONIC_INDEX)).trim().length() == 1) {
                action.putValue(Action.MNEMONIC_KEY, (int) rb.getMnemonic(aa.getValue(MNEMONIC_INDEX)));
            }

            // load the ID into the action so it can be recalled later
            action.putValue(ID_ATTRIBUTE, aa.getValue(ID_INDEX));

            actionMap.put(aa.getValue(ID_INDEX), action); // load into the actionMAp
        }
    }

    /**
     * @param namespaceURI name space
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String namespaceURI, final String localName, final String qName) {
        if (ACTION_NODE_ELEMENT.equals(qName) && currentNode != null) {
            if (currentNode.getParent() != null) { // child node
                currentNode = currentNode.getParent();
            } else {
                assert currentNode.id != null;
                actionTrees.put(currentNode.id, currentNode);
                currentNode = null;
            }
        }
    }

    /**
     * @param namespaceURI name space
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts) {
        if (ACTION_ELEMENT.equals(qName)) {
            assert currentNode == null;
            ActionAttributes aa = new ActionAttributes(atts);
            actionList.add(aa); // add to global action list
        } else if (ACTION_NODE_ELEMENT.equals(qName)) {
            if (currentNode != null) {
                ActionNode n = new ActionNode(atts);
                currentNode.addChild(n);
                currentNode = n;
            } else {
                currentNode = new ActionNode(atts); // initialize parent
            }
        } else if (EMPTY_ELEMENT.equals(qName) && currentNode != null) { // separator
            currentNode.addChild(new ActionNode());
        }
    }

    /**
     * A simple class that holds the parsed XML Attributes.
     */
    static class ActionAttributes {

        private final String[] array;

        public ActionAttributes(final Attributes attrs) {
            // Populate the array with the objects that map to the attributes
            array = new String[9];

            array[ACCEL_INDEX] = attrs.getValue(ACCEL_ATTRIBUTE);
            array[DESC_INDEX] = attrs.getValue(DESC_ATTRIBUTE);
            array[ICON_INDEX] = attrs.getValue(ICON_ATTRIBUTE);
            array[ID_INDEX] = attrs.getValue(ID_ATTRIBUTE);
            array[MNEMONIC_INDEX] = attrs.getValue(MNEMONIC_ATTRIBUTE);
            array[NAME_INDEX] = attrs.getValue(NAME_ATTRIBUTE);
            array[SMICON_INDEX] = attrs.getValue(SMICON_ATTRIBUTE);
            array[TYPE_INDEX] = attrs.getValue(TYPE_ATTRIBUTE);
            array[METHOD_INDEX] = attrs.getValue(METHOD_ATTRIBUTE);
        }

        /**
         * Retrieves the Attribute value.
         *
         * @param index one of ActionManager.._INDEX
         * @return value
         */
        public String getValue(int index) {
            return array[index];
        }
    }

    static class ActionNode {

        String idref;

        String id;

        String group;

        String type;

        ArrayList<ActionNode> children = null;

        ActionNode parent = null; // make it easy to walk back up the tree

        public ActionNode() {
        } // an empty node... use as a separator

        public ActionNode(final Attributes attrs) {
            idref = attrs.getValue(IDREF_ATTRIBUTE); // reference to action id
            id = attrs.getValue(ID_ATTRIBUTE); // can be null if not root node
            group = attrs.getValue(GROUP_ATTRIBUTE); // may be null
            type = attrs.getValue(TYPE_ATTRIBUTE); // type attribute
        }

        public void addChild(final ActionNode child) {
            if (children == null) {
                children = new ArrayList<>();
            }
            child.parent = this;
            children.add(child);
        }

        public int size() {
            if (children == null) {
                return 0;
            }
            return children.size();
        }

        public ActionNode getChildAt(final int index) {
            return children.get(index);
        }

        public ActionNode getParent() {
            return parent;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(id).append(": ").append(idref);
            if (size() > 0) {
                int size = size();
                for (int i = 0; i < size; i++) {
                    b.append('\n');
                    b.append(getChildAt(i).toString());
                }
            }
            return b.toString();
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and sub-packages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException exception
     * @throws IOException            exception
     */
    private static ArrayList<Class<?>> getClasses(final String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        assert classLoader != null;

        String path = packageName.replace('.', '/');

        Enumeration<URL> resources = classLoader.getResources(path);

        List<File> dirs = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            String fileName = resource.getFile();
            String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
            dirs.add(new File(fileNameDecoded));
        }
        ArrayList<Class<?>> classes = new ArrayList<>();

        for (File directory : dirs) {
            classes.addAll(findActionClasses(directory, packageName));
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirectories.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException error
     */
    private static List<Class<?>> findActionClasses(final File directory, final String packageName) throws ClassNotFoundException {

        List<Class<?>> classes = new ArrayList<>();

        if (directory.getPath().contains(".jar!")) { // loading from the jar distribution
            try {
                int index = directory.getPath().indexOf(".jar!");

                URL url = new URL("jar:" + directory.getPath().substring(0, index).replace('/', File.separatorChar) + ".jar!/");

                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jar = conn.getJarFile();

                Enumeration<JarEntry> entries = jar.entries();

                String path = packageName.replace('.', '/');

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    String name = entry.getName();

                    if (name.startsWith(path) && name.endsWith(".class") && !name.contains("$")) {
                        Class<?> clazz = Class.forName(name.substring(0, name.length() - 6).replace('/', '.'));

                        if (Action.class.isAssignableFrom(clazz)) {
                            classes.add(Class.forName(name.substring(0, name.length() - 6).replace('/', '.')));
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        } else if (!directory.exists()) {
            log.info("Was not a directory");
        } else { // loading through an IDE or jar file has been expanded
            File[] files = directory.listFiles();

            for (File file : files) {

                final String fileName = file.getName();

                if (file.isDirectory()) {
                    assert !file.getName().contains(".");

                    classes.addAll(findActionClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class") && !fileName.contains("$")) {

                    Class<?> clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));

                    if (Action.class.isAssignableFrom(clazz)) {
                        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                    }
                }
            }
        }

        return classes;
    }

    /**
     * AbstractAction that uses reflection to implement actionPerformed
     */
    static class ReflectiveAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        private transient Method method;

        private transient Object clazz;

        public ReflectiveAction(final String name, final String methodName, final Object clazz) {
            super(name);

            if (methodName != null) {
                this.clazz = clazz;
                try {
                    method = clazz.getClass().getMethod(methodName, (Class[]) null);
                } catch (NoSuchMethodException nsme) {
                    log.log(Level.WARNING, "No such method: {0}", nsme.getLocalizedMessage());
                    setEnabled(false);
                }
            }
        }

        /**
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed(final ActionEvent ae) {
            if (method != null) {
                try {
                    method.invoke(clazz, (Object[]) null);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }
}
