package jgnash.engine.jpa;

import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.StoredObject;

import java.io.File;
import java.util.Collection;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaDataStore implements DataStore {
    @Override
    public void closeEngine() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Engine getClientEngine(String host, int port, String user, String password, String engineName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Engine getLocalEngine(String fileName, String engineName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFileExt() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFileName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRemote() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void saveAs(File file, Collection<StoredObject> objects) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
