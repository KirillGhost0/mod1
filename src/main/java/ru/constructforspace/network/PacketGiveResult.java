package ru.constructforspace.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.ConstructForSpace;

/** Silent acknowledgment, displayed only inside the mod GUI. */
public class PacketGiveResult implements IMessage {
    private int code;
    private int added;
    private int requested;

    public PacketGiveResult() { }

    public PacketGiveResult(int code, int added, int requested) {
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

    public static final class Handler implements IMessageHandler<PacketGiveResult, IMessage> {
        @Override
        public IMessage onMessage(PacketGiveResult message, MessageContext context) {
            ConstructForSpace.proxy.onGiveResult(message.code, message.added, message.requested);
            return null;
        }
    }
}
