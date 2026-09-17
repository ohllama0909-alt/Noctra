package de.snenjih.noctra.mixin;

import de.snenjih.noctra.modules.impl.mention_highlight.MentionHighlightModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyArg(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        index = 0,
        require = 0
    )
    private Text onAddMessage(Text message) {
        MentionHighlightModule mod = MentionHighlightModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) return message;
        return mod.highlight(message);
    }
}
