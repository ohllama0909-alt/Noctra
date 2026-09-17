package de.snenjih.noctra.modules.impl.zoom;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.input.KeybindManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ZoomModule extends BaseModule {

    public static ZoomModule INSTANCE = null;

    private final ModuleSetting<Float>   zoomFov;
    private final ModuleSetting<Boolean> smoothZoom;
    private final ModuleSetting<Float>   smoothSpeed;
    private final ModuleSetting<Float>   scrollSensitivity;
    private final ModuleSetting<Boolean> cinematicCam;

    private final KeyBinding zoomKey;

    private boolean zoomActive  = false;
    private double  currentFov  = 70.0;
    public  double  targetFov   = 70.0;
    private double  baseFov     = 70.0;
    private boolean savedCinematic = false;

    public ZoomModule() {
        super(
            "zoom",
            "Zoom",
            "Optifine-like zoom activated by holding a key.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/zoom")
        );
        zoomFov           = addSetting(new FloatSetting("zoom_fov",           "Zoom FOV",            15.0f, 1.0f,  60.0f));
        smoothZoom        = addSetting(new BooleanSetting("smooth_zoom",       "Smooth Zoom",         true));
        smoothSpeed       = addSetting(new FloatSetting("smooth_speed",        "Smooth Speed",        10.0f, 1.0f,  30.0f));
        scrollSensitivity = addSetting(new FloatSetting("scroll_sensitivity",  "Scroll Sensitivity",  1.0f,  0.1f,  5.0f));
        cinematicCam      = addSetting(new BooleanSetting("cinematic_cam",     "Cinematic Camera",    false));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.noctra.zoom",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KeybindManager.CATEGORY
        ));
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            baseFov    = mc.options.getFov().getValue();
            currentFov = baseFov;
            targetFov  = baseFov;
        }
    }

    @Override
    public void onDisable() {
        INSTANCE   = null;
        zoomActive = false;
        currentFov = baseFov;
        targetFov  = baseFov;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && cinematicCam.get()) {
            mc.options.smoothCameraEnabled = savedCinematic;
        }
    }

    @Override
    public void onLeaveWorld() {
        zoomActive = false;
        currentFov = baseFov;
        targetFov  = baseFov;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean shouldZoom = zoomKey.isPressed();
        if (shouldZoom != zoomActive) {
            zoomActive = shouldZoom;
            if (!zoomActive) {
                targetFov = baseFov;
                if (cinematicCam.get()) client.options.smoothCameraEnabled = savedCinematic;
            } else {
                baseFov   = client.options.getFov().getValue();
                targetFov = zoomFov.get().doubleValue();
                if (cinematicCam.get()) {
                    savedCinematic = client.options.smoothCameraEnabled;
                    client.options.smoothCameraEnabled = true;
                }
            }
        }

        if (smoothZoom.get()) {
            double speed = smoothSpeed.get() / 100.0 * 2.0;
            currentFov += (targetFov - currentFov) * speed;
            if (Math.abs(currentFov - targetFov) < 0.01) currentFov = targetFov;
        } else {
            currentFov = targetFov;
        }
    }

    public double getCurrentFov()  { return currentFov; }
    public boolean isZoomActive()  { return zoomActive; }

    public void adjustZoom(double scrollDelta) {
        double delta = scrollDelta * -scrollSensitivity.get();
        targetFov = Math.clamp(targetFov + delta, 1.0, 60.0);
    }
}
