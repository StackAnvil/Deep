package net.stackanvil.deep.ui;

import net.stackanvil.deep.setting.BooleanSetting;
import net.stackanvil.deep.setting.NumberSetting;
import net.stackanvil.deep.setting.SelectSetting;
import net.stackanvil.deep.setting.Setting;
import net.stackanvil.deep.setting.TextValueSetting;
import net.lenni0451.commons.math.MathUtils;

import java.math.BigDecimal;

final class SettingControls {

    private SettingControls() {
    }

    static String displayValue(final Setting<?> setting) {
        Object value = setting.value();
        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        if (setting instanceof TextValueSetting text) {
            return text.valueString();
        }
        return String.valueOf(value);
    }

    static boolean adjust(final Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            bool.value(!bool.value());
            return true;
        }
        if (setting instanceof NumberSetting number) {
            double next = number.value() + number.step();
            if (next > number.max()) {
                next = number.min();
            }
            number.value(next);
            return true;
        }
        if (setting instanceof SelectSetting select) {
            select.next();
            return true;
        }
        return false;
    }

    static void setNumberFromProgress(final NumberSetting setting, final double progress) {
        double clampedProgress = MathUtils.clamp(progress, 0, 1);
        double raw = setting.min() + (setting.max() - setting.min()) * clampedProgress;
        double step = setting.step();
        if (step > 0) {
            raw = setting.min() + Math.round((raw - setting.min()) / step) * step;
        }
        setting.value(raw);
    }
}
