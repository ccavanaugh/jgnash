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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ExchangeRate;
import jgnash.engine.Transaction;
import jgnash.engine.budget.Budget;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.recurring.Reminder;
import jgnash.net.ConnectionFactory;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.CharArrayWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Remote message bus client
 *
 * @author Craig Cavanaugh
 */
class MessageBusRemoteClient {

    private String host = "localhost";

    private int port = 0;

    private static final Logger logger = Logger.getLogger(MessageBusRemoteClient.class.getName());

    private IoSession session;

    private final XStream xstream;

    private String dataBasePath;

    private DataStoreType dataBaseType;

    private EncryptionFilter filter = null;

    static {
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
    }

    public MessageBusRemoteClient(final String host, final int port) {
        this.host = host;
        this.port = port;

        xstream = new XStream(new StaxDriver());
        xstream.alias("Message", Message.class);
        xstream.alias("MessageProperty", MessageProperty.class);
    }

    public String getDataBasePath() {
        return dataBasePath;
    }

    public DataStoreType getDataStoreType() {
        return dataBaseType;
    }

    private static int getConnectionTimeout() {
        return ConnectionFactory.getConnectionTimeout();
    }

    public boolean connectToServer(final char[] password) {
        boolean result = false;

        boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty("ssl"));

        // If a user and password has been specified, enable an encryption filter
        if (useSSL &&  password != null && password.length > 0) {
            filter = new EncryptionFilter(password);
        }

        SocketConnector connector = new NioSocketConnector();

        connector.setConnectTimeoutMillis(getConnectionTimeout() * 1000L);
        connector.setHandler(new ClientSessionHandler());
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        try {
            ConnectFuture future = connector.connect(new InetSocketAddress(host, port));

            future.awaitUninterruptibly();
            session = future.getSession();
            result = true;
            logger.info("Connected to remote message server");
        } catch (RuntimeIoException e) {
            logger.log(Level.SEVERE, "Failed to connect to remote message bus", e);

            if (session != null) {
                session.close(true);
            }
        }

        return result;
    }

    public void disconnectFromServer() {
        session.close(true);
    }

    public synchronized void sendRemoteMessage(final Message message) {
        CharArrayWriter writer = new CharArrayWriter();
        xstream.marshal(message, new CompactWriter(writer));

        if (filter != null) {
            session.write(filter.encrypt(writer.toString()));
        } else {
            session.write(writer.toString());
        }

        logger.log(Level.INFO, "sent: {0}", writer.toString());
    }

    public void sendRemoteShutdownRequest() {
        session.write(JpaNetworkServer.STOP_SERVER_MESSAGE);
    }

    private class ClientSessionHandler extends IoHandlerAdapter {

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        /**
         * Time in milliseconds to force an update latency to ensure client container is current before processing the
         * message
         */
        private static final int FORCED_LATENCY = 2000;

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionOpened(IoSession s) {
        }

        private String decrypt(final Object object) {
            String plainMessage;

            if (filter != null) {
                plainMessage = filter.decrypt(object.toString());
            } else {
                plainMessage = object.toString();
            }

            return plainMessage;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void messageReceived(final IoSession s, final Object object) {
            String plainMessage = decrypt(object);

            logger.log(Level.INFO, "messageReceived: {0}", plainMessage);

            if (plainMessage.startsWith("<Message")) {
                final Message message = (Message) xstream.fromXML(plainMessage);

                // ignore our own messages
                if (!EngineFactory.getEngine(EngineFactory.DEFAULT).getUuid().equals(message.getSource())) {

                    // force latency and process after a fixed delay
                    scheduler.schedule(new Runnable() {

                        @Override
                        public void run() {
                            processRemoteMessage(message);
                        }
                    }, FORCED_LATENCY, TimeUnit.MILLISECONDS);
                }
            } else if (plainMessage.startsWith(MessageBusRemoteServer.PATH_PREFIX)) {
                dataBasePath = plainMessage.substring(MessageBusRemoteServer.PATH_PREFIX.length());
                logger.log(Level.INFO, "Remote data path is: {0}", dataBasePath);
            } else if (plainMessage.startsWith(MessageBusRemoteServer.DATA_STORE_TYPE_PREFIX)) {
                dataBaseType = DataStoreType.valueOf(plainMessage.substring(MessageBusRemoteServer.DATA_STORE_TYPE_PREFIX.length()));
                logger.log(Level.INFO, "Remote dataBaseType type is: {0}", dataBaseType.name());
            } else if (plainMessage.startsWith(EncryptionFilter.DECRYPTION_ERROR_TAG)) {    // decryption has failed, shut down the engine
                logger.log(Level.SEVERE, "Unable to decrypt the remote message");
            } else if (plainMessage.startsWith(JpaNetworkServer.STOP_SERVER_MESSAGE)) {
                logger.info("Server is shutting down");
                EngineFactory.closeEngine(EngineFactory.DEFAULT);
            } else {
                logger.log(Level.SEVERE, "Unknown message: {0}", plainMessage);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void exceptionCaught(final IoSession s, final Throwable cause) {
            logger.log(Level.SEVERE, null, cause);
            s.close(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionClosed(final IoSession s) throws Exception {
            s.close(true);
        }
    }

    /**
     * Takes a remote message and forces remote updates before sending the message to the MessageBus to notify UI
     * components of changes.
     *
     * @param message Message to process and send
     */
    private synchronized static void processRemoteMessage(final Message message) {
        logger.info("processing a remote message");

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (message.getChannel() == MessageChannel.ACCOUNT) {
            final Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
            switch (message.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_REMOVE:
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));
                    engine.refreshAccount(account.getParent());
                    break;
                case ACCOUNT_MODIFY:
                case ACCOUNT_SECURITY_ADD:
                case ACCOUNT_SECURITY_REMOVE:
                case ACCOUNT_VISIBILITY_CHANGE:
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.BUDGET) {
            final Budget budget = (Budget) message.getObject(MessageProperty.BUDGET);
            switch (message.getEvent()) {
                case BUDGET_ADD:
                case BUDGET_UPDATE:
                case BUDGET_REMOVE:
                case BUDGET_GOAL_UPDATE:
                    engine.refreshBudget(budget);
                    message.setObject(MessageProperty.BUDGET, engine.getBudgetByUuid(budget.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.COMMODITY) {
            switch (message.getEvent()) {
                case CURRENCY_MODIFY:
                    final CommodityNode currency = (CommodityNode) message.getObject(MessageProperty.COMMODITY);
                    engine.refreshCommodity(currency);
                    message.setObject(MessageProperty.COMMODITY, engine.getCurrencyNodeByUuid(currency.getUuid()));
                    break;
                case SECURITY_MODIFY:
                case SECURITY_HISTORY_ADD:
                case SECURITY_HISTORY_REMOVE:
                    final CommodityNode node = (CommodityNode) message.getObject(MessageProperty.COMMODITY);
                    engine.refreshCommodity(node);
                    message.setObject(MessageProperty.COMMODITY, engine.getSecurityNodeByUuid(node.getUuid()));
                    break;
                case EXCHANGE_RATE_ADD:
                case EXCHANGE_RATE_REMOVE:
                    final ExchangeRate rate = (ExchangeRate) message.getObject(MessageProperty.EXCHANGERATE);
                    engine.refreshExchangeRate(rate);
                    message.setObject(MessageProperty.EXCHANGERATE, engine.getExchangeRateByUuid(rate.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.REMINDER) {
            switch (message.getEvent()) {
                case REMINDER_ADD:
                case REMINDER_REMOVE:
                    final Reminder reminder = (Reminder) message.getObject(MessageProperty.REMINDER);
                    engine.refreshReminder(reminder);
                    message.setObject(MessageProperty.REMINDER, engine.getReminderByUuid(reminder.getUuid()));
                    break;
                default:
                    break;

            }
        }

        if (message.getChannel() == MessageChannel.TRANSACTION) {
            switch (message.getEvent()) {
                case TRANSACTION_ADD:
                case TRANSACTION_REMOVE:
                    final Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));

                    final Transaction transaction = (Transaction) message.getObject(MessageProperty.TRANSACTION);
                    engine.refreshTransaction(transaction);
                    message.setObject(MessageProperty.TRANSACTION, engine.getTransactionByUuid(transaction.getUuid()));
                    break;
                default:
                    break;
            }
        }

        /* Flag the message as remote */
        message.setRemote(true);

        logger.info("fire remote message");
        MessageBus.getInstance().fireEvent(message);
    }
}
