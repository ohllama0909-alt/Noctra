package de.snenjih.noctra.modules.impl.boss_bar_customizer;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class BossBarCustomizerModule extends BaseModule {

    public static BossBarCustomizerModule INSTANCE = null;

    public final ModuleSetting<Boolean> hideBar;
    public final ModuleSetting<Integer> xOffset;
    public final ModuleSetting<Integer> yOffset;
    public final ModuleSetting<Float>   scale;
    public final ModuleSetting<Boolean> hideText;

    public BossBarCustomizerModule() {
        super(
            "boss_bar_customizer",
            "Boss Bar Customizer",
            "Move, scale, or hide the vanilla boss health bar.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/boss_bar_customizer")
        );
        hideBar  = addSetting(new BooleanSetting("hide_bar",  "Hide Boss Bar", false));
        xOffset  = addSetting(new IntSetting    ("x_offset",  "X Offset",      0, -500, 500));
        yOffset  = addSetting(new IntSetting    ("y_offset",  "Y Offset",      0, -400, 400));
        scale    = addSetting(new FloatSetting  ("scale",     "Scale",         1.0f, 0.25f, 2.0f));
        hideText = addSetting(new BooleanSetting("hide_text", "Hide Text",     false));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }
}
