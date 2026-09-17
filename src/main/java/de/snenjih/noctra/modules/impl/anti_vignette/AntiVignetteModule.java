package de.snenjih.noctra.modules.impl.anti_vignette;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class AntiVignetteModule extends BaseModule {

    public static AntiVignetteModule INSTANCE = null;

    public final ModuleSetting<Boolean> disableUnderwater;
    public final ModuleSetting<Boolean> disablePumpkin;

    public AntiVignetteModule() {
        super(
            "anti_vignette",
            "Anti Vignette",
            "Disables the screen edge vignette and optional overlays.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/anti_vignette")
        );
        disableUnderwater = addSetting(new BooleanSetting("disable_underwater", "Disable Underwater Overlay", false));
        disablePumpkin    = addSetting(new BooleanSetting("disable_pumpkin",    "Disable Pumpkin Overlay",    false));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }
}
