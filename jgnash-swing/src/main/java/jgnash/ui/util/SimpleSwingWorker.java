package jgnash.ui.util;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 * A simple wrapper around SwingWorker to expose exceptions on a null return SwingWorker implementation.
 * 
 * 
 * @author Jonathan Giles
 * @version $Id: SimpleSwingWorker.java 2443 2010-11-27 23:31:19Z ccavanaugh $
 */
public abstract class SimpleSwingWorker {

    private final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

        @Override
        protected Void doInBackground() throws Exception {
            SimpleSwingWorker.this.doInBackground();
            return null;
        }

        @Override
        protected void done() {
            // call get to make sure any exceptions
            // thrown during doInBackground() are
            // thrown again
            try {
                get();
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (final ExecutionException ex) {
                throw new RuntimeException(ex.getCause());
            }
            SimpleSwingWorker.this.done();
        }
    };

    public SimpleSwingWorker() {
    }

    protected abstract Void doInBackground() throws Exception;

    protected abstract void done();

    public void execute() {
        worker.execute();
    }
}
