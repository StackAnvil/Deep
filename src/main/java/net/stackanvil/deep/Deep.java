package net.stackanvil.deep;

import net.stackanvil.deep.command.DeepCommands;
import net.stackanvil.deep.config.ClientConfig;
import net.stackanvil.deep.config.ConfigBackups;
import net.stackanvil.deep.event.ClientTickEvent;
import net.stackanvil.deep.event.HudRenderEvent;
import net.stackanvil.deep.event.WorldRenderEvent;
import net.stackanvil.deep.friends.FriendManager;
import net.stackanvil.deep.inventory.InventoryActionScheduler;
import net.stackanvil.deep.inventory.SilentHotbar;
import net.stackanvil.deep.module.ModuleManager;
import net.stackanvil.deep.module.ModuleRegistry;
import net.stackanvil.deep.notification.ToggleNotifications;
import net.stackanvil.deep.module.impl.EspOutlineRegistry;
import net.stackanvil.deep.profile.ProfileManager;
import net.stackanvil.deep.render.MarkerManager;
import net.stackanvil.deep.rivet.DeepRenderPipelines;
import net.stackanvil.deep.rotation.RotationManager;
import net.stackanvil.deep.target.RenderedEntityCache;
import net.stackanvil.deep.timer.TimerManager;
import net.stackanvil.deep.ui.DeepScreen;
import net.stackanvil.deep.ui.HudLayout;
import net.stackanvil.deep.waypoint.WaypointStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;

public final class Deep implements ClientModInitializer {

    public static final String MOD_ID = "deep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModuleManager MODULES = new ModuleManager();
    public static final FriendManager FRIENDS = new FriendManager(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "-friends.txt")
    );
    public static final ProfileManager PROFILES = new ProfileManager(ProfileManager.defaultPath());
    public static final WaypointStore WAYPOINTS = new WaypointStore(WaypointStore.defaultPath());
    public static final ClientConfig CONFIG = new ClientConfig(MODULES, FRIENDS);
    private static final Identifier HUD_MODULES_ID = Identifier.fromNamespaceAndPath(MOD_ID, "module_hud");

    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        DeepRenderPipelines.initialize();
        backupConfig();
        ModuleRegistry.registerDefaults(MODULES);
        FRIENDS.load();
        PROFILES.load();
        WAYPOINTS.load();
        CONFIG.load();
        HudLayout.load();

        this.openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.deep.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                Category.MISC
        ));
        DeepCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            EspOutlineRegistry.clear();
            MODULES.call(new WorldRenderEvent(context));
            MarkerManager.render(context);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, HUD_MODULES_ID,
                (graphics, deltaTracker) -> {
                    MODULES.call(new HudRenderEvent(Minecraft.getInstance(), graphics));
                    ToggleNotifications.render(Minecraft.getInstance(), graphics);
                });
    }

    private static void backupConfig() {
        try {
            ConfigBackups.defaults().backup(Clock.systemDefaultZone())
                    .ifPresent(archive -> LOGGER.info("Backed up Deep config to {}", archive));
        } catch (IOException exception) {
            LOGGER.warn("Failed to back up Deep config", exception);
        }
    }

    private void onClientTick(final Minecraft client) {
        while (this.openMenuKey.consumeClick()) {
            client.gui.setScreen(new DeepScreen(MODULES, CONFIG));
        }
        RenderedEntityCache.refresh(client);
        RotationManager.tick();
        TimerManager.tick();
        MarkerManager.tick();
        SilentHotbar.tick(client);
        MODULES.handleKeybinds(client);
        MODULES.call(new ClientTickEvent(client));
        InventoryActionScheduler.tick(client);
    }
}
