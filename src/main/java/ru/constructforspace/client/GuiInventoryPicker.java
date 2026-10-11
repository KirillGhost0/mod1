package ru.constructforspace.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Click-only replica of the 36 player inventory slots; does not move any items. */
public class GuiInventoryPicker extends GuiScreen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 154;
    private final GuiDuplicate previous;

    public GuiInventoryPicker(GuiDuplicate previous) {
        this.previous = previous;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        buttonList.add(new GuiButton(0, x + 74, y + 128, 92, 20, "Назад"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        drawRect(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, 0xFF161616);
        drawRect(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFFC6C6C6);
        drawRect(x + 4, y + 4, x + PANEL_WIDTH - 4, y + PANEL_HEIGHT - 4, 0xFF383838);
        drawCenteredString(fontRendererObj, "Выбери предмет в инвентаре", x + PANEL_WIDTH / 2, y + 11, 0xFFFFFF);

        int hoveredSlot = -1;
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = inventorySlot(row, col);
                int slotX = x + 30 + col * 20;
                int slotY = y + 30 + row * 20 + (row == 3 ? 5 : 0);
                drawRect(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF202020);
                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                if (mouseX >= slotX && mouseX < slotX + 18
                    && mouseY >= slotY && mouseY < slotY + 18) {
                    drawRect(slotX, slotY, slotX + 18, slotY + 18, 0x88FFFFFF);
                    hoveredSlot = slot;
                }
                ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
                if (stack != null) {
                    itemRender.renderItemAndEffectIntoGUI(fontRendererObj,
                        mc.getTextureManager(), stack, slotX + 1, slotY + 1);
                    itemRender.renderItemOverlayIntoGUI(fontRendererObj,
                        mc.getTextureManager(), stack, slotX + 1, slotY + 1);
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hoveredSlot >= 0) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[hoveredSlot];
            if (stack != null) {
                renderToolTip(stack, mouseX, mouseY);
            }
        }
    }

    /** Main inventory: rows 9-35, last row is the hotbar (slots 0-8). */
    private int inventorySlot(int row, int column) {
        return row == 3 ? column : 9 + row * 9 + column;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && mc.thePlayer != null) {
            int x = (width - PANEL_WIDTH) / 2;
            int y = (height - PANEL_HEIGHT) / 2;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = x + 30 + col * 20;
                    int slotY = y + 30 + row * 20 + (row == 3 ? 5 : 0);
                    if (mouseX >= slotX && mouseX < slotX + 18
                        && mouseY >= slotY && mouseY < slotY + 18) {
                        int slot = inventorySlot(row, col);
                        if (mc.thePlayer.inventory.mainInventory[slot] != null) {
                            previous.selectInventorySlot(slot);
                            mc.displayGuiScreen(previous);
                        }
                        return;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(previous);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
