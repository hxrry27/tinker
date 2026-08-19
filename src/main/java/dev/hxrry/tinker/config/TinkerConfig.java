package dev.hxrry.tinker.config;

import dev.hxrry.tinker.property.Category;
import dev.hxrry.tinker.property.TinkerProperty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class TinkerConfig {

    private final boolean requireToolItem;
    private final Material toolItem;
    private final Set<String> allowedProperties;
    private final Set<Category> allowedCategories;
    private final Messages messages;

    private TinkerConfig(boolean requireToolItem,
            Material toolItem,
            Set<String> allowedProperties,
            Set<Category> allowedCategories,
            Messages messages) {
        this.requireToolItem = requireToolItem;
        this.toolItem = toolItem;
        this.allowedProperties = allowedProperties;
        this.allowedCategories = allowedCategories;
        this.messages = messages;
    }

    public static TinkerConfig load(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();

        boolean requireToolItem = config.getBoolean("tool.require-item", false);
        Material toolItem = resolveToolItem(plugin, config.getString("tool.item", "minecraft:golden_axe"));

        Set<String> allowedProperties = new HashSet<>();
        Set<Category> allowedCategories = EnumSet.noneOf(Category.class);
        readAllowlist(config, allowedProperties, allowedCategories);

        return new TinkerConfig(requireToolItem, toolItem,
                Set.copyOf(allowedProperties), Set.copyOf(allowedCategories),
                new Messages(plugin, config));
    }

    private static void readAllowlist(FileConfiguration config,
            Set<String> properties,
            Set<Category> categories) {
        ConfigurationSection allowlist = config.getConfigurationSection("allowlist");
        if (allowlist == null) {
            return;
        }
        for (Category category : Category.values()) {
            ConfigurationSection section = allowlist.getConfigurationSection(category.key());
            if (section == null) {
                continue;
            }
            for (String property : section.getKeys(false)) {
                if (section.getBoolean(property, false)) {
                    properties.add(category.key() + "." + property);
                    categories.add(category);
                }
            }
        }
    }

    private static Material resolveToolItem(Plugin plugin, String raw) {
        Material material = raw == null ? null : Material.matchMaterial(raw);
        if (material == null || !material.isItem()) {
            plugin.getLogger().warning("tool.item '" + raw + "' is not a valid item; falling back to golden_axe.");
            return Material.GOLDEN_AXE;
        }
        return material;
    }

    public boolean requireToolItem() {
        return requireToolItem;
    }

    public Material toolItem() {
        return toolItem;
    }

    public boolean isAllowed(TinkerProperty property) {
        return allowedProperties.contains(property.key());
    }

    public boolean isAllowed(Category category, String property) {
        return allowedProperties.contains(category.key() + "." + property);
    }

    public boolean isCategoryEnabled(Category category) {
        return allowedCategories.contains(category);
    }

    public Messages messages() {
        return messages;
    }
}
