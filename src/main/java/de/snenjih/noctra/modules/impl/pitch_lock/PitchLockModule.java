package de.snenjih.noctra.modules.impl.pitch_lock;

import de.snenjih.noctra.input.KeybindManager;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PitchLockModule extends BaseModule {

    private final ModuleSetting<Float>   targetPitch;
    private final ModuleSetting<Float>   lockStrength;
    private final ModuleSetting<Boolean> onlyWhileGliding;

    private KeyBinding lockKeybind;
    private boolean lockActive = false;

    public PitchLockModule() {
        super(
            "pitch_lock",
            "Pitch Lock",
            "Locks your view angle while gliding for consistent travel speed.",
            ModuleCategory.ELYTRA,
            Identifier.of("noctra", "modules/pitch_lock")
        );
        targetPitch      = addSetting(new FloatSetting  ("target_pitch",       "Target Pitch (°)",    -29.0f, -90.0f, 90.0f));
        lockStrength     = addSetting(new FloatSetting  ("lock_strength",       "Lock Strength",        1.0f,   0.1f,  1.0f));
        onlyWhileGliding = addSetting(new BooleanSetting("only_while_gliding", "Only While Gliding",   true));
    }

    @Override
    public void onEnable() {
        lockActive = false;
        if (lockKeybind == null) {
            lockKeybind = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                    "key.noctra.pitch_lock_toggle",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    KeybindManager.CATEGORY
                )
            );
        }
    }

    @Override
    public void onDisable() {
        lockActive = false;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.currentScreen != null) return;
        if (client.player.isSpectator()) return;

        // Check keybind for internal toggle
        if (lockKeybind != null) {
            while (lockKeybind.wasPressed()) {
                lockActive = !lockActive;
            }
        }

        if (!lockActive) return;
        if (onlyWhileGliding.get() && !client.player.isGliding()) return;

        float currentPitch = client.player.getPitch();
        float target = targetPitch.get();
        float strength = lockStrength.get();
        float newPitch = currentPitch + (target - currentPitch) * strength;

        // Clamp to valid range
        newPitch = Math.clamp(newPitch, -90f, 90f);
        client.player.setPitch(newPitch);
    }
}
