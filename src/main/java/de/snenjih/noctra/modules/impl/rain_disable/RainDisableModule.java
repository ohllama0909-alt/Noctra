package de.snenjih.noctra.modules.impl.rain_disable;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class RainDisableModule extends BaseModule {

    public final ModuleSetting<Boolean> alsoDisableThunder;

    public RainDisableModule() {
        super(
            "rain_disable",
            "Rain Disable",
            "Disables rain and snow rendering client-side without affecting server weather.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/rain_disable")
        );
        alsoDisableThunder = addSetting(new BooleanSetting("also_disable_thunder", "Disable Thunder Effect", true));
    }
}
