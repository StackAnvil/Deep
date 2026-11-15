package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.module.impl.BetterChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component deep$modifyChatMessage(final Component message) {
        return Deep.MODULES.chatMessage(Minecraft.getInstance(), message);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void deep$preventChatClear(final boolean history, final CallbackInfo info) {
        if (BetterChatModule.shouldPreventClearMessages()) {
            info.cancel();
        }
    }
}
