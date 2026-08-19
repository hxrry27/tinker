package dev.hxrry.tinker.property;

import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class SimpleProperty<D extends BlockData, V> implements TinkerProperty {

    private static final List<Boolean> BOOLEANS = List.of(false, true);

    private final Category category;
    private final String id;
    private final Class<D> type;
    private final Predicate<D> gate;
    private final Function<D, V> getter;
    private final BiConsumer<D, V> setter;
    private final Function<D, List<V>> values;

    private SimpleProperty(Category category, String id, Class<D> type, Predicate<D> gate, Function<D, V> getter,
            BiConsumer<D, V> setter, Function<D, List<V>> values) {
        this.category = Objects.requireNonNull(category, "category");
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
        this.values = Objects.requireNonNull(values, "values");
    }

    public static <D extends BlockData, V> TinkerProperty of(Category category, String id, Class<D> type,
            Predicate<D> gate, Function<D, V> getter, BiConsumer<D, V> setter, Function<D, List<V>> values) {
        return new SimpleProperty<>(category, id, type, gate, getter, setter, values);
    }

    public static <D extends BlockData> TinkerProperty bool(Category category, String id, Class<D> type,
            Predicate<D> gate, Function<D, Boolean> getter, BiConsumer<D, Boolean> setter) {
        return new SimpleProperty<>(category, id, type, gate, getter, setter, data -> BOOLEANS);
    }

    public static <D extends BlockData, E extends Enum<E>> TinkerProperty enums(Category category, String id,
            Class<D> type, Class<E> enumType, Predicate<D> gate, Function<D, E> getter, BiConsumer<D, E> setter) {
        List<E> constants = List.of(enumType.getEnumConstants());
        return new SimpleProperty<>(category, id, type, gate, getter, setter, data -> constants);
    }

    public static <D extends BlockData> TinkerProperty ints(Category category, String id, Class<D> type,
            Predicate<D> gate, ToIntFunction<D> min, ToIntFunction<D> max, Function<D, Integer> getter,
            BiConsumer<D, Integer> setter) {
        return new SimpleProperty<>(category, id, type, gate, getter, setter, data -> {
            List<Integer> range = new ArrayList<>();
            for (int i = min.applyAsInt(data); i <= max.applyAsInt(data); i++) {
                range.add(i);
            }
            return List.copyOf(range);
        });
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean appliesTo(BlockData data) {
        return type.isInstance(data) && gate.test(type.cast(data));
    }

    @Override
    public String render(BlockData data) {
        return String.valueOf(getter.apply(type.cast(data)));
    }

    @Override
    public BlockData cycle(BlockData data, int direction) {
        D copy = type.cast(data.clone());
        List<V> candidates = values.apply(copy);
        if (candidates.isEmpty()) {
            return copy;
        }
        int current = candidates.indexOf(getter.apply(copy));
        int next = current < 0 ? 0 : Math.floorMod(current + direction, candidates.size());
        setter.accept(copy, candidates.get(next));
        return copy;
    }

    @Override
    public String toString() {
        return key();
    }
}
