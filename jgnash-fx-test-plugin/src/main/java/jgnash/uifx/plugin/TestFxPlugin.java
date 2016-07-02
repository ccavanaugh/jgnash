package jgnash.uifx.plugin;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import jgnash.plugin.FxPlugin;

/**
 * Test plugin.
 *
 * @author Craig Cavanaugh
 */
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
    public void start() {
        System.out.println("Starting");
    }

    @Override
    public void stop() {
        System.out.println("Stopping");
    }
}
