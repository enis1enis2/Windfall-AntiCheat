package io.windfall.anticheat.core.command;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecklistGUI implements Listener {

    // 6 rows total, bottom row reserved for navigation/info
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = (ROWS - 1) * 9;
    private static final String TITLE_PREFIX = "Windfall Checks";

    // Materials that don't exist in 1.12 and below — null means fallback to solid colors
    private static final Material MAT_LIME_GLASS = getMaterial("LIME_STAINED_GLASS_PANE");
    private static final Material MAT_YELLOW_GLASS = getMaterial("YELLOW_STAINED_GLASS_PANE");
    private static final Material MAT_RED_GLASS = getMaterial("RED_STAINED_GLASS_PANE");
    // Paper 1.20.5+ removed string-based ItemMeta methods — detect once at startup
    private static final boolean HAS_MODERN_META = hasModernMeta();

    private final WindfallPlugin plugin;
    // Inventory hashCode → checks on that page; fragile if inventory is recreated, but fine for single-session GUI
    private final Map<Integer, List<Check>> pageChecks = new HashMap<>();
    private final Map<Integer, Integer> inventoryPage = new HashMap<>();

    public ChecklistGUI(WindfallPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        openPage(player, 0);
    }

    private void openPage(Player player, int page) {
        CheckManager checkManager = plugin.getCheckManager();
        List<Check> allChecks = checkManager.getChecks();

        int totalPages = Math.max(1, (int) Math.ceil((double) allChecks.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allChecks.size());
        List<Check> pageChecksList = allChecks.subList(start, end);

        String title = TITLE_PREFIX + " (" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, ROWS * 9, title);

        for (int i = 0; i < pageChecksList.size(); i++) {
            Check check = pageChecksList.get(i);
            inv.setItem(i, createCheckItem(check));
        }

        if (page > 0) {
            inv.setItem(ROWS * 9 - 9, createNavArrow(Material.ARROW, "Previous Page"));
        }
        if (page < totalPages - 1) {
            inv.setItem(ROWS * 9 - 1, createNavArrow(Material.ARROW, "Next Page"));
        }

        inv.setItem(ROWS * 9 - 5, createInfoItem(allChecks));

        int invHash = inv.hashCode();
        pageChecks.put(invHash, new ArrayList<>(pageChecksList));
        inventoryPage.put(invHash, page);

        player.openInventory(inv);
    }

    private ItemStack createCheckItem(Check check) {
        Material mat;
        if (check.isEnabled() && check.isPunishable()) {
            mat = MAT_LIME_GLASS != null ? MAT_LIME_GLASS : Material.DIAMOND;
        } else if (check.isEnabled() && !check.isPunishable()) {
            mat = MAT_YELLOW_GLASS != null ? MAT_YELLOW_GLASS : Material.GOLD_INGOT;
        } else {
            mat = MAT_RED_GLASS != null ? MAT_RED_GLASS : Material.REDSTONE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        ChatColor nameColor;
        if (check.isEnabled() && check.isPunishable()) {
            nameColor = ChatColor.GREEN;
        } else if (check.isEnabled()) {
            nameColor = ChatColor.YELLOW;
        } else {
            nameColor = ChatColor.RED;
        }

        setDisplayName(meta, nameColor + check.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Enabled: " + (check.isEnabled() ? ChatColor.GREEN : ChatColor.RED)
            + (check.isEnabled() ? "YES" : "NO"));
        lore.add(ChatColor.GRAY + "Punishable: " + (check.isPunishable() ? ChatColor.GREEN : ChatColor.RED)
            + (check.isPunishable() ? "YES" : "NO"));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Key: " + check.getStableKey());
        lore.add(ChatColor.DARK_GRAY + "Setback VL: " + check.getSetbackVl());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click: toggle enabled");
        lore.add(ChatColor.LIGHT_PURPLE + "Shift-click: toggle punishable");

        setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavArrow(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        setDisplayName(meta, ChatColor.WHITE + name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(List<Check> allChecks) {
        long enabled = allChecks.stream().filter(Check::isEnabled).count();
        long punishable = allChecks.stream().filter(Check::isPunishable).count();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        setDisplayName(meta, ChatColor.AQUA + "Windfall Info");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Checks: " + enabled + "/" + allChecks.size() + " active");
        lore.add(ChatColor.GRAY + "Punishable: " + punishable + "/" + allChecks.size());
        lore.add("");
        lore.add(ChatColor.GRAY + "Click arrows to navigate pages");
        setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();
        if (slot < 0 || slot >= topInventory.getSize()) return;

        ItemStack clicked = topInventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int invHash = topInventory.hashCode();
        List<Check> checks = pageChecks.get(invHash);
        if (checks == null) return;

        if (slot == topInventory.getSize() - 9) {
            int currentPage = inventoryPage.getOrDefault(invHash, 0);
            if (currentPage > 0) {
                openPage(player, currentPage - 1);
            }
            return;
        }

        if (slot == topInventory.getSize() - 1) {
            int currentPage = inventoryPage.getOrDefault(invHash, 0);
            int totalPages = (int) Math.ceil((double) plugin.getCheckManager().getChecks().size() / PAGE_SIZE);
            if (currentPage < totalPages - 1) {
                openPage(player, currentPage + 1);
            }
            return;
        }

        if (slot >= checks.size()) return;

        Check check = checks.get(slot);
        WindfallConfig config = plugin.getWindfallConfig();

        ClickType clickType = event.getClick();
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT
                || clickType == ClickType.RIGHT) {
            check.setPunishable(!check.isPunishable());
            config.saveCheckPunishable(check.getStableKey(), check.isPunishable());
        } else {
            check.setEnabled(!check.isEnabled());
            config.saveCheckEnabled(check.getStableKey(), check.isEnabled());
        }

        topInventory.setItem(slot, createCheckItem(check));
    }

    // Returns null on pre-1.13 servers where stained glass panes don't exist
    private static Material getMaterial(String name) {
        try {
            Material mat = Material.matchMaterial(name);
            return mat;
        } catch (Exception e) {
            return null;
        }
    }

    // Paper 1.20.5+ added displayName(Component) and removed displayName(String)
    // Detection via reflection avoids compile-time dependency on Adventure
    private static boolean hasModernMeta() {
        try {
            ItemMeta.class.getMethod("displayName", net.kyori.adventure.text.Component.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // Three-tier fallback: Paper 1.20.5+ → legacy setDisplayName(String) → direct call
    // Reflection is used because Adventure API isn't on Spigot's classpath
    private void setDisplayName(ItemMeta meta, String name) {
        try {
            if (HAS_MODERN_META) {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method textOf = componentClass.getMethod("text", Object.class);
                Object component = textOf.invoke(null, name);
                Method displayName = ItemMeta.class.getMethod("displayName", componentClass);
                displayName.invoke(meta, component);
            } else {
                throw new NoSuchMethodException("fallback");
            }
        } catch (Exception e) {
            try {
                Method setDisplayName = ItemMeta.class.getMethod("setDisplayName", String.class);
                setDisplayName.invoke(meta, name);
            } catch (Exception ex) {
                meta.setDisplayName(name);
            }
        }
    }

    // Same three-tier fallback as setDisplayName but for lore lines
    private void setLore(ItemMeta meta, List<String> lore) {
        try {
            if (HAS_MODERN_META) {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method textOf = componentClass.getMethod("text", Object.class);

                List<Object> components = new ArrayList<>();
                for (String line : lore) {
                    components.add(textOf.invoke(null, line));
                }

                Method loreMethod = ItemMeta.class.getMethod("lore", List.class);
                loreMethod.invoke(meta, components);
            } else {
                throw new NoSuchMethodException("fallback");
            }
        } catch (Exception e) {
            try {
                Method setLore = ItemMeta.class.getMethod("setLore", List.class);
                setLore.invoke(meta, lore);
            } catch (Exception ex) {
                meta.setLore(lore);
            }
        }
    }
}
