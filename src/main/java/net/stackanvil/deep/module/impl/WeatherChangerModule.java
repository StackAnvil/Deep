package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.SelectSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class WeatherChangerModule extends Module {

    private final SelectSetting weather = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("weather")
            .name("Weather")
            .defaultValue("Clear")
            .addAllOptions(List.of("Clear", "Rain", "Thunder"))
            .build()));

    public WeatherChangerModule() {
        super("weather_changer", "Weather Changer", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level == null) {
            return;
        }
        float rain = switch (this.weather.value()) {
            case "Rain", "Thunder" -> 1.0F;
            default -> 0.0F;
        };
        float thunder = "Thunder".equals(this.weather.value()) ? 1.0F : 0.0F;
        client.level.setRainLevel(rain);
        client.level.setThunderLevel(thunder);
    }
}
