package de.snenjih.noctra.modules.impl.auto_shield;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AutoShieldModule extends BaseModule {

    private final ModuleSetting<Boolean> reactToArrows;
    private final ModuleSetting<Boolean> reactToMelee;
    private final ModuleSetting<Integer> holdDuration;
    private final ModuleSetting<Float>   arrowDetectionRadius;

    private int     shieldHoldTicksRemaining = 0;
    private boolean isShieldActive           = false;

    public AutoShieldModule() {
        super(
            "auto_shield",
            "Auto Shield",
            "Automatically raises your shield when attacked or an arrow approaches.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/auto_shield")
        );
        reactToArrows        = addSetting(new BooleanSetting("react_to_arrows",        "React to Arrows",        true));
        reactToMelee         = addSetting(new BooleanSetting("react_to_melee",         "React to Melee",         true));
        holdDuration         = addSetting(new IntSetting    ("hold_duration",           "Hold Duration (ticks)",  10, 1, 40));
        arrowDetectionRadius = addSetting(new FloatSetting  ("arrow_detection_radius",  "Arrow Radius",           8.0f, 2.0f, 20.0f));
    }

    @Override
    public void onEnable() {
        shieldHoldTicksRemaining = 0;
        isShieldActive = false;
    }

    @Override
    public void onDisable() {
        if (isShieldActive) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                deactivateShield(mc.player);
            }
        }
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (client.currentScreen != null) return;
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isOf(Items.SHIELD)) return;

        // Check if shield is on cooldown
        if (player.getItemCooldownManager().isCoolingDown(offhand)) {
            isShieldActive = false;
            return;
        }

        boolean threatened = false;

        // Melee detection: hurtTime > 0 means player was recently hit
        if (reactToMelee.get() && player.hurtTime > 0) {
            threatened = true;
        }

        // Arrow detection: look for approaching projectiles
        if (reactToArrows.get() && !threatened && client.world != null) {
            float radius = arrowDetectionRadius.get();
            Box searchBox = player.getBoundingBox().expand(radius);
            List<PersistentProjectileEntity> nearby = client.world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                searchBox,
                e -> e.isAlive() && !e.isRemoved()
            );
            for (PersistentProjectileEntity proj : nearby) {
                if (isProjectileApproaching(proj, player)) {
                    threatened = true;
                    break;
                }
            }
        }

        if (threatened) {
            shieldHoldTicksRemaining = holdDuration.get();
            if (!isShieldActive && !player.isBlocking()) {
                activateShield(client, player);
            }
        }

        if (shieldHoldTicksRemaining > 0) {
            shieldHoldTicksRemaining--;
        } else if (isShieldActive) {
            deactivateShield(player);
        }
    }

    private boolean hasShieldInOffhand(ClientPlayerEntity player) {
        return player.getOffHandStack().isOf(Items.SHIELD);
    }

    private void activateShield(MinecraftClient mc, ClientPlayerEntity player) {
        ActionResult result = mc.interactionManager.interactItem(player, Hand.OFF_HAND);
        if (result != ActionResult.FAIL) {
            isShieldActive = true;
        }
    }

    private void deactivateShield(ClientPlayerEntity player) {
        player.stopUsingItem();
        isShieldActive = false;
    }

    private boolean isProjectileApproaching(PersistentProjectileEntity proj, ClientPlayerEntity player) {
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d projPos   = new Vec3d(proj.getX(), proj.getY(), proj.getZ());
        Vec3d toPlayer  = playerPos.subtract(projPos).normalize();
        Vec3d velocity  = proj.getVelocity().normalize();
        return toPlayer.dotProduct(velocity) > 0.5;
    }
}
