/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.engine.message;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.io.CharArrayWriter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.Config;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ExchangeRate;
import jgnash.engine.Transaction;
import jgnash.engine.budget.Budget;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.recurring.Reminder;
import jgnash.net.ConnectionFactory;
import jgnash.util.EncryptionManager;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import jgnash.util.LogUtil;

/**
 * Message bus client for remote connections.
 *
 * @author Craig Cavanaugh
 */
class MessageBusClient {
    private final String host;

    private final int port;

    private static final Logger logger = Logger.getLogger(MessageBusClient.class.getName());

    private final XStream xstream;

    private String dataBasePath;

    private DataStoreType dataBaseType;

    private EncryptionManager encryptionManager = null;

    private NioEventLoopGroup eventLoopGroup;

    private Channel channel;

    private final String name;

    private final ReentrantLock channelLock = new ReentrantLock();

    static {
        logger.setLevel(Level.INFO);
    }

    MessageBusClient(final String host, final int port, final String name) {
        this.host = host;
        this.port = port;
        this.name = name;

        xstream = XStreamFactory.getInstance();
    }

    String getDataBasePath() {
        return dataBasePath;
    }

    DataStoreType getDataStoreType() {
        return dataBaseType;
    }

    private static int getConnectionTimeout() {
        return ConnectionFactory.getConnectionTimeout();
    }

    boolean connectToServer(final char[] password) {
        boolean result = false;

        // If a password has been specified, create an EncryptionManager
        if (password != null && password.length > 0) {
            encryptionManager = new EncryptionManager(password);
        }

        eventLoopGroup = new NioEventLoopGroup();

        final Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new MessageBusClientInitializer())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectionTimeout() * 1000)
                .option(ChannelOption.SO_KEEPALIVE, true);

        channelLock.lock();

        try {
            // Start the connection attempt.
            channel = bootstrap.connect(host, port).sync().channel();

            result = true;
            logger.info("Connected to remote message server");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to connect to remote message bus", e);
            disconnectFromServer();
        } finally {
            channelLock.unlock();
        }

        return result;
    }

    private class MessageBusClientInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();

            // Add the text line codec combination first,
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));
            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));

            // and then business logic.
            pipeline.addLast("handler", new MessageBusClientHandler());
        }
    }

    /**
     * Handles a client-side channel.
     */
    @ChannelHandler.Sharable
    private class MessageBusClientHandler extends ChannelInboundHandlerAdapter {

        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        private String decrypt(final Object object) {
            String plainMessage;

            if (encryptionManager != null) {
                plainMessage = encryptionManager.decrypt(object.toString());
            } else {
                plainMessage = object.toString();
            }

            return plainMessage;
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {

            try {
                final String plainMessage = decrypt(msg);

                logger.log(Level.FINE, "messageReceived: {0}", plainMessage);

                if (plainMessage.startsWith("<Message")) {
                    executorService.submit(() -> {
                        final Message message = (Message) xstream.fromXML(plainMessage);

                        final Engine engine = EngineFactory.getEngine(name);
                        Objects.requireNonNull(engine);

                        // ignore our own messages
                        if (!engine.getUuid().equals(message.getSource())) {
                            processRemoteMessage(message);
                        }
                    });
                } else if (plainMessage.startsWith(MessageBusServer.PATH_PREFIX)) {
                    dataBasePath = plainMessage.substring(MessageBusServer.PATH_PREFIX.length());
                    logger.log(Level.FINE, "Remote data path is: {0}", dataBasePath);
                } else if (plainMessage.startsWith(MessageBusServer.DATA_STORE_TYPE_PREFIX)) {
                    dataBaseType = DataStoreType.valueOf(plainMessage.substring(MessageBusServer.DATA_STORE_TYPE_PREFIX.length()));
                    logger.log(Level.FINE, "Remote dataBaseType type is: {0}", dataBaseType.name());
                } else if (plainMessage.startsWith(EncryptionManager.DECRYPTION_ERROR_TAG)) {    // decryption has failed, shut down the engine
                    logger.log(Level.SEVERE, "Unable to decrypt the remote message");
                } else if (plainMessage.startsWith(JpaNetworkServer.STOP_SERVER_MESSAGE)) {
                    logger.info("Server is shutting down");
                    EngineFactory.closeEngine(name);
                } else {
                    logger.log(Level.SEVERE, "Unknown message: {0}", plainMessage);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            ctx.close();
        }
    }

    void disconnectFromServer() {

        channelLock.lock();

        try {
            if (channel != null) {  // null from a prior failed connection
                channel.close().sync();
            }
        } catch (final InterruptedException e) {
            LogUtil.logSevere(MessageBusClient.class, e);
        } finally {
            channelLock.unlock();
        }

        eventLoopGroup.shutdownGracefully();

        channel = null;
        eventLoopGroup = null;
    }

    synchronized void sendRemoteMessage(final Message message) {
        CharArrayWriter writer = new CharArrayWriter();
        xstream.marshal(message, new CompactWriter(writer));

        sendRemoteMessage(writer.toString());

        logger.log(Level.FINE, "sent: {0}", writer);
    }

    void sendRemoteShutdownRequest() {
        sendRemoteMessage(JpaNetworkServer.STOP_SERVER_MESSAGE);
    }

    private void sendRemoteMessage(final String message) {
        channelLock.lock();

        try {
            if (encryptionManager != null) {
                channel.writeAndFlush(encryptionManager.encrypt(message) + MessageBusServer.EOL_DELIMITER).sync();
            } else {
                channel.writeAndFlush(message + MessageBusServer.EOL_DELIMITER).sync();
            }
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } catch (final NullPointerException e) {
            if (channel == null) {
                logger.info("Channel was null");
            }

            logger.log(Level.INFO, "Tried to send message: {0} through a null channel", message);
        } finally {
            channelLock.unlock();
        }
    }

    /**
     * Takes a remote message and forces remote updates before sending the message to the MessageBus to notify UI
     * components of changes.
     *
     * @param message Message to process and send
     */
    private void processRemoteMessage(final Message message) {
        logger.fine("processing a remote message");

        final Engine engine = EngineFactory.getEngine(name);
        Objects.requireNonNull(engine);

        if (message.getChannel() == MessageChannel.ACCOUNT) {
            final Account account = message.getObject(MessageProperty.ACCOUNT);
            switch (message.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_REMOVE:
                    engine.refresh(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));
                    engine.refresh(account.getParent());
                    break;
                case ACCOUNT_MODIFY:
                case ACCOUNT_SECURITY_ADD:
                case ACCOUNT_SECURITY_REMOVE:
                case ACCOUNT_VISIBILITY_CHANGE:
                    engine.refresh(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.BUDGET) {
            final Budget budget = message.getObject(MessageProperty.BUDGET);
            switch (message.getEvent()) {
                case BUDGET_ADD:
                case BUDGET_UPDATE:
                case BUDGET_REMOVE:
                case BUDGET_GOAL_UPDATE:
                    engine.refresh(budget);
                    message.setObject(MessageProperty.BUDGET, engine.getBudgetByUuid(budget.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.COMMODITY) {
            switch (message.getEvent()) {
                case CURRENCY_ADD:
                case CURRENCY_MODIFY:
                    final CommodityNode currency = message.getObject(MessageProperty.COMMODITY);
                    engine.refresh(currency);
                    message.setObject(MessageProperty.COMMODITY, engine.getCurrencyNodeByUuid(currency.getUuid()));
                    break;
                case SECURITY_ADD:
                case SECURITY_MODIFY:
                case SECURITY_HISTORY_ADD:
                case SECURITY_HISTORY_REMOVE:
                    final CommodityNode node = message.getObject(MessageProperty.COMMODITY);
                    engine.refresh(node);
                    message.setObject(MessageProperty.COMMODITY, engine.getSecurityNodeByUuid(node.getUuid()));
                    break;
                case EXCHANGE_RATE_ADD:
                case EXCHANGE_RATE_REMOVE:
                    final ExchangeRate rate = message.getObject(MessageProperty.EXCHANGE_RATE);
                    engine.refresh(rate);
                    message.setObject(MessageProperty.EXCHANGE_RATE, engine.getExchangeRateByUuid(rate.getUuid()));
                    break;
                default:
                    break;
            }
        }

        if (message.getChannel() == MessageChannel.CONFIG && message.getEvent() == ChannelEvent.CONFIG_MODIFY) {
            final Config config = message.getObject(MessageProperty.CONFIG);
            engine.refresh(config);
            message.setObject(MessageProperty.CONFIG, engine.getStoredObjectByUuid(Config.class, config.getUuid()));
        }

        if (message.getChannel() == MessageChannel.REMINDER) {
            switch (message.getEvent()) {
                case REMINDER_ADD:
                case REMINDER_REMOVE:
                    final Reminder reminder = message.getObject(MessageProperty.REMINDER);
                    engine.refresh(reminder);
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
                    final Transaction transaction = message.getObject(MessageProperty.TRANSACTION);
                    engine.refresh(transaction);
                    message.setObject(MessageProperty.TRANSACTION, engine.getTransactionByUuid(transaction.getUuid()));

                    final Account account = message.getObject(MessageProperty.ACCOUNT);
                    engine.refresh(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getAccountByUuid(account.getUuid()));
                    break;
                default:
                    break;
            }
        }

        /* Flag the message as remote */
        message.setRemote();

        logger.fine("fire remote message");
        MessageBus.getInstance(name).fireEvent(message);
    }
}
