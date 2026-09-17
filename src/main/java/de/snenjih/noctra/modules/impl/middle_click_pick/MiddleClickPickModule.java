package de.snenjih.noctra.modules.impl.middle_click_pick;

import de.snenjih.noctra.mixin.accessor.PlayerInventoryAccessor;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;

public class MiddleClickPickModule extends BaseModule {

    private final ModuleSetting<Boolean> searchInventory;
    private final ModuleSetting<Boolean> preferFullStack;
    private final ModuleSetting<Boolean> switchToExisting;

    public MiddleClickPickModule() {
        super(
            "middle_click_pick",
            "Middle Click Pick",
            "Pick up targeted blocks from your inventory by middle-clicking, like in Creative mode.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/middle_click_pick")
        );
        searchInventory  = addSetting(new BooleanSetting("search_inventory",  "Search Inventory",  true));
        preferFullStack  = addSetting(new BooleanSetting("prefer_full_stack",  "Prefer Full Stack", true));
        switchToExisting = addSetting(new BooleanSetting("switch_to_existing", "Switch Hotbar Slot", true));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) return;
        if (player.isCreative() || player.isSpectator()) return;

        if (!client.options.pickItemKey.wasPressed()) return;

        handleMiddleClick(client, player);
    }

    private void handleMiddleClick(MinecraftClient client, ClientPlayerEntity player) {
        if (client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        var blockState = client.world.getBlockState(hit.getBlockPos());
        if (blockState.isAir()) return;

        Item pickItem = blockState.getBlock().asItem();
        if (pickItem == Items.AIR) return;

        int currentHotbar = player.getInventory().getSelectedSlot();
        if (player.getInventory().getStack(currentHotbar).isOf(pickItem)) return;

        if (switchToExisting.get()) {
            int hotbarSlot = findInRange(player, pickItem, 0, 8);
            if (hotbarSlot != -1) {
                switchHotbarTo(player, hotbarSlot);
                return;
            }
        }

        if (searchInventory.get()) {
            int invSlot = findInRange(player, pickItem, 9, 35);
            if (invSlot != -1) {
                moveToHotbar(client, player, invSlot, currentHotbar);
            }
        }
    }

    private int findInRange(ClientPlayerEntity player, Item targetItem, int from, int to) {
        int bestSlot  = -1;
        int bestCount = -1;
        for (int i = from; i <= to; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isOf(targetItem)) continue;
            if (preferFullStack.get()) {
                if (stack.getCount() > bestCount) {
                    bestCount = stack.getCount();
                    bestSlot  = i;
                }
            } else {
                return i;
            }
        }
        return bestSlot;
    }

    private void switchHotbarTo(ClientPlayerEntity player, int hotbarIndex) {
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex));
        ((PlayerInventoryAccessor) player.getInventory()).setSelectedSlot(hotbarIndex);
    }

    private void moveToHotbar(MinecraftClient client, ClientPlayerEntity player,
                              int inventorySlot, int hotbarIndex) {
        int syncId     = player.playerScreenHandler.syncId;
        int hotbarSlot = 36 + hotbarIndex;
        ItemStack hotbarStack = player.getInventory().getStack(hotbarIndex);
        client.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(syncId, hotbarSlot,    0, SlotActionType.PICKUP, player);
        if (!hotbarStack.isEmpty()) {
            client.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.PICKUP, player);
        }
    }
}
