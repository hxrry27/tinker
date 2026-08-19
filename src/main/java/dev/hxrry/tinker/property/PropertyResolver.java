package dev.hxrry.tinker.property;

import dev.hxrry.tinker.config.TinkerConfig;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class PropertyResolver {

    private final Supplier<TinkerConfig> config;

    public PropertyResolver(Supplier<TinkerConfig> config) {
        this.config = config;
    }

    public List<TinkerProperty> editable(BlockData data) {
        TinkerConfig current = config.get();
        List<TinkerProperty> editable = new ArrayList<>();
        for (TinkerProperty property : PropertyRegistry.all()) {
            if (current.isAllowed(property) && property.appliesTo(data)) {
                editable.add(property);
            }
        }
        return editable;
    }

    public TinkerProperty find(BlockData data, Category category, String id) {
        TinkerConfig current = config.get();
        for (TinkerProperty property : PropertyRegistry.all()) {
            if (property.category() == category
                    && property.id().equals(id)
                    && property.appliesTo(data)
                    && current.isAllowed(property)) {
                return property;
            }
        }
        return null;
    }

    public static int indexOf(List<TinkerProperty> properties, String key) {
        if (key == null) {
            return -1;
        }
        for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).key().equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
