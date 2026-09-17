package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.food_tooltip.FoodTooltipModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class FoodTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void injectFoodTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type,
                                   CallbackInfoReturnable<List<Text>> cir) {
        FoodTooltipModule module = FoodTooltipModule.INSTANCE;
        if (module == null || !module.isEnabled()) return;

        ItemStack self = (ItemStack) (Object) this;
        FoodComponent food = self.get(DataComponentTypes.FOOD);
        if (food == null) return;

        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null) return;

        int nutrition       = food.nutrition();
        float saturationMod = food.saturation();

        tooltip.add(Text.literal("Hunger: +" + nutrition)
                .styled(s -> s.withColor(0xFFAA00)));

        if (module.showSaturation.get()) {
            tooltip.add(Text.literal("Saturation: " + String.format("%.1f", saturationMod))
                    .styled(s -> s.withColor(0xFF55FF)));
        }

        if (module.showEffectiveSaturation.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                float currentSat = mc.player.getHungerManager().getSaturationLevel();
                float effective  = Math.min(nutrition * saturationMod * 2f, 20f - currentSat);
                tooltip.add(Text.literal("Effective Saturation: +" + String.format("%.1f", effective))
                        .styled(s -> s.withColor(0x55FFFF)));
            }
        }

        if (module.showEffects.get()) {
            ConsumableComponent consumable = self.get(DataComponentTypes.CONSUMABLE);
            if (consumable != null) {
                for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
                    if (!(consumeEffect instanceof ApplyEffectsConsumeEffect applyEffect)) continue;
                    float probability = applyEffect.probability();
                    for (StatusEffectInstance effect : applyEffect.effects()) {
                        String effectName = effect.getEffectType().value().getName().getString();
                        int durationSec   = effect.getDuration() / 20;
                        String line = effectName + " " + toRoman(effect.getAmplifier() + 1)
                                + " (" + durationSec + "s)"
                                + (probability < 1.0f ? " " + (int)(probability * 100) + "%" : "");
                        int color = effect.getEffectType().value().isBeneficial() ? 0x55FF55 : 0xFF5555;
                        tooltip.add(Text.literal(line).styled(s -> s.withColor(color)));
                    }
                }
            }
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
