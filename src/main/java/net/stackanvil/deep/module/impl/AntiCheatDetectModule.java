package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.server.ServerObserver;
import net.stackanvil.deep.server.ServerProfileStore;
import net.stackanvil.deep.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AntiCheatDetectModule extends Module {

    private final BooleanSetting storeProfile = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("store_profile")
            .name("Store Profile")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showUnknown = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_unknown")
            .name("Unknown")
            .defaultValue(false)
            .build()));
    private String lastAlert = "";

    public AntiCheatDetectModule() {
        super("anti_cheat_detect", "AntiCheat Detect", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.lastAlert = "";
    }

    @Override
    public void tick(final Minecraft client) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        String antiCheat = snapshot.antiCheat().orElse("");
        if (antiCheat.isBlank() || antiCheat.equals(this.lastAlert) || "Unknown".equals(antiCheat) && !this.showUnknown.value()) {
            return;
        }
        this.lastAlert = antiCheat;
        if (client.player != null) {
            String labels = snapshot.environmentLabels().isEmpty() ? "" : " " + snapshot.environmentLabels();
            client.player.sendSystemMessage(Component.literal(
                    "Detected anti-cheat: " + antiCheat + " (safety " + snapshot.safetyScore() + "/100)." + labels
            ));
        }
        if (this.storeProfile.value()) {
            ServerProfileStore.recordAntiCheat(snapshot.rootDomain(), antiCheat);
        }
    }
}
