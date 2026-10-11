package ru.constructforspace.server;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import ru.constructforspace.ConstructForSpace;
import ru.constructforspace.network.PacketDuplicateResult;
import ru.constructforspace.network.PacketGiveResult;

/** The server creates the item. Players never run /give, become OP or enter creative mode. */
public class GiveServerQueue {
    private static final ConcurrentLinkedQueue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();

    public static void enqueue(EntityPlayerMP player, String name, int meta, int amount) {
        if (player != null) {
            REQUESTS.offer(new Request(player, name, meta, amount));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Request request;
        int processed = 0;
        while (processed++ < 64 && (request = REQUESTS.poll()) != null) {
            give(request);
        }
    }

    private static void give(Request request) {
        EntityPlayerMP player = request.player;
        if (player.isDead || player.playerNetServerHandler == null) return;
        if (request.amount < 1 || request.amount > DuplicateServerQueue.MAX_AMOUNT
            || request.name == null || request.name.length() < 3 || request.name.length() > 128
            || request.meta < 0 || request.meta > 32767) {
            result(player, PacketDuplicateResult.INVALID_REQUEST, 0, request.amount);
            return;
        }
        if (player.theItemInWorldManager.getGameType() != WorldSettings.GameType.SURVIVAL) {
            result(player, PacketDuplicateResult.SURVIVAL_ONLY, 0, request.amount);
            return;
        }
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null || held.getItem() != Items.stick) {
            result(player, PacketDuplicateResult.NO_STICK, 0, request.amount);
            return;
        }
        Object value = Item.itemRegistry.getObject(request.name);
        if (!(value instanceof Item)) {
            result(player, PacketDuplicateResult.NO_ITEM, 0, request.amount);
            return;
        }
        Item item = (Item) value;
        if (!request.name.equals(Item.itemRegistry.getNameForObject(item))) {
            result(player, PacketDuplicateResult.NO_ITEM, 0, request.amount);
            return;
        }

        ItemStack template = new ItemStack(item, 1, request.meta);
        int added = InsertHelper.insert(player, template, request.amount);
        result(player, added == 0 ? PacketDuplicateResult.NO_SPACE : PacketDuplicateResult.DONE,
            added, request.amount);
    }

    private static void result(EntityPlayerMP player, int code, int added, int amount) {
        ConstructForSpace.network.sendTo(new PacketGiveResult(code, added, amount), player);
    }

    private static final class Request {
        final EntityPlayerMP player;
        final String name;
        final int meta;
        final int amount;
        Request(EntityPlayerMP player, String name, int meta, int amount) {
            this.player = player;
            this.name = name;
            this.meta = meta;
            this.amount = amount;
        }
    }
}
