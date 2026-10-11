package ru.constructforspace.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * A searchable, paged TooManyItems-style catalog of registered items and their
 * visible subitems. This never opens creative mode or transfers items itself.
 */
public class GuiCatalogPicker extends GuiScreen {
    private static final int PANEL_WIDTH = 288;
    private static final int PANEL_HEIGHT = 212;
    private static final int COLS = 10;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLS * ROWS;
    private static final int MAX_ITEMS = 20000;
    private static final int MAX_SUBITEMS_PER_ITEM = 512;

    private static List<Entry> catalog;
    private final GuiDuplicate previous;
    private final List<Entry> matches = new ArrayList<Entry>();
    private GuiTextField searchField;
    private String currentQuery = "";
    private int page;
    private GuiButton prevPage;
    private GuiButton nextPage;

    public GuiCatalogPicker(GuiDuplicate previous) {
        this.previous = previous;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        searchField = new GuiTextField(fontRendererObj, x + 17, y + 30, PANEL_WIDTH - 34, 18);
        searchField.setMaxStringLength(64);
        searchField.setText(currentQuery);
        searchField.setFocused(true);
        prevPage = new GuiButton(1, x + 18, y + 181, 38, 20, "<");
        nextPage = new GuiButton(2, x + 61, y + 181, 38, 20, ">");
        buttonList.add(prevPage);
        buttonList.add(nextPage);
        buttonList.add(new GuiButton(0, x + PANEL_WIDTH - 84, y + 181, 65, 20, "Назад"));
        if (catalog == null) {
            buildCatalog();
        }
        rebuildMatches();
    }

    private static void buildCatalog() {
        List<Entry> collected = new ArrayList<Entry>();
        Set<String> seen = new HashSet<String>();
        for (Object obj : Item.itemRegistry) {
            if (!(obj instanceof Item) || collected.size() >= MAX_ITEMS) {
                continue;
            }
            Item item = (Item) obj;
            String id = Item.itemRegistry.getNameForObject(item);
            if (id == null) {
                continue;
            }
            List<ItemStack> variants = new ArrayList<ItemStack>();
            try {
                // 1.7.10 API: this enumerates blocks as ItemBlocks, potions, dyes, mod items.
                CreativeTabs tab = item.getCreativeTab();
                item.getSubItems(item, tab, variants);
            } catch (RuntimeException ignored) {
                // Some old third-party mod items have broken creative-tab implementations.
            }
            if (variants.isEmpty()) {
                variants.add(new ItemStack(item, 1, 0));
            }
            int limit = Math.min(variants.size(), MAX_SUBITEMS_PER_ITEM);
            for (int i = 0; i < limit && collected.size() < MAX_ITEMS; i++) {
                ItemStack stack = variants.get(i);
                if (stack == null || stack.getItem() == null) {
                    continue;
                }
                int meta = stack.getItemDamage();
                if (meta < 0 || meta > 32767) {
                    continue;
                }
                String realId = Item.itemRegistry.getNameForObject(stack.getItem());
                if (realId == null || !seen.add(realId + "#" + meta)) {
                    continue;
                }
                try {
                    ItemStack displayed = stack.copy();
                    displayed.stackSize = 1;
                    String name = displayed.getDisplayName();
                    collected.add(new Entry(displayed, realId, name));
                } catch (RuntimeException ignored) {
                    // A broken item must not make the entire catalog inaccessible.
                }
            }
        }
        catalog = collected;
    }

    private void rebuildMatches() {
        currentQuery = searchField.getText();
        String needle = currentQuery.trim().toLowerCase(Locale.ROOT);
        matches.clear();
        for (Entry entry : catalog) {
            if (needle.isEmpty() || entry.searchText.contains(needle)) {
                matches.add(entry);
            }
        }
        page = 0;
        refreshPager();
    }

    private void refreshPager() {
        int pages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
        prevPage.enabled = page > 0;
        nextPage.enabled = page + 1 < pages;
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        drawRect(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, 0xFF111111);
        drawRect(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFFC6C6C6);
        drawRect(x + 4, y + 4, x + PANEL_WIDTH - 4, y + PANEL_HEIGHT - 4, 0xFF383838);
        drawRect(x + 7, y + 60, x + PANEL_WIDTH - 7, y + 175, 0xFF8B8B8B);
        drawCenteredString(fontRendererObj, "Каталог предметов", x + PANEL_WIDTH / 2, y + 11, 0xFFFFFF);
        drawString(fontRendererObj, "Найдено: " + matches.size(), x + 17, y + 51, 0xFFFFFF);

        int hovered = -1;
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotX = x + 39 + col * 21;
                int slotY = y + 67 + row * 20;
                int idx = page * PAGE_SIZE + row * COLS + col;
                drawRect(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF222222);
                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF999999);
                if (idx < matches.size()) {
                    Entry entry = matches.get(idx);
                    if (mouseX >= slotX && mouseX < slotX + 18
                        && mouseY >= slotY && mouseY < slotY + 18) {
                        drawRect(slotX, slotY, slotX + 18, slotY + 18, 0x88FFFFFF);
                        hovered = idx;
                    }
                    itemRender.renderItemAndEffectIntoGUI(fontRendererObj,
                        mc.getTextureManager(), entry.stack, slotX + 1, slotY + 1);
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
        searchField.drawTextBox();
        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        drawString(fontRendererObj, (page + 1) + "/" + totalPages, x + 114, y + 187, 0xFFFFFF);
        if (hovered >= 0) {
            renderToolTip(matches.get(hovered).stack, mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(previous);
        } else if (button.id == 1 && page > 0) {
            page--;
            refreshPager();
        } else if (button.id == 2) {
            page++;
            refreshPager();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            int x = (width - PANEL_WIDTH) / 2;
            int y = (height - PANEL_HEIGHT) / 2;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int slotX = x + 39 + col * 21;
                    int slotY = y + 67 + row * 20;
                    if (mouseX >= slotX && mouseX < slotX + 18
                        && mouseY >= slotY && mouseY < slotY + 18) {
                        int index = page * PAGE_SIZE + row * COLS + col;
                        if (index < matches.size()) {
                            previous.selectCatalogItem(matches.get(index).stack);
                            mc.displayGuiScreen(previous);
                        }
                        return;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
        searchField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            page += delta < 0 ? 1 : -1;
            refreshPager();
        }
    }

    @Override
    protected void keyTyped(char character, int code) {
        if (searchField.textboxKeyTyped(character, code)) {
            if (!currentQuery.equals(searchField.getText())) {
                rebuildMatches();
            }
            return;
        }
        super.keyTyped(character, code);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class Entry {
        final ItemStack stack;
        final String searchText;
        Entry(ItemStack stack, String registryName, String displayName) {
            this.stack = stack;
            this.searchText = (displayName + " " + registryName + " " + stack.getItemDamage())
                .toLowerCase(Locale.ROOT);
        }
    }
}
