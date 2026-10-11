package ru.constructforspace.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.constructforspace.ConstructForSpace;
import ru.constructforspace.network.PacketDuplicate;
import ru.constructforspace.network.PacketDuplicateResult;
import ru.constructforspace.network.PacketGive;
import ru.constructforspace.network.PacketRegenerationRequest;
import ru.constructforspace.network.PacketRegenerationStatus;
import ru.constructforspace.server.DuplicateServerQueue;

/** Three command-free survival mechanics: duplicate, give, and permanent regeneration. */
public class GuiDuplicate extends GuiScreen {
    private static final int PANEL_WIDTH = 264;
    private static final int PANEL_HEIGHT = 196;
    private static final int MODE_DUPLICATE = 0;
    private static final int MODE_GIVE = 1;
    private static final int MODE_REGEN = 2;

    private GuiTextField amountField;
    private GuiButton magnifier;
    private GuiButton dupeTab;
    private GuiButton giveTab;
    private GuiButton regenTab;
    private GuiButton actionButton;
    private boolean regenKnown;
    private boolean regenEnabled;
    private boolean regenPending;
    private int currentMode = MODE_DUPLICATE;
    private int selectedInventorySlot = -1;
    private ItemStack selectedCatalogStack;
    private String selectedCatalogId;
    private int selectedCatalogMeta;
    private final String[] savedAmounts = { "64", "64", "" };
    private final String[] statuses = { "", "", "Получение состояния..." };

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;

        dupeTab = new GuiButton(10, x + 20, y + 32, 73, 20, "1. Дюп");
        giveTab = new GuiButton(11, x + 96, y + 32, 74, 20, "2. Выдача");
        regenTab = new GuiButton(12, x + 173, y + 32, 71, 20, "3. Жизнь");
        buttonList.add(dupeTab);
        buttonList.add(giveTab);
        buttonList.add(regenTab);

        amountField = new GuiTextField(fontRendererObj, x + 123, y + 100, 56, 20);
        amountField.setMaxStringLength(4);
        amountField.setText(savedAmounts[currentMode]);
        amountField.setFocused(false);
        magnifier = new GuiButton(2, x + 186, y + 100, 24, 20, "");
        buttonList.add(magnifier);
        actionButton = new GuiButton(1, x + 20, y + 145, 155, 20, "");
        buttonList.add(actionButton);
        buttonList.add(new GuiButton(0, x + 183, y + 145, 61, 20, "Закрыть"));
        refreshButtons();
        // Read actual per-player state from the server instead of assuming OFF.
        ConstructForSpace.network.sendToServer(
            new PacketRegenerationRequest(PacketRegenerationRequest.QUERY));
    }

    private void refreshButtons() {
        dupeTab.enabled = currentMode != MODE_DUPLICATE;
        giveTab.enabled = currentMode != MODE_GIVE;
        regenTab.enabled = currentMode != MODE_REGEN;
        magnifier.visible = currentMode != MODE_REGEN;
        if (currentMode == MODE_DUPLICATE) {
            actionButton.displayString = "Дублировать";
        } else if (currentMode == MODE_GIVE) {
            actionButton.displayString = "Выдать предмет";
        } else {
            actionButton.displayString = !regenKnown ? "Проверка..."
                : (regenEnabled ? "Отключить регенерацию" : "Включить регенерацию");
        }
        actionButton.enabled = currentMode != MODE_REGEN || (regenKnown && !regenPending);
    }

    private void setMode(int mode) {
        if (currentMode != MODE_REGEN) {
            savedAmounts[currentMode] = amountField.getText();
        }
        currentMode = mode;
        if (mode != MODE_REGEN) {
            amountField.setText(savedAmounts[mode]);
        }
        amountField.setFocused(false);
        refreshButtons();
    }

    public void selectInventorySlot(int index) {
        selectedInventorySlot = index;
        statuses[MODE_DUPLICATE] = "";
    }

    public void selectCatalogItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        String id = Item.itemRegistry.getNameForObject(stack.getItem());
        if (id == null) {
            return;
        }
        selectedCatalogStack = stack.copy();
        selectedCatalogStack.stackSize = 1;
        selectedCatalogId = id;
        selectedCatalogMeta = stack.getItemDamage();
        statuses[MODE_GIVE] = "";
    }

    @Override
    public void updateScreen() {
        if (currentMode != MODE_REGEN) {
            amountField.updateCursorCounter();
        }
    }

    private ItemStack getSelectedStack() {
        if (currentMode == MODE_GIVE) {
            return selectedCatalogStack;
        }
        if (mc.thePlayer == null || selectedInventorySlot < 0 || selectedInventorySlot >= 36) {
            return null;
        }
        return mc.thePlayer.inventory.mainInventory[selectedInventorySlot];
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        drawRect(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, 0xFF111111);
        drawRect(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFFC6C6C6);
        drawRect(x + 4, y + 4, x + PANEL_WIDTH - 4, y + PANEL_HEIGHT - 4, 0xFF383838);
        drawRect(x + 6, y + 58, x + PANEL_WIDTH - 6, y + PANEL_HEIGHT - 29, 0xFF8B8B8B);
        drawCenteredString(fontRendererObj, "construct for space", x + PANEL_WIDTH / 2, y + 12, 0xFFFFFF);

        if (currentMode == MODE_REGEN) {
            drawCenteredString(fontRendererObj,
                !regenKnown ? "Состояние: проверка..." :
                    (regenEnabled ? "Бессмертие: ВКЛ" : "Бессмертие: ВЫКЛ"),
                x + PANEL_WIDTH / 2, y + 71,
                regenKnown && regenEnabled ? 0x55FF55 : 0xFFFFFF);
            drawCenteredString(fontRendererObj, "Максимальная регенерация", x + PANEL_WIDTH / 2,
                y + 92, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "Здоровье восстанавливается", x + PANEL_WIDTH / 2,
                y + 106, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "пока функция включена", x + PANEL_WIDTH / 2,
                y + 118, 0xFFFFFF);
        } else {
            drawRect(x + 19, y + 65, x + 40, y + 86, 0xFF222222);
            drawRect(x + 20, y + 66, x + 39, y + 85, 0xFF8B8B8B);
            ItemStack selected = getSelectedStack();
            if (selected == null) {
                drawString(fontRendererObj, "Предмет: не выбран", x + 45, y + 71, 0xFFFFFF);
            } else {
                RenderHelper.enableGUIStandardItemLighting();
                GL11.glColor4f(1F, 1F, 1F, 1F);
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(),
                    selected, x + 22, y + 68);
                RenderHelper.disableStandardItemLighting();
                drawString(fontRendererObj,
                    fontRendererObj.trimStringToWidth(selected.getDisplayName(), 194),
                    x + 45, y + 71, 0xFFFFFF);
            }
            drawString(fontRendererObj, "Количество:", x + 20, y + 106, 0xFFFFFF);
            drawString(fontRendererObj, "от 1 до " + DuplicateServerQueue.MAX_AMOUNT,
                x + 20, y + 125, 0xEEEEEE);
        }

        if (!statuses[currentMode].isEmpty()) {
            drawCenteredString(fontRendererObj,
                fontRendererObj.trimStringToWidth(statuses[currentMode], PANEL_WIDTH - 14),
                x + PANEL_WIDTH / 2, y + 177, 0xFFFFE080);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (currentMode == MODE_REGEN) {
            return;
        }
        amountField.drawTextBox();
        // Little pixel magnifier on a standard Minecraft button.
        int ix = magnifier.xPosition + 10;
        int iy = magnifier.yPosition + 8;
        int color = 0xFFEFEFEF;
        drawRect(ix - 2, iy - 4, ix + 2, iy - 3, color);
        drawRect(ix - 3, iy - 3, ix - 2, iy + 1, color);
        drawRect(ix + 2, iy - 3, ix + 3, iy + 1, color);
        drawRect(ix - 2, iy + 1, ix + 2, iy + 2, color);
        drawRect(ix + 2, iy + 2, ix + 4, iy + 4, color);
        drawRect(ix + 4, iy + 4, ix + 6, iy + 6, color);
        ItemStack selected = getSelectedStack();
        if (selected != null && mouseX >= x + 20 && mouseX < x + 40
            && mouseY >= y + 65 && mouseY < y + 86) {
            renderToolTip(selected, mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(null);
        } else if (button.id == 10) {
            setMode(MODE_DUPLICATE);
        } else if (button.id == 11) {
            setMode(MODE_GIVE);
        } else if (button.id == 12) {
            setMode(MODE_REGEN);
        } else if (button.id == 2 && currentMode != MODE_REGEN) {
            savedAmounts[currentMode] = amountField.getText();
            mc.displayGuiScreen(currentMode == MODE_DUPLICATE
                ? new GuiInventoryPicker(this) : new GuiCatalogPicker(this));
        } else if (button.id == 1) {
            executeAction();
        }
    }

    private void executeAction() {
        if (currentMode == MODE_REGEN) {
            requestRegenerationToggle();
            return;
        }
        savedAmounts[currentMode] = amountField.getText().trim();
        if (getSelectedStack() == null) {
            statuses[currentMode] = "Сначала выбери предмет через лупу.";
            return;
        }
        if (mc.thePlayer == null) {
            statuses[currentMode] = "Игрок не найден.";
            return;
        }
        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || held.getItem() != Items.stick) {
            statuses[currentMode] = "Для работы нужна палка в руке.";
            return;
        }

        final int amount;
        try {
            amount = Integer.parseInt(savedAmounts[currentMode]);
        } catch (NumberFormatException ignored) {
            statuses[currentMode] = "Укажи целое число от 1 до 4096.";
            return;
        }
        if (amount < 1 || amount > DuplicateServerQueue.MAX_AMOUNT) {
            statuses[currentMode] = "Укажи целое число от 1 до 4096.";
            return;
        }
        if (currentMode == MODE_DUPLICATE) {
            ConstructForSpace.network.sendToServer(new PacketDuplicate(selectedInventorySlot, amount));
        } else {
            if (selectedCatalogId == null) {
                statuses[currentMode] = "Сначала выбери предмет.";
                return;
            }
            ConstructForSpace.network.sendToServer(
                new PacketGive(selectedCatalogId, selectedCatalogMeta, amount));
        }
        statuses[currentMode] = "Ожидание ответа игры...";
    }

    private void requestRegenerationToggle() {
        if (!regenKnown || regenPending || mc.thePlayer == null) {
            return;
        }
        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || held.getItem() != Items.stick) {
            statuses[MODE_REGEN] = "Для переключения нужна палка в руке.";
            return;
        }
        regenPending = true;
        statuses[MODE_REGEN] = "Ожидание ответа сервера...";
        refreshButtons();
        ConstructForSpace.network.sendToServer(new PacketRegenerationRequest(
            regenEnabled ? PacketRegenerationRequest.DISABLE : PacketRegenerationRequest.ENABLE));
    }

    public void showRegenerationStatus(boolean enabled, int resultCode) {
        regenEnabled = enabled;
        regenKnown = true;
        regenPending = false;
        if (resultCode == PacketRegenerationStatus.NO_STICK) {
            statuses[MODE_REGEN] = "Для переключения нужна палка в руке.";
        } else if (resultCode == PacketRegenerationStatus.SURVIVAL_ONLY) {
            statuses[MODE_REGEN] = "Только для режима Выживание.";
        } else if (resultCode == PacketRegenerationStatus.INVALID_REQUEST) {
            statuses[MODE_REGEN] = "Некорректный запрос.";
        } else {
            statuses[MODE_REGEN] = enabled
                ? "Активна до отключения через меню." : "Регенерация отключена.";
        }
        refreshButtons();
    }

    public void showResult(int code, int added, int requested) {
        statuses[MODE_DUPLICATE] = resultText(code, added, requested);
    }

    public void showGiveResult(int code, int added, int requested) {
        statuses[MODE_GIVE] = resultText(code, added, requested);
    }

    private String resultText(int code, int added, int requested) {
        if (code == PacketDuplicateResult.NO_STICK) {
            return "Для работы нужна палка в руке.";
        } else if (code == PacketDuplicateResult.NO_ITEM) {
            return "Предмет не найден или уже отсутствует.";
        } else if (code == PacketDuplicateResult.NO_SPACE) {
            return "Недостаточно места в инвентаре.";
        } else if (code == PacketDuplicateResult.INVALID_REQUEST) {
            return "Недопустимый предмет, число или ячейка.";
        } else if (code == PacketDuplicateResult.SURVIVAL_ONLY) {
            return "Функция доступна только в выживании.";
        } else if (added < requested) {
            return "Добавлено " + added + " из " + requested + ". Мало места.";
        }
        return "Готово! Добавлено предметов: " + added;
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (currentMode == MODE_REGEN) {
            super.keyTyped(character, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN && amountField.isFocused()) {
            executeAction();
            return;
        }
        if (amountField.textboxKeyTyped(character, keyCode)) {
            savedAmounts[currentMode] = amountField.getText();
            return;
        }
        super.keyTyped(character, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (currentMode != MODE_REGEN) {
            amountField.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
