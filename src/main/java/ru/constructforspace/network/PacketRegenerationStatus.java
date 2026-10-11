package ru.constructforspace.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.ConstructForSpace;

/** Server-authoritative switch state and private GUI result; never uses the chat. */
public class PacketRegenerationStatus implements IMessage {
    public static final int OK = 0;
    public static final int NO_STICK = 1;
    public static final int SURVIVAL_ONLY = 2;
    public static final int INVALID_REQUEST = 3;

    private boolean enabled;
    private int result;

    public PacketRegenerationStatus() { }

    public PacketRegenerationStatus(boolean enabled, int result) {
        this.enabled = enabled;
        this.result = result;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        result = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeByte(result);
    }

    public static final class Handler implements IMessageHandler<PacketRegenerationStatus, IMessage> {
        @Override
        public IMessage onMessage(PacketRegenerationStatus message, MessageContext context) {
            ConstructForSpace.proxy.onRegenerationStatus(message.enabled, message.result);
            return null;
        }
    }
}
