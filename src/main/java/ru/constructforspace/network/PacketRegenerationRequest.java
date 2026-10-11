package ru.constructforspace.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.server.RegenerationController;

/** A command-free request to read or change the player's personal regeneration setting. */
public class PacketRegenerationRequest implements IMessage {
    public static final int QUERY = 0;
    public static final int ENABLE = 1;
    public static final int DISABLE = 2;

    private int action;

    public PacketRegenerationRequest() { }

    public PacketRegenerationRequest(int action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    public static final class Handler implements IMessageHandler<PacketRegenerationRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketRegenerationRequest message, MessageContext context) {
            // Network callbacks execute on a Netty thread in 1.7.10.
            // Queue every request; player/NBT/potion state is only touched on the server tick.
            RegenerationController.enqueue(context.getServerHandler().playerEntity, message.action);
            return null;
        }
    }
}
