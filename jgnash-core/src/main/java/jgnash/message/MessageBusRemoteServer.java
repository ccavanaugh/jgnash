/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Remote message bus server
 * 
 * @author Craig Cavanaugh
 */
public class MessageBusRemoteServer {

    public static final String PATH_PREFIX = "<PATH>";

    private int port = 0;

    private IoAcceptor acceptor;

    private static final Logger logger = Logger.getLogger(MessageBusRemoteServer.class.getName());

    private final Set<IoSession> clientSessions = new HashSet<>();

    private final Set<LocalServerListener> listeners = new HashSet<>();

    private final ReadWriteLock rwl = new ReentrantReadWriteLock(true);

    private String dataBasePath = "";

    private EncryptionFilter filter;

    static {
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
    }

    public MessageBusRemoteServer(final int port) {      
        this.port = port;
    }

    public void startServer(final String dataBasePath, final String user, final char[] password) {
        this.dataBasePath = dataBasePath;

        boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty("ssl"));

        // If a user and password has been specified, enable an encryption filter
        if (useSSL && user != null && password != null && !user.isEmpty() && password.length > 0) {
            filter = new EncryptionFilter(user, password);
        }

        acceptor = new NioSocketAcceptor();

        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        acceptor.setHandler(new MessageHandler());

        try {
            acceptor.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        logger.info("MessageBusRemoteServer started");
    }

    public void stopServer() {

        rwl.writeLock().lock();

        try {
            for (IoSession client : clientSessions) {
                client.close(true);
            }
            //acceptor.unbindAll();
            acceptor.unbind();
            listeners.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public void addLocalListener(final LocalServerListener listener) {
        rwl.writeLock().lock();

        try {
            listeners.add(listener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /*public void removeLocalListener(final LocalServerListener listener) {
        rwl.writeLock().lock();

        try {
            listeners.remove(listener);
        } finally {
            rwl.writeLock().unlock();
        }
    }*/

    private class MessageHandler extends IoHandlerAdapter {

        @Override
        public void exceptionCaught(final IoSession session, final Throwable t) throws Exception {           
            logger.log(Level.SEVERE, null, t);

            rwl.writeLock().lock();

            try {
                session.close(true);
                clientSessions.remove(session);
            } finally {
                rwl.writeLock().unlock();
            }
        }

        @Override
        public void sessionIdle(final IoSession session, final IdleStatus status) {
            logger.info("Disconnecting the idle client.");

            // disconnect an idle client                                   
            rwl.writeLock().lock();

            try {
                session.close(true);
                clientSessions.remove(session);
            } finally {
                rwl.writeLock().unlock();
            }
        }

        /**
         * Utility method to encrypt a message
         * @param message message to encrypt
         * @return encrypted message
         */
        private String encrypt(final String message) {
            if (filter != null) {
                return filter.encrypt(message);
            }
            return message;
        }

        @Override
        public void messageReceived(final IoSession session, final Object message) throws Exception {

            String str;

            if (filter != null) {
                str = filter.decrypt(message.toString());
            } else {
                str = message.toString();
            }

            rwl.readLock().lock();

            try {
                for (IoSession client : clientSessions) {
                    if (client.isConnected()) {
                        //client.write(str);
                        client.write(encrypt(str));
                    }
                }

                // Local listeners do not receive encrypted messages
                for (LocalServerListener listener : listeners) {
                    listener.messagePosted(str);
                }
            } finally {
                rwl.readLock().unlock();
            }

            logger.log(Level.INFO, "Broadcast: {0}", str);
        }

        @Override
        public void sessionCreated(final IoSession session) throws Exception {

            rwl.writeLock().lock();

            try {
                clientSessions.add(session);
                logger.log(Level.INFO, "Remote connection from: {0}", session.toString());
                session.write(encrypt(PATH_PREFIX + dataBasePath));

                //session.write(PATH_PREFIX + dataBasePath);
            } finally {
                rwl.writeLock().unlock();
            }
        }
    }
}
