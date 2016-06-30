package jgnash.uifx.plugin;

import javafx.scene.Node;

import jgnash.plugin.FxPlugin;

/**
 * Test plugin.
 *
 * @author Craig Cavanaugh
 */
public class TestFxPlugin implements FxPlugin {

    @Override
    public Node getOptionsNode() {
        return null;
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
