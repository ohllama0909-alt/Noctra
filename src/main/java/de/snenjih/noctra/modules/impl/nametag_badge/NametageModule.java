package de.snenjih.noctra.modules.impl.nametag_badge;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class NametageModule extends BaseModule {

    public static NametageModule INSTANCE;

    final ModuleSetting<Boolean> showInTablist;
    final ModuleSetting<Integer> badgeColor;

    public NametageModule() {
        super(
            "nametag_badge",
            "Nametag Badge",
            "Shows a ✦ symbol before your name in nametags and the tablist.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/nametag_badge")
        );
        INSTANCE = this;
        showInTablist = addSetting(new BooleanSetting("show_in_tablist", "Show in Tablist", true));
        badgeColor    = addSetting(new ColorSetting("badge_color", "Badge Color", 0xFFFFD700));
    }

    public static boolean isBadgeActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static int getBadgeColor() {
        return INSTANCE != null ? INSTANCE.badgeColor.get() : 0xFFFFD700;
    }

    public static boolean isShowInTablist() {
        return INSTANCE != null && INSTANCE.showInTablist.get();
    }
}
