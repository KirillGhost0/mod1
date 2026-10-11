package ru.constructforspace.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.ConstructForSpace;

/** A small server-to-client acknowledgement. No chat messages or commands are used. */
public class PacketDuplicateResult implements IMessage {
    public static final int DONE = 0;
    public static final int NO_STICK = 1;
    public static final int NO_ITEM = 2;
    public static final int NO_SPACE = 3;
    public static final int INVALID_REQUEST = 4;
    public static final int SURVIVAL_ONLY = 5;

    private int code;
    private int added;
    private int requested;

    public PacketDuplicateResult() { }

    public PacketDuplicateResult(int code, int added, int requested) {
        this.code = code;
        this.added = added;
        this.requested = requested;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        code = buffer.readInt();
        added = buffer.readInt();
        requested = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(code);
        buffer.writeInt(added);
        buffer.writeInt(requested);
    }

    public static class Handler implements IMessageHandler<PacketDuplicateResult, IMessage> {
        @Override
        public IMessage onMessage(PacketDuplicateResult message, MessageContext context) {
            // This runs on the networking thread in 1.7.10. The client proxy queues
            // the result and applies it during the next client tick.
            ConstructForSpace.proxy.onDuplicateResult(message.code, message.added, message.requested);
            return null;
        }
    }
}
