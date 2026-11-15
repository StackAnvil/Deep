package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class SilentDisconnectModule extends Module {

    private static boolean active;

    public SilentDisconnectModule() {
        super("silent_disconnect", "Silent Disconnect", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static boolean shouldSuppress(final Minecraft client) {
        return active && client != null && client.level != null && client.player != null;
    }
}
