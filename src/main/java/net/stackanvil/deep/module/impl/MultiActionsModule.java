package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.BooleanSetting;

public final class MultiActionsModule extends Module {

    private static MultiActionsModule instance;
    private final BooleanSetting attackWhileUsing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("attack_while_using")
            .name("Attack")
            .defaultValue(true)
            .build()));
    private final BooleanSetting useWhileBusy = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_while_busy")
            .name("Use")
            .defaultValue(true)
            .build()));
    private final BooleanSetting breakWhileUsing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("break_while_using")
            .name("Break")
            .defaultValue(true)
            .build()));
    private final BooleanSetting placeWhileBreaking = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place_while_breaking")
            .name("Place")
            .defaultValue(true)
            .build()));

    public MultiActionsModule() {
        super("multi_actions", "Multi Actions", ModuleCategory.MISC, java.util.List.of("multitask"));
        instance = this;
    }

    public static boolean attackWhileUsing() {
        return instance != null && instance.enabled() && instance.attackWhileUsing.value();
    }

    public static boolean useWhileBusy() {
        return instance != null && instance.enabled() && instance.useWhileBusy.value();
    }

    public static boolean breakWhileUsing() {
        return instance != null && instance.enabled() && instance.breakWhileUsing.value();
    }

    public static boolean placeWhileBreaking() {
        return instance != null && instance.enabled() && instance.placeWhileBreaking.value();
    }
}
