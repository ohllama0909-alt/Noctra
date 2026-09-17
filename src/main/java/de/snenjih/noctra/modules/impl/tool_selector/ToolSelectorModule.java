package de.snenjih.noctra.modules.impl.tool_selector;

import de.snenjih.noctra.mixin.accessor.PlayerInventoryAccessor;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ToolSelectorModule extends BaseModule {

    private final ModuleSetting<Boolean> autoRestore;
    private final ModuleSetting<Boolean> requireHold;
    private final ModuleSetting<Float> minSpeedGain;

    private int originalSlot = -1;
    private int lastSwitchedSlot = -1;
    private boolean wasTargetingBlock = false;

    public ToolSelectorModule() {
        super(
            "tool_selector",
            "Tool Selector",
            "Automatically switches to the best tool in your hotbar for the targeted block.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/tool_selector")
        );
        autoRestore  = addSetting(new BooleanSetting("auto_restore",   "Restore Slot",      true));
        requireHold  = addSetting(new BooleanSetting("require_hold",   "Only While Mining", false));
        minSpeedGain = addSetting(new FloatSetting(  "min_speed_gain", "Min Speed Gain",    1.5f, 1.0f, 10.0f));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.currentScreen != null) {
            maybeRestore(client, player);
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            maybeRestore(client, player);
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            if (wasTargetingBlock) {
                maybeRestore(client, player);
                wasTargetingBlock = false;
            }
            return;
        }

        BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
        BlockState blockState = client.world.getBlockState(blockPos);

        if (blockState.isAir() || blockState.getHardness(client.world, blockPos) < 0) {
            if (wasTargetingBlock) maybeRestore(client, player);
            wasTargetingBlock = false;
            return;
        }

        if (requireHold.get() && !client.options.attackKey.isPressed()) {
            if (wasTargetingBlock && lastSwitchedSlot != -1) maybeRestore(client, player);
            return;
        }

        wasTargetingBlock = true;

        int currentSlot = player.getInventory().getSelectedSlot();
        if (lastSwitchedSlot != -1 && currentSlot != lastSwitchedSlot) {
            originalSlot = -1;
            lastSwitchedSlot = -1;
        }

        int bestSlot = findBestTool(player, blockState);
        if (bestSlot == -1 || bestSlot == currentSlot) return;

        float currentSpeed = getSpeed(player.getInventory().getStack(currentSlot), blockState);
        float bestSpeed    = getSpeed(player.getInventory().getStack(bestSlot), blockState);

        if (bestSpeed < currentSpeed * minSpeedGain.get()) return;

        if (originalSlot == -1) originalSlot = currentSlot;

        switchHotbarTo(client, player, bestSlot);
        lastSwitchedSlot = bestSlot;
    }

    private void maybeRestore(MinecraftClient client, ClientPlayerEntity player) {
        if (autoRestore.get() && originalSlot != -1 && player != null) {
            switchHotbarTo(client, player, originalSlot);
        }
        clearState();
    }

    private void clearState() {
        originalSlot = -1;
        lastSwitchedSlot = -1;
        wasTargetingBlock = false;
    }

    private int findBestTool(ClientPlayerEntity player, BlockState blockState) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            float speed = getSpeed(stack, blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private float getSpeed(ItemStack stack, BlockState blockState) {
        return stack.getMiningSpeedMultiplier(blockState);
    }

    private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slotIndex) {
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex));
        ((PlayerInventoryAccessor) player.getInventory()).setSelectedSlot(slotIndex);
    }

    @Override
    public void onDisable() {
        clearState();
    }
}
