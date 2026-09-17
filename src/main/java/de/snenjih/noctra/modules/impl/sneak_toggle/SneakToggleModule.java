package de.snenjih.noctra.modules.impl.sneak_toggle;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class SneakToggleModule extends BaseModule {

    private final ModuleSetting<Boolean> disableOnSneakPress;
    private final ModuleSetting<Boolean> disableOnSprint;

    private boolean sneakActive = false;
    private boolean sneakKeyWasPressed = false;

    public SneakToggleModule() {
        super(
            "sneak_toggle",
            "Sneak Toggle",
            "Toggle sneaking permanently without holding the Shift key.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/sneak_toggle")
        );
        disableOnSneakPress = addSetting(new BooleanSetting("disable_on_sneak_press", "Disable on Sneak Press", true));
        disableOnSprint     = addSetting(new BooleanSetting("disable_on_sprint",      "Disable on Sprint",      true));
    }

    @Override
    public void onEnable() {
        sneakActive = true;
        sneakKeyWasPressed = false;
    }

    @Override
    public void onDisable() {
        sneakActive = false;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean sneakKeyNowPressed = client.options.sneakKey.isPressed();

        if (disableOnSneakPress.get() && sneakKeyNowPressed && !sneakKeyWasPressed) {
            sneakActive = !sneakActive;
        }

        if (disableOnSprint.get() && client.player.isSprinting()) {
            sneakActive = false;
        }

        sneakKeyWasPressed = sneakKeyNowPressed;
    }

    public boolean isSneakActive() {
        return sneakActive;
    }
}
