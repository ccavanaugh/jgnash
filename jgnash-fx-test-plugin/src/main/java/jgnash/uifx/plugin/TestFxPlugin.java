package jgnash.uifx.plugin;

import javafx.scene.Node;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import jgnash.plugin.FxPlugin;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;

/**
 * Test plugin.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("unused")
public class TestFxPlugin implements FxPlugin {

    @Override
    public Node getOptionsNode() {
        return new Rectangle(100, 100, Color.LIGHTSTEELBLUE);
    }

    @Override
    public String getName() {
        return "Test Plugin";
    }

    @Override
    public void start(final PluginPlatform pluginPlatform) {
        System.out.println("Starting test plugin");

        if (pluginPlatform != PluginPlatform.Fx) {
            throw new RuntimeException("Invalid platform");
        }

        JavaFXUtils.runLater(() -> {

            //for API test.  Lookup allows plugins to find nodes within the application scene
            final Node node = MainView.getInstance().lookup("#fileMenu");
            if (node != null) {
                System.out.println("found the file menu");
                System.out.println(node.getClass().toString()); // Not really a node, but the skin for the node,
            }

            assert MainView.getInstance().lookup("#importMenu") != null;

            // Install a menu item
            final MenuBar menuBar = MainView.getInstance().getMenuBar();

            menuBar.getMenus().stream().filter(menu -> menu.getId().equals("fileMenu")).forEach(menu -> {
                System.out.println("found the file menu");
                menu.getItems().add(new MenuItem("Plugin Menu"));
            });
        });

    }

    @Override
    public void stop() {
        System.out.println("Stopping test plugin");
    }
}
