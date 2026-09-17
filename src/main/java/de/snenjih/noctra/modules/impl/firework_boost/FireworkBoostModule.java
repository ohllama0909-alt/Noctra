package de.snenjih.noctra.modules.impl.firework_boost;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class FireworkBoostModule extends BaseModule {

    private final ModuleSetting<Float>   minSpeed;
    private final ModuleSetting<Integer> cooldownTicks;
    private final ModuleSetting<Boolean> preferHotbar;

    private int cooldownRemaining = 0;

    public FireworkBoostModule() {
        super(
            "firework_boost",
            "Firework Boost",
            "Automatically uses firework rockets to maintain glide speed.",
            ModuleCategory.ELYTRA,
            Identifier.of("noctra", "modules/firework_boost")
        );
        minSpeed      = addSetting(new FloatSetting  ("min_speed",       "Min Speed (b/s)",       10.0f, 1.0f, 50.0f));
        cooldownTicks = addSetting(new IntSetting    ("cooldown_ticks",  "Boost Cooldown (ticks)", 20,    1,   200));
        preferHotbar  = addSetting(new BooleanSetting("prefer_hotbar",  "Prefer Hotbar",           true));
    }

    @Override
    public void onEnable() {
        cooldownRemaining = 0;
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.currentScreen != null) return;

        ClientPlayerEntity player = client.player;

        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return;
        }

        if (!player.isGliding()) return;

        Vec3d vel = player.getVelocity();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0;

        if (hSpeed >= minSpeed.get()) return;

        int rocketSlot = findRocket(player, preferHotbar.get());
        if (rocketSlot == -1) {
            player.sendMessage(Text.translatable("noctra.firework_boost.no_rockets"), true);
            cooldownRemaining = 40;
            return;
        }

        triggerBoost(client, player, rocketSlot);
        cooldownRemaining = cooldownTicks.get();
    }

    /**
     * Finds a firework rocket in the player's inventory.
     * Returns the PlayerInventory index (0-8 hotbar, 9-35 main), or -1 if not found.
     */
    private int findRocket(ClientPlayerEntity player, boolean preferHotbar) {
        // Search order based on preference
        if (preferHotbar) {
            // Search hotbar first (0-8), then main inventory (9-35)
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return i;
            }
            for (int i = 9; i < 36; i++) {
                if (player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return i;
            }
        } else {
            // Search main inventory first (9-35), then hotbar (0-8)
            for (int i = 9; i < 36; i++) {
                if (player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return i;
            }
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return i;
            }
        }
        return -1;
    }

    /**
     * Triggers a firework boost using the rocket at the given inventory slot index.
     */
    private void triggerBoost(MinecraftClient mc, ClientPlayerEntity player, int inventorySlot) {
        int syncId = player.playerScreenHandler.syncId;
        int hotbarIndex = player.getInventory().getSelectedSlot(); // 0-8

        // Convert PlayerInventory index to PlayerScreenHandler slot:
        // Hotbar (0-8) → PSH slots 36-44
        // Main inventory (9-35) → PSH slots 9-35
        int pshSlot = (inventorySlot < 9) ? 36 + inventorySlot : inventorySlot;
        int activeAbsSlot = 36 + hotbarIndex;

        if (pshSlot == activeAbsSlot) {
            // Rocket is already in active slot — use it directly
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
        } else {
            // Swap rocket into active hotbar slot using SWAP action
            mc.interactionManager.clickSlot(syncId, pshSlot, hotbarIndex, SlotActionType.SWAP, player);
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            // Swap back to restore original positions (rocket is now consumed or still there)
            mc.interactionManager.clickSlot(syncId, pshSlot, hotbarIndex, SlotActionType.SWAP, player);
        }
    }
}
