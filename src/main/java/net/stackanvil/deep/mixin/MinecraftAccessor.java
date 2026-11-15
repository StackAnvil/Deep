package net.stackanvil.deep.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    int deep$rightClickDelay();

    @Accessor("rightClickDelay")
    void deep$setRightClickDelay(int delay);

    @Mutable
    @Accessor("user")
    void deep$setUser(User user);

    @Mutable
    @Accessor("userApiService")
    void deep$setUserApiService(UserApiService userApiService);

    @Mutable
    @Accessor("profileKeyPairManager")
    void deep$setProfileKeyPairManager(ProfileKeyPairManager profileKeyPairManager);

    @Mutable
    @Accessor("profileFuture")
    void deep$setProfileFuture(CompletableFuture<ProfileResult> profileFuture);

    @Accessor("reportingContext")
    void deep$setReportingContext(ReportingContext reportingContext);
}
