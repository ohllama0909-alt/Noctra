package de.snenjih.mandatory.mixin;

import de.snenjih.mandatory.modules.impl.shulker_tooltip.ShulkerTooltipData;
import de.snenjih.mandatory.modules.impl.shulker_tooltip.ShulkerTooltipModule;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ItemStack.class)
public class ShulkerTooltipItemMixin {

    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void injectShulkerPreview(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        ShulkerTooltipModule module = ShulkerTooltipModule.INSTANCE;
        if (module == null || !module.isEnabled()) return;
        if (!module.isPreviewActive()) return;

        ItemStack self = (ItemStack) (Object) this;
        List<ItemStack> items = module.getContainerItems(self);
        if (items == null) return;

        cir.setReturnValue(Optional.of(new ShulkerTooltipData(
            items,
            module.getTotalSlots(self),
            module.maxRowSize.get(),
            module.shortCounts.get(),
            module.shouldUseCompactMode(),
            module.getBackgroundColor(self)
        )));
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void injectKeyHint(Item.TooltipContext context, PlayerEntity player, TooltipType type,
                               CallbackInfoReturnable<List<Text>> cir) {
        ShulkerTooltipModule module = ShulkerTooltipModule.INSTANCE;
        if (module == null || !module.isEnabled() || !module.showKeyHint.get()) return;
        if (module.alwaysOn.get()) return;

        ItemStack self = (ItemStack) (Object) this;
        List<ItemStack> items = module.getContainerItems(self);
        if (items == null) return;

        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null) return;

        if (!module.isPreviewActive()) {
            int keyCode = module.previewKey.get();
            String keyName;
            try {
                keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
            } catch (Exception e) {
                keyName = "Shift";
            }
            tooltip.add(Text.literal("Hold " + keyName + " to preview").styled(s -> s.withColor(0xAAAAAA)));
        }
    }
}
