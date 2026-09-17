package de.snenjih.noctra.modules.impl.stack_refill;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StackRefillModule extends BaseModule {

    private final ModuleSetting<Boolean> refillBlocks;
    private final ModuleSetting<Boolean> refillTools;
    private final ModuleSetting<Boolean> showNotification;

    private Item lastItem = Items.AIR;
    private int lastCount = 0;

    public StackRefillModule() {
        super(
            "stack_refill",
            "Stack Refill",
            "Automatically refills your held stack from inventory when it runs out.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/stack_refill")
        );
        refillBlocks     = addSetting(new BooleanSetting("refill_blocks",      "Refill Blocks",      true));
        refillTools      = addSetting(new BooleanSetting("refill_tools",       "Refill Tools",       false));
        showNotification = addSetting(new BooleanSetting("show_notification",  "Show Notification",  false));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (player.isCreative()) return;
        if (player.isSpectator()) return;

        ItemStack held = player.getMainHandStack();

        if (client.currentScreen == null && lastItem != Items.AIR && held.isEmpty()) {
            refill(client, player, lastItem);
        }

        lastItem  = held.isEmpty() ? Items.AIR : held.getItem();
        lastCount = held.getCount();
    }

    private void refill(MinecraftClient client, ClientPlayerEntity player, Item targetItem) {
        ItemStack dummy = new ItemStack(targetItem);
        if (dummy.isDamageable() && !refillTools.get()) return;
        if (targetItem instanceof BlockItem && !refillBlocks.get()) return;

        int sourceSlot = findInMainInventory(player, targetItem);
        if (sourceSlot == -1) return;

        int hotbarSlot = 36 + player.getInventory().getSelectedSlot();
        int syncId = player.playerScreenHandler.syncId;

        client.interactionManager.clickSlot(syncId, sourceSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(syncId, hotbarSlot, 0, SlotActionType.PICKUP, player);

        if (showNotification.get()) {
            player.sendMessage(
                Text.translatable("noctra.stack_refill.refilled",
                    Text.translatable(targetItem.getTranslationKey())),
                true
            );
        }
    }

    private int findInMainInventory(ClientPlayerEntity player, Item targetItem) {
        int bestSlot  = -1;
        int bestCount = 0;
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem) && stack.getCount() > bestCount) {
                bestSlot  = i;
                bestCount = stack.getCount();
            }
        }
        return bestSlot;
    }

    @Override
    public void onDisable() {
        lastItem  = Items.AIR;
        lastCount = 0;
    }
}
