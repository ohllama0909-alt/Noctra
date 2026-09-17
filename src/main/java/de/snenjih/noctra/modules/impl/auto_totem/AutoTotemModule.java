package de.snenjih.noctra.modules.impl.auto_totem;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AutoTotemModule extends BaseModule {

    private final ModuleSetting<Boolean> preferHotbar;
    private final ModuleSetting<Boolean> showWarning;

    private int lastWarnedTick = 0;

    public AutoTotemModule() {
        super(
            "auto_totem",
            "Auto Totem",
            "Keeps a Totem of Undying in your offhand automatically.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/auto_totem")
        );
        preferHotbar = addSetting(new BooleanSetting("prefer_hotbar", "Prefer Hotbar", false));
        showWarning  = addSetting(new BooleanSetting("show_warning",  "Show Warning",  true));
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (client.currentScreen != null) return;
        if (player.isCreative()) return;
        if (player.isSpectator()) return;

        ItemStack offhand = player.getOffHandStack();
        if (isTotem(offhand)) return;

        if (!offhand.isEmpty()) return;

        int totemSlot = findTotem(player, preferHotbar.get());
        if (totemSlot == -1) {
            if (showWarning.get()) {
                int currentTick = client.inGameHud.getTicks();
                if (currentTick - lastWarnedTick >= 40) {
                    lastWarnedTick = currentTick;
                    player.sendMessage(Text.translatable("noctra.auto_totem.no_totem"), true);
                }
            }
            return;
        }

        int syncId = player.playerScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, totemSlot, 40, SlotActionType.SWAP, player);
    }

    private int findTotem(ClientPlayerEntity player, boolean hotbarFirst) {
        if (hotbarFirst) {
            for (int i = 0; i <= 8; i++) {
                if (isTotem(player.getInventory().getStack(i))) return 36 + i;
            }
            for (int i = 9; i <= 35; i++) {
                if (isTotem(player.getInventory().getStack(i))) return i;
            }
        } else {
            for (int i = 9; i <= 35; i++) {
                if (isTotem(player.getInventory().getStack(i))) return i;
            }
            for (int i = 0; i <= 8; i++) {
                if (isTotem(player.getInventory().getStack(i))) return 36 + i;
            }
        }
        return -1;
    }

    private static boolean isTotem(ItemStack stack) {
        return stack.isOf(Items.TOTEM_OF_UNDYING);
    }
}
