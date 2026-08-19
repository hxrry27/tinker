package dev.hxrry.tinker.property;

import org.bukkit.block.data.BlockData;

public interface TinkerProperty {

    Category category();

    String id();

    default String key() {
        return category().key() + "." + id();
    }

    boolean appliesTo(BlockData data);

    String render(BlockData data);

    BlockData cycle(BlockData data, int direction);
}
