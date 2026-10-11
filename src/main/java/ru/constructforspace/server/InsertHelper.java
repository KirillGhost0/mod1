package ru.constructforspace.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Insert bounded amounts; never drop an overflow stack into the world. */
final class InsertHelper {
    private InsertHelper() { }

    static int insert(EntityPlayerMP player, ItemStack template, int requested) {
        int remaining = requested;
        while (remaining > 0) {
            ItemStack portion = template.copy();
            int attempted = Math.min(remaining, Math.max(1, portion.getMaxStackSize()));
            portion.stackSize = attempted;
            player.inventory.addItemStackToInventory(portion);
            int inserted = attempted - portion.stackSize;
            if (inserted <= 0) break;
            remaining -= inserted;
        }
        int added = requested - remaining;
        if (added > 0) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
            player.sendContainerToPlayer(player.inventoryContainer);
        }
        return added;
    }
}
