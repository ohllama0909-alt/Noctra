package de.snenjih.noctra.modules.impl.auto_eat;

import de.snenjih.noctra.mixin.accessor.PlayerInventoryAccessor;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class AutoEatModule extends BaseModule {

    private final ModuleSetting<Integer> hungerThreshold;
    private final ModuleSetting<Boolean> preferBest;
    private final ModuleSetting<Boolean> searchInventory;
    private final ModuleSetting<Boolean> eatGoldenApple;

    private boolean eating = false;

    public AutoEatModule() {
        super(
            "auto_eat",
            "Auto Eat",
            "Automatically eats food when your hunger drops below the threshold.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/auto_eat")
        );
        hungerThreshold = addSetting(new IntSetting("hunger_threshold",  "Eat Below",        16, 1, 20));
        preferBest      = addSetting(new BooleanSetting("prefer_best",     "Prefer Best Food",  true));
        searchInventory = addSetting(new BooleanSetting("search_inventory", "Search Inventory", false));
        eatGoldenApple  = addSetting(new BooleanSetting("eat_golden_apple", "Eat Golden Apples", true));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            eating = false;
            return;
        }
        if (player.isCreative() || player.isSpectator()) return;

        if (eating) {
            if (player.isUsingItem()) {
                FoodComponent food = player.getActiveItem().get(DataComponentTypes.FOOD);
                if (food == null) eating = false; // interrupted by bow/shield, not food
            } else {
                eating = false; // done or interrupted
            }
            return;
        }

        int foodLevel = player.getHungerManager().getFoodLevel();
        if (foodLevel > hungerThreshold.get() || foodLevel >= 20) return;
        if (player.isGliding()) return;
        if (player.isUsingItem()) return; // already using bow or other item

        int foodSlot = findFood(player);
        if (foodSlot == -1) return;

        int currentHotbar = player.getInventory().getSelectedSlot();

        if (foodSlot <= 8) {
            if (foodSlot != currentHotbar) {
                switchHotbarTo(client, player, foodSlot);
            }
        } else {
            moveToHotbar(client, player, foodSlot, currentHotbar);
        }

        eating = true;
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
    }

    private int findFood(ClientPlayerEntity player) {
        int bestSlot  = -1;
        float bestScore = -1f;
        int limit = searchInventory.get() ? 35 : 8;
        for (int i = 0; i <= limit; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || !isValidFood(stack)) continue;
            float score = preferBest.get() ? getSaturation(stack) : 1f;
            if (score > bestScore) {
                bestScore = score;
                bestSlot  = i;
            }
        }
        return bestSlot;
    }

    private boolean isValidFood(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return false;
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return eatGoldenApple.get();
        }
        return true;
    }

    private float getSaturation(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null ? food.saturation() : 0f;
    }

    private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slotIndex) {
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex));
        ((PlayerInventoryAccessor) player.getInventory()).setSelectedSlot(slotIndex);
    }

    private void moveToHotbar(MinecraftClient client, ClientPlayerEntity player, int inventorySlot, int hotbarIndex) {
        int syncId     = player.playerScreenHandler.syncId;
        int hotbarSlot = 36 + hotbarIndex;
        client.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(syncId, hotbarSlot,    0, SlotActionType.PICKUP, player);
        if (!player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            client.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.PICKUP, player);
        }
    }

    @Override
    public void onDisable() {
        eating = false;
    }
}
