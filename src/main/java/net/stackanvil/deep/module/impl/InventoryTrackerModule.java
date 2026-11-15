package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.inventory.InventoryTracker;
import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class InventoryTrackerModule extends Module {

    private final NumberSetting retentionSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("retention_seconds")
            .name("Retain")
            .defaultValue(60.0)
            .min(5.0)
            .max(600.0)
            .step(5.0)
            .build()));

    public InventoryTrackerModule() {
        super("inventory_tracker", "Inventory Tracker", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        InventoryTracker.recordVisiblePlayers(client);
        InventoryTracker.pruneOlderThan(System.currentTimeMillis() - this.retentionSeconds.value().longValue() * 1000L);
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        InventoryTracker.clear();
    }

    @Override
    protected void onDisable() {
        InventoryTracker.clear();
    }
}
