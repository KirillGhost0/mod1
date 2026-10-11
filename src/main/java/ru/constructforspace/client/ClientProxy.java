package ru.constructforspace.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import ru.constructforspace.CommonProxy;

public class ClientProxy extends CommonProxy {
    private final ConcurrentLinkedQueue<Notice> pending = new ConcurrentLinkedQueue<Notice>();
    private final ConcurrentLinkedQueue<RegenNotice> regenPending = new ConcurrentLinkedQueue<RegenNotice>();

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(ClientKeyHandler.OPEN_KEY);
        FMLCommonHandler.instance().bus().register(new ClientKeyHandler());
        FMLCommonHandler.instance().bus().register(this);
    }

    @Override
    public void onDuplicateResult(int code, int added, int requested) {
        // Netty thread: do not access the current screen here.
        pending.offer(new Notice(false, code, added, requested));
    }

    @Override
    public void onGiveResult(int code, int added, int requested) {
        pending.offer(new Notice(true, code, added, requested));
    }

    @Override
    public void onRegenerationStatus(boolean enabled, int resultCode) {
        regenPending.offer(new RegenNotice(enabled, resultCode));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        RegenNotice regeneration;
        while ((regeneration = regenPending.poll()) != null) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiDuplicate) {
                ((GuiDuplicate) Minecraft.getMinecraft().currentScreen)
                    .showRegenerationStatus(regeneration.enabled, regeneration.resultCode);
            }
        }
        Notice notice;
        while ((notice = pending.poll()) != null) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiDuplicate) {
                GuiDuplicate screen = (GuiDuplicate) Minecraft.getMinecraft().currentScreen;
                if (notice.isGive) {
                    screen.showGiveResult(notice.code, notice.added, notice.requested);
                } else {
                    screen.showResult(notice.code, notice.added, notice.requested);
                }
            }
        }
    }

    private static final class RegenNotice {
        final boolean enabled;
        final int resultCode;

        RegenNotice(boolean enabled, int resultCode) {
            this.enabled = enabled;
            this.resultCode = resultCode;
        }
    }

    private static final class Notice {
        final boolean isGive;
        final int code;
        final int added;
        final int requested;

        Notice(boolean isGive, int code, int added, int requested) {
            this.isGive = isGive;
            this.code = code;
            this.added = added;
            this.requested = requested;
        }
    }
}
