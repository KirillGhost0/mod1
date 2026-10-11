package ru.constructforspace.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.server.GiveServerQueue;

/** A request containing only a registered item name, damage value and count; no commands or NBT. */
public class PacketGive implements IMessage {
    private String registryName;
    private int metadata;
    private int requestedAmount;

    public PacketGive() { }

    public PacketGive(String registryName, int metadata, int amount) {
        this.registryName = registryName;
        this.metadata = metadata;
        this.requestedAmount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        registryName = ByteBufUtils.readUTF8String(buffer);
        metadata = buffer.readInt();
        requestedAmount = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, registryName);
        buffer.writeInt(metadata);
        buffer.writeInt(requestedAmount);
    }

    public static final class Handler implements IMessageHandler<PacketGive, IMessage> {
        @Override
        public IMessage onMessage(PacketGive message, MessageContext context) {
            // The Forge 1.7.10 network handler runs off-thread.
            GiveServerQueue.enqueue(context.getServerHandler().playerEntity,
                message.registryName, message.metadata, message.requestedAmount);
            return null;
        }
    }
}
