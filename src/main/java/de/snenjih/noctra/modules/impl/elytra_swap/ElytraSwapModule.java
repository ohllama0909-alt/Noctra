package de.snenjih.noctra.modules.impl.elytra_swap;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.KeybindSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class ElytraSwapModule extends BaseModule {

    private static final int CHEST_SLOT = 6;

    private final ModuleSetting<Integer> keybindSetting;
    private boolean prevKeyHeld = false;

    public ElytraSwapModule() {
        super(
            "elytra_swap",
            "Elytra Swap",
            "Right-click elytra/chestplate to swap.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/elytra_swap")
        );
        keybindSetting = addSetting(new KeybindSetting("keybind", "Swap Keybind", -1));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        int keyCode = keybindSetting.get();
        if (keyCode == -1 || client.currentScreen != null || client.player == null) {
            prevKeyHeld = false;
            return;
        }
        boolean held = InputUtil.isKeyPressed(client.getWindow(), keyCode);
        if (held && !prevKeyHeld) {
            performKeySwap(client, client.player);
        }
        prevKeyHeld = held;
    }

    @Override
    public ActionResult onInteractItem(ClientPlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return ActionResult.PASS;

        ItemStack handStack = player.getMainHandStack();
        if (!isChestEquippable(handStack)) return ActionResult.PASS;

        if (player.isCreative()) {
            player.sendMessage(Text.literal("Elytra Swap: Not supported in Creative mode."), true);
            return ActionResult.FAIL;
        }

        if (player.isGliding()) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.flying"), true);
            return ActionResult.FAIL;
        }

        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);

        if (!chestStack.isEmpty() && hasBindingCurse(chestStack)) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.binding"), true);
            return ActionResult.FAIL;
        }

        if (handStack.isDamageable() && handStack.getDamage() >= handStack.getMaxDamage()) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.broken"), true);
            return ActionResult.FAIL;
        }

        performSwap(mc, player, chestStack.isEmpty());
        return ActionResult.SUCCESS;
    }

    private void performKeySwap(MinecraftClient mc, ClientPlayerEntity player) {
        if (player.isCreative()) {
            player.sendMessage(Text.literal("Elytra Swap: Not supported in Creative mode."), true);
            return;
        }
        if (player.isGliding()) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.flying"), true);
            return;
        }

        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chestStack.isEmpty() && hasBindingCurse(chestStack)) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.binding"), true);
            return;
        }

        int targetSlot = findSwapTarget(player);
        if (targetSlot == -1) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.no_swap_target"), true);
            return;
        }

        ItemStack targetStack = getInventoryStack(player, targetSlot);
        if (targetStack.isDamageable() && targetStack.getDamage() >= targetStack.getMaxDamage()) {
            player.sendMessage(Text.translatable("noctra.elytra_swap.blocked.broken"), true);
            return;
        }

        performSwapFromSlot(mc, player, targetSlot, chestStack.isEmpty());
    }

    private int findSwapTarget(ClientPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        boolean chestHasElytra = !chest.isEmpty() && chest.isOf(Items.ELYTRA);
        // Prefer the "other" type: if chest has elytra, look for chestplate; otherwise look for elytra
        boolean preferElytra = !chestHasElytra;

        int fallback = -1;
        // Hotbar: PlayerInventory indices 0-8 → ScreenHandler slots 36-44
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!isChestEquippable(s)) continue;
            boolean isElytra = s.isOf(Items.ELYTRA);
            if (isElytra == preferElytra) return 36 + i;
            if (fallback == -1) fallback = 36 + i;
        }
        // Main inventory: PlayerInventory indices 9-35 → ScreenHandler slots 9-35
        for (int i = 9; i < 36; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!isChestEquippable(s)) continue;
            boolean isElytra = s.isOf(Items.ELYTRA);
            if (isElytra == preferElytra) return i;
            if (fallback == -1) fallback = i;
        }
        return fallback;
    }

    private ItemStack getInventoryStack(ClientPlayerEntity player, int screenHandlerSlot) {
        if (screenHandlerSlot >= 36 && screenHandlerSlot <= 44) {
            return player.getInventory().getStack(screenHandlerSlot - 36);
        }
        return player.getInventory().getStack(screenHandlerSlot);
    }

    private void performSwap(MinecraftClient mc, ClientPlayerEntity player, boolean chestWasEmpty) {
        int syncId    = player.playerScreenHandler.syncId;
        int hotbarIdx = 36 + player.getInventory().getSelectedSlot();

        mc.interactionManager.clickSlot(syncId, hotbarIdx,  0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player);
        if (!chestWasEmpty) {
            mc.interactionManager.clickSlot(syncId, hotbarIdx, 0, SlotActionType.PICKUP, player);
        }
    }

    private void performSwapFromSlot(MinecraftClient mc, ClientPlayerEntity player,
                                      int invSlot, boolean chestWasEmpty) {
        int syncId = player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, invSlot,    0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player);
        if (!chestWasEmpty) {
            mc.interactionManager.clickSlot(syncId, invSlot, 0, SlotActionType.PICKUP, player);
        }
    }

    private static boolean isChestEquippable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static boolean hasBindingCurse(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.getEnchantments();
        return enchantments.getEnchantments().stream()
            .anyMatch(entry -> entry.matchesKey(Enchantments.BINDING_CURSE));
    }
}
