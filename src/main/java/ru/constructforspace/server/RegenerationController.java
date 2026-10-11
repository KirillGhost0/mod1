package ru.constructforspace.server;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import ru.constructforspace.ConstructForSpace;
import ru.constructforspace.network.PacketRegenerationRequest;
import ru.constructforspace.network.PacketRegenerationStatus;

/**
 * Server-owned, per-player, permanent ultra-fast regeneration.
 * No /effect, /god, /gamemode, permission change or chat messages are involved.
 *
 * The toggle is saved in the normal player's Forge NBT. Each server tick heals
 * enabled survival players; lethal vanilla hits are intercepted before death.
 */
public final class RegenerationController {
    private static final String PLAYER_PERSISTED = EntityPlayer.PERSISTED_NBT_TAG;
    private static final String MOD_TAG = "constructforspace";
    private static final String ENABLED_TAG = "regenerationEnabled";
    // In vanilla 1.7.10 the fastest reliable natural regeneration interval is
    // one server tick: 50 >> 5 = 1. Using amplifier 255 is unnecessary and
    // can behave inconsistently because Java bit shifts wrap every 32 bits.
    private static final int REGEN_AMPLIFIER = 5;
    private static final ConcurrentLinkedQueue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();

    public static void enqueue(EntityPlayerMP player, int action) {
        if (player != null) {
            REQUESTS.offer(new Request(player, action));
        }
    }

    private static boolean isSurvival(EntityPlayerMP player) {
        return player.theItemInWorldManager.getGameType() == WorldSettings.GameType.SURVIVAL;
    }

    private static NBTTagCompound readSettings(EntityPlayer player) {
        return player.getEntityData().getCompoundTag(PLAYER_PERSISTED).getCompoundTag(MOD_TAG);
    }

    public static boolean isEnabled(EntityPlayer player) {
        return readSettings(player).getBoolean(ENABLED_TAG);
    }

    private static void setEnabled(EntityPlayer player, boolean enabled) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(PLAYER_PERSISTED);
        NBTTagCompound modSettings = persisted.getCompoundTag(MOD_TAG);
        modSettings.setBoolean(ENABLED_TAG, enabled);
        persisted.setTag(MOD_TAG, modSettings);
        entityData.setTag(PLAYER_PERSISTED, persisted);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Request request;
        int handled = 0;
        while (handled++ < 64 && (request = REQUESTS.poll()) != null) {
            processRequest(request);
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return;
        }
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            if (player.isDead || !isSurvival(player) || !isEnabled(player)) {
                continue;
            }
            // A visible, genuine regeneration effect, replenished automatically.
            PotionEffect effect = player.getActivePotionEffect(Potion.regeneration);
            if (effect == null || effect.getAmplifier() < REGEN_AMPLIFIER
                || (effect.getAmplifier() == REGEN_AMPLIFIER && effect.getDuration() < 40)) {
                player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 100, REGEN_AMPLIFIER));
            }
            // Instant top-up every game tick (20 times/second), beyond normal
            // potion speed. Does not affect food, inventory or game mode.
            if (player.getHealth() > 0.0F && player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void processRequest(Request request) {
        EntityPlayerMP player = request.player;
        if (player.isDead || player.playerNetServerHandler == null) {
            return;
        }
        boolean enabled = isEnabled(player);
        int result = PacketRegenerationStatus.OK;

        if (request.action == PacketRegenerationRequest.QUERY) {
            // A read-only status check; does not change anything.
        } else if (request.action != PacketRegenerationRequest.ENABLE
            && request.action != PacketRegenerationRequest.DISABLE) {
            result = PacketRegenerationStatus.INVALID_REQUEST;
        } else if (!isSurvival(player)) {
            result = PacketRegenerationStatus.SURVIVAL_ONLY;
        } else {
            ItemStack held = player.inventory.getCurrentItem();
            if (held == null || held.getItem() != Items.stick) {
                result = PacketRegenerationStatus.NO_STICK;
            } else {
                enabled = request.action == PacketRegenerationRequest.ENABLE;
                setEnabled(player, enabled);
                if (enabled) {
                    player.setHealth(player.getMaxHealth());
                } else {
                    PotionEffect regen = player.getActivePotionEffect(Potion.regeneration);
                    // Only remove the specific effect maintained by this mechanic.
                    if (regen != null && regen.getAmplifier() == REGEN_AMPLIFIER) {
                        player.removePotionEffect(Potion.regeneration.id);
                    }
                }
            }
        }
        ConstructForSpace.network.sendTo(new PacketRegenerationStatus(enabled, result), player);
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.entityLiving instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (!isSurvival(player) || !isEnabled(player)) {
            return;
        }
        // Rapid regeneration alone cannot prevent a one-hit kill. On a fatal
        // hit, instantly restore health instead of granting generic immunity;
        // ordinary nonfatal hits remain real and are healed on the next tick.
        if (event.ammount >= player.getHealth() && event.ammount > 0.0F) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
            if (isSurvival(player) && isEnabled(player)) {
                // Safety net for damage sources bypassing LivingHurtEvent.
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // Copy the persisted toggle on dimension switch or death/respawn.
        NBTTagCompound previous = event.original.getEntityData();
        if (previous.hasKey(PLAYER_PERSISTED, 10)) {
            event.entityPlayer.getEntityData().setTag(PLAYER_PERSISTED,
                previous.getCompoundTag(PLAYER_PERSISTED).copy());
        }
    }

    private static final class Request {
        final EntityPlayerMP player;
        final int action;

        Request(EntityPlayerMP player, int action) {
            this.player = player;
            this.action = action;
        }
    }
}
