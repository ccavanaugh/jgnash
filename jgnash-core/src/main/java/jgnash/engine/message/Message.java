/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.util.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Objects;

/**
 * Message object.
 *
 * @author Craig Cavanaugh
 */
public class Message implements Serializable, Cloneable {

    private ChannelEvent event;

    private MessageChannel channel;

    private String source;

    transient private EnumMap<MessageProperty, StoredObject> properties = new EnumMap<>(MessageProperty.class);

    /**
     * Used to flag message sent remotely.
     */
    private transient boolean remote;

    /**
     * No argument constructor for reflection purposes.<br>
     * <b>Do not use to create new instances</b>
     *
     * @deprecated
     */
    @Deprecated
    public Message() {
    }

    public Message(final MessageChannel channel, final ChannelEvent event, final Engine source) {
        this(channel, event, source.getUuid());
    }

    private Message(final MessageChannel channel, final ChannelEvent event, final String source) {
        this.source = Objects.requireNonNull(source);
        this.event = Objects.requireNonNull(event);
        this.channel = Objects.requireNonNull(channel);
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public ChannelEvent getEvent() {
        return event;
    }

    /**
     * Sets a message property. The value must be reachable by the engine or and exception will be thrown.
     *
     * @param key   property key
     * @param value message value
     * @throws NullPointerException throws an exception if value is null
     */
    public void setObject(@NotNull final MessageProperty key, @NotNull final StoredObject value) throws NullPointerException {
        properties.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
    }

    /**
     * Returns a {@code StoredObject} given a property key.
     *
     * @param key {@code MessageProperty} to search for
     * @param <T> instance of {@code StoredObject}
     * @return object if found, {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends StoredObject> T getObject(final MessageProperty key) {
        return (T) properties.get(key);
    }

    public String getSource() {
        return source;
    }

    void setRemote() {
        remote = true;
    }

    public boolean isRemote() {
        return remote;
    }

    /**
     * Write message out to ObjectOutputStream.
     *
     * @param s stream
     * @throws IOException io exception
     * @serialData Write serializable fields, if any exist. Write out the integer count of properties. Write out key and
     * value of each property
     */
    @SuppressWarnings("unused")
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        // write the property count
        s.writeInt(properties.size());

        StoredObject[] values = properties.values().toArray(new StoredObject[properties.size()]);
        MessageProperty[] keys = properties.keySet().toArray(new MessageProperty[properties.size()]);

        for (int i = 0; i < properties.size(); i++) {
            s.writeObject(keys[i]);
            s.writeUTF(values[i].getClass().getName());
            s.writeUTF(values[i].getUuid());
        }
    }

    /**
     * Read a Message from an ObjectInputStream.
     *
     * @param s input stream
     * @throws java.io.IOException    io exception
     * @throws ClassNotFoundException thrown is class is not found
     * @serialData Read serializable fields, if any exist. Read the integer count of properties. Read the key and value
     * of each property
     */
    @SuppressWarnings({"unchecked", "unused"})
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        properties = new EnumMap<>(MessageProperty.class);

        final int size = s.readInt();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (int i = 0; i < size; i++) {
            MessageProperty key = (MessageProperty) s.readObject();
            Class<? extends StoredObject> clazz = (Class<? extends StoredObject>) Class.forName(s.readUTF());
            StoredObject value = engine.getStoredObjectByUuid(clazz, s.readUTF());
            properties.put(key, value);
        }
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
        final Message m = (Message) super.clone();
        m.properties = properties.clone();

        return m;
    }

    /**
     * Returns the event and channel for this message.
     *
     * @return event and channel information
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Message [event=%s, channel=%s, source=%s]", event, channel, source);
    }
}
