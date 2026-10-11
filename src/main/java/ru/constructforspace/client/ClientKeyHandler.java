package ru.constructforspace.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

public class ClientKeyHandler {
    public static final KeyBinding OPEN_KEY = new KeyBinding(
        "key.constructforspace.open", Keyboard.KEY_F, "key.categories.constructforspace"
    );

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // F is a registered key; ALT is checked separately, so F alone does nothing.
        while (OPEN_KEY.isPressed()) {
            boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
            if (!altDown) {
                continue;
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen != null || minecraft.thePlayer == null) {
                continue;
            }
            ItemStack held = minecraft.thePlayer.inventory.getCurrentItem();
            if (held != null && held.getItem() == Items.stick) {
                minecraft.displayGuiScreen(new GuiDuplicate());
            }
        }
    }
}
