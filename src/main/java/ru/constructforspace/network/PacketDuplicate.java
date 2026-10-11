package ru.constructforspace.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.constructforspace.server.DuplicateServerQueue;

/** Only a slot index and an amount are sent. The server chooses the real source stack. */
public class PacketDuplicate implements IMessage {
    private int inventorySlot;
    private int requestedAmount;

    public PacketDuplicate() {
        // Required for Forge to deserialize this packet.
    }

    public PacketDuplicate(int inventorySlot, int requestedAmount) {
        this.inventorySlot = inventorySlot;
        this.requestedAmount = requestedAmount;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        inventorySlot = buffer.readInt();
        requestedAmount = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(inventorySlot);
        buffer.writeInt(requestedAmount);
    }

    public static class Handler implements IMessageHandler<PacketDuplicate, IMessage> {
        @Override
        public IMessage onMessage(PacketDuplicate message, MessageContext context) {
            // Minecraft 1.7.10 handles packets on the Netty thread, NOT the game thread.
            // Do not touch any inventory here. Process it during the server tick instead.
            DuplicateServerQueue.enqueue(
                context.getServerHandler().playerEntity,
                message.inventorySlot,
                message.requestedAmount
            );
            return null;
        }
    }
}
