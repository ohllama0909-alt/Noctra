package de.snenjih.noctra.modules.impl.smart_replace;

import de.snenjih.noctra.mixin.accessor.PlayerInventoryAccessor;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SmartReplaceModule extends BaseModule {

    public enum MatchMode { SAME_ITEM, SAME_FAMILY }

    private final ModuleSetting<Boolean> searchInventory;
    private final ModuleSetting<MatchMode> matchMode;
    private final ModuleSetting<Boolean> preferBestDurability;
    private final ModuleSetting<Boolean> showNotification;

    private Item lastItem = Items.AIR;
    private int lastMaxDamage = 0;
    private Item pendingItem = null;

    public SmartReplaceModule() {
        super(
            "smart_replace",
            "Smart Replace",
            "Automatically replaces a broken tool with the next best from your inventory.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/smart_replace")
        );
        searchInventory      = addSetting(new BooleanSetting("search_inventory",       "Search Inventory",      true));
        matchMode            = addSetting(new EnumSetting<>("match_mode",              "Match Mode",            MatchMode.SAME_ITEM, MatchMode.class));
        preferBestDurability = addSetting(new BooleanSetting("prefer_best_durability", "Prefer Best Durability", true));
        showNotification     = addSetting(new BooleanSetting("show_notification",       "Show Notification",     true));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (pendingItem != null && client.currentScreen == null && player != null
                && !player.isCreative() && !player.isSpectator()) {
            handleBreak(client, player, pendingItem);
            pendingItem = null;
        }

        if (player == null || player.isCreative() || player.isSpectator()) {
            updateSnapshot(player);
            return;
        }

        ItemStack held = player.getMainHandStack();
        boolean toolBroke = lastItem != Items.AIR && lastMaxDamage > 0 && held.isEmpty();

        if (toolBroke) {
            if (client.currentScreen != null) {
                pendingItem = lastItem;
            } else {
                handleBreak(client, player, lastItem);
            }
        }

        updateSnapshot(player);
    }

    private void handleBreak(MinecraftClient client, ClientPlayerEntity player, Item brokenItem) {
        int replacement = findReplacement(player, brokenItem);
        int currentHotbar = player.getInventory().getSelectedSlot();

        if (replacement == -1) {
            if (showNotification.get()) {
                player.sendMessage(Text.translatable("noctra.smart_replace.no_replacement",
                    Text.translatable(brokenItem.getTranslationKey())), true);
            }
            return;
        }

        if (replacement <= 8) {
            switchHotbarTo(client, player, replacement);
        } else {
            int syncId = player.playerScreenHandler.syncId;
            int hotbarSlot = 36 + currentHotbar;
            client.interactionManager.clickSlot(syncId, replacement, 0, SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(syncId, hotbarSlot,  0, SlotActionType.PICKUP, player);
        }

        if (showNotification.get()) {
            player.sendMessage(Text.translatable("noctra.smart_replace.replaced",
                Text.translatable(brokenItem.getTranslationKey())), true);
        }
    }

    private int findReplacement(ClientPlayerEntity player, Item brokenItem) {
        int currentHotbar = player.getInventory().getSelectedSlot();
        int bestSlot = -1;
        int bestDurability = -1;
        int limit = searchInventory.get() ? 35 : 8;

        for (int i = 0; i <= limit; i++) {
            if (i == currentHotbar) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            if (!isMatch(stack, brokenItem)) continue;

            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (!preferBestDurability.get()) return i;
            if (remaining > bestDurability) {
                bestDurability = remaining;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean isMatch(ItemStack candidate, Item brokenItem) {
        if (matchMode.get() == MatchMode.SAME_ITEM) {
            return candidate.isOf(brokenItem);
        }
        return isSameFamily(candidate.getItem(), brokenItem);
    }

    private static final List<TagKey<Item>> TOOL_FAMILIES = List.of(
        ItemTags.PICKAXES,
        ItemTags.AXES,
        ItemTags.SWORDS,
        ItemTags.SHOVELS,
        ItemTags.HOES
    );

    private boolean isSameFamily(Item candidate, Item original) {
        ItemStack cs = candidate.getDefaultStack();
        ItemStack os = original.getDefaultStack();
        for (TagKey<Item> tag : TOOL_FAMILIES) {
            if (cs.isIn(tag) && os.isIn(tag)) return true;
        }
        // Fallback for non-standard tools (trident, mace, shears, etc.)
        return candidate.getClass() == original.getClass();
    }

    private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slotIndex) {
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex));
        ((PlayerInventoryAccessor) player.getInventory()).setSelectedSlot(slotIndex);
    }

    private void updateSnapshot(ClientPlayerEntity player) {
        if (player == null || player.getMainHandStack().isEmpty()) {
            lastItem = Items.AIR;
            lastMaxDamage = 0;
        } else {
            ItemStack stack = player.getMainHandStack();
            lastItem = stack.getItem();
            lastMaxDamage = stack.getMaxDamage();
        }
    }

    @Override
    public void onDisable() {
        lastItem = Items.AIR;
        lastMaxDamage = 0;
        pendingItem = null;
    }
}
