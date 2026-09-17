package de.snenjih.noctra.modules.impl.anti_afk;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.JumpingMount;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class AntiAfkModule extends BaseModule {

    public enum Action { ROTATE, JUMP, SNEAK, SWING }

    private final ModuleSetting<Integer> interval;
    private final ModuleSetting<Action>  action;
    private final ModuleSetting<Boolean> randomOffset;
    private final ModuleSetting<Boolean> showIndicator;

    private int     tickCounter   = 0;
    private int     intervalTicks = 0;
    private boolean sneakToggle   = false;
    private float   baseYaw       = 0f;

    public AntiAfkModule() {
        super(
            "anti_afk",
            "Anti AFK",
            "Performs small actions at regular intervals to prevent AFK kicks.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/anti_afk")
        );
        interval      = addSetting(new IntSetting("interval",       "Interval (s)",   60, 10, 600));
        action        = addSetting(new EnumSetting<>("action",       "Action",         Action.ROTATE, Action.class));
        randomOffset  = addSetting(new BooleanSetting("random_offset",  "Random Offset",  true));
        showIndicator = addSetting(new BooleanSetting("show_indicator", "Show Indicator", false));
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        sneakToggle = false;
        baseYaw     = 0f;
        recomputeInterval();
    }

    @Override
    public void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && sneakToggle) {
            client.player.setSneaking(false);
            sneakToggle = false;
        }
        tickCounter = 0;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (sneakToggle) {
            player.setSneaking(false);
            sneakToggle = false;
        }

        if (showIndicator.get() && tickCounter % 20 == 0) {
            int secondsLeft = Math.max(0, (intervalTicks - tickCounter) / 20);
            player.sendMessage(Text.literal("AFK: " + secondsLeft + "s"), true);
        }

        tickCounter++;
        if (tickCounter < intervalTicks) return;

        tickCounter = 0;
        recomputeInterval();
        performAction(client, player);
    }

    private void recomputeInterval() {
        int base = interval.get() * 20;
        if (randomOffset.get()) {
            int jitter = (int) (base * 0.1f * (Math.random() * 2 - 1));
            intervalTicks = Math.max(10 * 20, base + jitter);
        } else {
            intervalTicks = base;
        }
    }

    private void performAction(MinecraftClient client, ClientPlayerEntity player) {
        switch (action.get()) {
            case ROTATE -> {
                float delta = (baseYaw == 0f) ? 5.0f : -5.0f;
                player.setYaw(player.getYaw() + delta);
                baseYaw = (baseYaw == 0f) ? 5.0f : 0f;
            }
            case JUMP -> {
                if (player.hasVehicle()) {
                    if (player.getVehicle() instanceof JumpingMount mount) {
                        mount.setJumpStrength(1);
                    }
                } else if (player.isOnGround()) {
                    player.jump();
                }
            }
            case SNEAK -> {
                player.setSneaking(true);
                sneakToggle = true;
            }
            case SWING -> player.swingHand(Hand.MAIN_HAND);
        }
    }
}
