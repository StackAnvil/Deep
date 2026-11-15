package net.stackanvil.deep.setting;

import net.minecraft.world.item.Item;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface ItemListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<Item> defaultValue();

    List<String> aliases();
}
