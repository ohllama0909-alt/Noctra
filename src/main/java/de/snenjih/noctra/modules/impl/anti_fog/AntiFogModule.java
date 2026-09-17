package de.snenjih.noctra.modules.impl.anti_fog;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class AntiFogModule extends BaseModule {

    public static AntiFogModule INSTANCE = null;

    public final ModuleSetting<Float>   fogStart;
    public final ModuleSetting<Float>   fogEnd;
    public final ModuleSetting<Boolean> disableLavaFog;
    public final ModuleSetting<Boolean> disableWaterFog;
    public final ModuleSetting<Boolean> disableBlindnessFog;

    public AntiFogModule() {
        super(
            "anti_fog",
            "Anti Fog",
            "Reduces or removes world fog to increase render visibility.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/anti_fog")
        );
        fogStart            = addSetting(new FloatSetting  ("fog_start",             "Fog Start",             900.0f, 0.0f,  1000.0f));
        fogEnd              = addSetting(new FloatSetting  ("fog_end",               "Fog End",               1000.0f, 1.0f, 1000.0f));
        disableLavaFog      = addSetting(new BooleanSetting("disable_lava_fog",      "Disable Lava Fog",      true));
        disableWaterFog     = addSetting(new BooleanSetting("disable_water_fog",     "Disable Water Fog",     false));
        disableBlindnessFog = addSetting(new BooleanSetting("disable_blindness_fog", "Disable Blindness Fog", true));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }
}
