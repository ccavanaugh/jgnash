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

    private int port = 0;

    private IoAcceptor acceptor;

    private Logger logger;

    private Set<IoSession> clientSessions = new HashSet<>();

    private Set<LocalServerListener> listeners = new HashSet<>();

    private ReadWriteLock rwl = new ReentrantReadWriteLock(true);

    static {
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
    }

    public MessageBusRemoteServer(final int port) {
        logger = Logger.getLogger(MessageBusRemoteServer.class.getName());
        this.port = port;
    }

    public void startServer() {
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

    public void removeLocalListener(final LocalServerListener listener) {
        rwl.writeLock().lock();

        try {
            listeners.remove(listener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private class MessageHandler extends IoHandlerAdapter {

        @Override
        public void exceptionCaught(final IoSession session, final Throwable t) throws Exception {
            t.printStackTrace();

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

        @Override
        public void messageReceived(final IoSession session, final Object message) throws Exception {

            String str = message.toString();

            rwl.readLock().lock();

            try {
                for (IoSession client : clientSessions) {
                    if (client.isConnected()) {
                        client.write(str);
                    }
                }

                for (LocalServerListener listener : listeners) {
                    listener.messagePosted(str);
                }
            } finally {
                rwl.readLock().unlock();
            }

            logger.info("Broadcast: " + str);
        }

        @Override
        public void sessionCreated(final IoSession session) throws Exception {

            rwl.writeLock().lock();

            try {
                clientSessions.add(session);
            } finally {
                rwl.writeLock().unlock();
            }
        }
    }
}
