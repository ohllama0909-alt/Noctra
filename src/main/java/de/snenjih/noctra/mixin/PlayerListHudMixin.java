package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.nametag_badge.NametageModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Redirect(
        method = "getPlayerName",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/network/PlayerListEntry;getDisplayName()Lnet/minecraft/text/Text;")
    )
    private Text redirectTablistName(PlayerListEntry entry) {
        Text original = entry.getDisplayName();
        if (!NametageModule.isBadgeActive() || !NametageModule.isShowInTablist()) return original;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return original;
        if (!entry.getProfile().id().equals(mc.player.getUuid())) return original;
        Text base = original != null ? original : Text.literal(entry.getProfile().name());
        return Text.literal("✦ ").withColor(NametageModule.getBadgeColor()).append(base);
    }
}
