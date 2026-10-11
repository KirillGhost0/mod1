package ru.constructforspace.server;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import ru.constructforspace.ConstructForSpace;
import ru.constructforspace.network.PacketDuplicateResult;

/** Make all inventory changes on the main server tick, never on the Netty thread. */
public class DuplicateServerQueue {
    public static final int MAX_AMOUNT = 4096;
    private static final ConcurrentLinkedQueue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();

    public static void enqueue(EntityPlayerMP player, int slot, int amount) {
        if (player != null) {
            REQUESTS.offer(new Request(player, slot, amount));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Request request;
        int processed = 0;
        while (processed++ < 64 && (request = REQUESTS.poll()) != null) {
            duplicate(request);
        }
    }

    private static void duplicate(Request request) {
        EntityPlayerMP player = request.player;
        if (player.isDead || player.playerNetServerHandler == null) {
            return;
        }
        if (request.slot < 0 || request.slot >= 36
            || request.amount < 1 || request.amount > MAX_AMOUNT) {
            result(player, PacketDuplicateResult.INVALID_REQUEST, 0, request.amount);
            return;
        }

        // This is a legitimate server-side modded mechanic for survival only.
        // It does not execute commands, elevate permissions, or change game mode.
        if (player.theItemInWorldManager.getGameType() != WorldSettings.GameType.SURVIVAL) {
            result(player, PacketDuplicateResult.SURVIVAL_ONLY, 0, request.amount);
            return;
        }

        // The stick is mandatory on the server too; no command permission is involved.
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null || held.getItem() != Items.stick) {
            result(player, PacketDuplicateResult.NO_STICK, 0, request.amount);
            return;
        }
        // The client only supplies the slot number; its item identity is never trusted.
        ItemStack source = player.inventory.mainInventory[request.slot];
        if (source == null || source.getItem() == null || source.stackSize <= 0) {
            result(player, PacketDuplicateResult.NO_ITEM, 0, request.amount);
            return;
        }

        int added = InsertHelper.insert(player, source, request.amount);
        result(player, added == 0 ? PacketDuplicateResult.NO_SPACE : PacketDuplicateResult.DONE,
            added, request.amount);
    }

    private static void result(EntityPlayerMP player, int code, int added, int amount) {
        ConstructForSpace.network.sendTo(new PacketDuplicateResult(code, added, amount), player);
        // Intentionally NO addChatMessage, /give, /gamemode, or any other command.
    }

    private static final class Request {
        final EntityPlayerMP player;
        final int slot;
        final int amount;

        Request(EntityPlayerMP player, int slot, int amount) {
            this.player = player;
            this.slot = slot;
            this.amount = amount;
        }
    }
}
