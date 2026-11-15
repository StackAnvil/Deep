package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.BooleanSetting;
import net.stackanvil.deep.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public final class AutoSignModule extends Module {

    private final StringSetting line1 = this.setting(StringSetting.from(StringSetting.builder().id("line_1").name("Line 1").defaultValue("Deep").build()));
    private final StringSetting line2 = this.setting(StringSetting.from(StringSetting.builder().id("line_2").name("Line 2").defaultValue("").build()));
    private final StringSetting line3 = this.setting(StringSetting.from(StringSetting.builder().id("line_3").name("Line 3").defaultValue("").build()));
    private final StringSetting line4 = this.setting(StringSetting.from(StringSetting.builder().id("line_4").name("Line 4").defaultValue("").build()));
    private final BooleanSetting front = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("front")
            .name("Front")
            .defaultValue(true)
            .build()));
    private BlockPos lastUpdated;

    public AutoSignModule() {
        super("auto_sign", "Auto Sign", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || client.level == null
                || client.getConnection() == null
                || !(client.level.getBlockState(hit.getBlockPos()).getBlock() instanceof SignBlock)
                || hit.getBlockPos().equals(this.lastUpdated)) {
            return;
        }
        client.getConnection().send(new ServerboundSignUpdatePacket(
                hit.getBlockPos(),
                List.of(
                        trim(this.line1.value()),
                        trim(this.line2.value()),
                        trim(this.line3.value()),
                        trim(this.line4.value())
                ),
                this.front.value() ? SignTextSlot.FRONT : SignTextSlot.BACK
        ));
        this.lastUpdated = hit.getBlockPos().immutable();
    }

    static String trim(final String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 384 ? value.substring(0, 384) : value;
    }
}
