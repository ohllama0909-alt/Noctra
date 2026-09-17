package de.snenjih.noctra.modules.impl.elytra_landing_swap;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class ElytraLandingSwapModule extends BaseModule {

    private static final int CHEST_SLOT = 6;

    private final ModuleSetting<Float>   triggerSpeed;
    private final ModuleSetting<Float>   lookaheadDistance;
    private final ModuleSetting<Boolean> swapBack;

    private boolean swapTriggered = false;
    private boolean wasGliding    = false;
    private int     chestSlotUsed = -1;

    public ElytraLandingSwapModule() {
        super(
            "elytra_landing_swap",
            "Landing Swap",
            "Automatically swaps elytra for chestplate before landing.",
            ModuleCategory.ELYTRA,
            Identifier.of("noctra", "modules/elytra_landing_swap")
        );
        triggerSpeed      = addSetting(new FloatSetting("trigger_speed",      "Trigger Speed (b/s)",      8.0f, 1.0f, 30.0f));
        lookaheadDistance = addSetting(new FloatSetting("lookahead_distance", "Look-ahead Distance (m)",  6.0f, 1.0f, 20.0f));
        swapBack          = addSetting(new BooleanSetting("swap_back",        "Auto Swap Back",           false));
    }

    @Override
    public void onClientTick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        if (player == null || world == null) return;

        boolean gliding = player.isGliding();

        // Reset: landed after gliding
        if (!gliding && wasGliding) {
            wasGliding    = false;
            swapTriggered = false;
            if (swapBack.get() && chestSlotUsed != -1 && player.isOnGround()) {
                performLandingSwap(mc, player, chestSlotUsed);
                chestSlotUsed = -1;
            }
            return;
        }

        // New glide session started
        if (gliding && !wasGliding) {
            wasGliding    = true;
            swapTriggered = false;
            chestSlotUsed = -1;
        }

        // Only trigger if currently gliding and not already triggered
        if (!gliding || swapTriggered) return;

        // Check fall speed
        Vec3d vel    = player.getVelocity();
        double vSpeed = -vel.y * 20.0;
        if (vSpeed < triggerSpeed.get()) return;

        // Check ground proximity via raycast
        if (!isGroundWithin(world, player, lookaheadDistance.get())) return;

        // No swap with screen open
        if (mc.currentScreen != null) return;

        // Creative mode guard
        if (player.isCreative()) return;

        // Find chestplate in inventory
        int chestplateSlot = findChestplate(player);
        if (chestplateSlot == -1) return;

        // Perform swap
        performLandingSwap(mc, player, chestplateSlot);
        swapTriggered = true;
        chestSlotUsed = chestplateSlot;
    }

    private boolean isGroundWithin(ClientWorld world, ClientPlayerEntity player, float maxDist) {
        Vec3d start = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d end   = start.add(0, -maxDist, 0);
        RaycastContext ctx = new RaycastContext(
            start, end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        );
        BlockHitResult hit = world.raycast(ctx);
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private int findChestplate(ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (isWearableChestplate(s) && !hasBindingCurse(s)) return i;
        }
        return -1;
    }

    private static boolean isWearableChestplate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
        return eq != null
            && eq.slot() == EquipmentSlot.CHEST
            && stack.getItem() != Items.ELYTRA;
    }

    private static boolean hasBindingCurse(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.getEnchantments();
        return enchantments.getEnchantments().stream()
            .anyMatch(e -> e.matchesKey(Enchantments.BINDING_CURSE));
    }

    private void performLandingSwap(MinecraftClient mc, ClientPlayerEntity player, int inventorySlot) {
        int syncId  = player.playerScreenHandler.syncId;
        // Hotbar slots 0-8 → PSH 36-44; main inventory 9-35 → PSH 9-35
        int pshSlot = (inventorySlot < 9) ? 36 + inventorySlot : inventorySlot;
        boolean chestWasEmpty = player.getEquippedStack(EquipmentSlot.CHEST).isEmpty();

        mc.interactionManager.clickSlot(syncId, pshSlot,    0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player);
        if (!chestWasEmpty) {
            mc.interactionManager.clickSlot(syncId, pshSlot, 0, SlotActionType.PICKUP, player);
        }
    }
}
