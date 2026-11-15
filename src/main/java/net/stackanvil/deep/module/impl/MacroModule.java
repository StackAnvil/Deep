package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.StringSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLScancode;

public final class MacroModule extends Module {

    private final StringSetting f6 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f6")
            .name("F6")
            .defaultValue("/home")
            .build()));
    private final StringSetting f7 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f7")
            .name("F7")
            .defaultValue("/spawn")
            .build()));
    private final StringSetting f8 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f8")
            .name("F8")
            .defaultValue("gg")
            .build()));
    private boolean f6Down;
    private boolean f7Down;
    private boolean f8Down;

    public MacroModule() {
        super("macros", "Macros", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        this.f6Down = this.handle(client, SDLScancode.SDL_SCANCODE_F6, this.f6.value(), this.f6Down);
        this.f7Down = this.handle(client, SDLScancode.SDL_SCANCODE_F7, this.f7.value(), this.f7Down);
        this.f8Down = this.handle(client, SDLScancode.SDL_SCANCODE_F8, this.f8.value(), this.f8Down);
    }

    private boolean handle(final Minecraft client, final int key, final String action, final boolean previous) {
        boolean pressed = InputStates.keyPressed(client, key);
        if (pressed && !previous) {
            ChatActions.send(client, action);
        }
        return pressed;
    }
}
