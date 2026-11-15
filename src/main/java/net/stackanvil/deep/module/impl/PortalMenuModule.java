package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;

public final class PortalMenuModule extends Module {

    private static boolean active;

    public PortalMenuModule() {
        super("portal_menu", "Portal Menu", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static boolean active() {
        return active;
    }
}
