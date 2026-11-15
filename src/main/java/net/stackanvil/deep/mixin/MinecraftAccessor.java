package net.stackanvil.deep.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    int deep$rightClickDelay();

    @Accessor("rightClickDelay")
    void deep$setRightClickDelay(int delay);
}
