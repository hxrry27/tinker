package dev.hxrry.tinker.session;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;


public final class PlayerSession {

    private boolean tinkerMode;

    private final Map<Material, String> selection = new HashMap<>();

    public boolean tinkerMode() {
        return tinkerMode;
    }

    public void tinkerMode(boolean tinkerMode) {
        this.tinkerMode = tinkerMode;
    }

    public String selected(Material material) {
        return selection.get(material);
    }

    public void select(Material material, String propertyKey) {
        selection.put(material, propertyKey);
    }

}
