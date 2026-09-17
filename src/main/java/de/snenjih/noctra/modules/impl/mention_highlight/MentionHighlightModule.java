package de.snenjih.noctra.modules.impl.mention_highlight;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MentionHighlightModule extends BaseModule {

    /** Singleton for the Mixin to access. */
    public static MentionHighlightModule INSTANCE;

    private final ModuleSetting<Integer> color;
    private final ModuleSetting<Boolean> playSound;
    private final ModuleSetting<Boolean> partialMatch;

    public MentionHighlightModule() {
        super(
            "mention_highlight",
            "Mention Highlight",
            "Highlights your name in chat and plays a sound ping.",
            ModuleCategory.CHAT,
            Identifier.of("noctra", "modules/mention_highlight")
        );
        color        = addSetting(new ColorSetting("color",         "Highlight Colour",      0xFFFF55));
        playSound    = addSetting(new BooleanSetting("play_sound",  "Play Sound on Mention", true));
        partialMatch = addSetting(new BooleanSetting("partial_match", "Partial Match",       false));
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        INSTANCE = null;
    }

    /**
     * Called from ChatHudMixin to (potentially) highlight the text.
     * Returns the original text unchanged if no match; returns a rebuilt Text if matched.
     */
    public Text highlight(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return message;

        String ownName = mc.player.getGameProfile().name();
        if (ownName == null || ownName.isBlank()) return message;

        String plain = message.getString();
        if (plain.isEmpty() || plain.length() < ownName.length()) return message;

        int matchStart = -1;
        int matchEnd   = -1;

        if (partialMatch.get()) {
            // Split on non-word characters to get tokens
            int idx = 0;
            outer:
            for (String token : plain.split("[^a-zA-Z0-9_]")) {
                int tokenStart = plain.indexOf(token, idx);
                if (token.toLowerCase().contains(ownName.toLowerCase())) {
                    matchStart = tokenStart;
                    matchEnd   = tokenStart + token.length();
                    break;
                }
                idx = tokenStart + token.length();
            }
        } else {
            int idx = plain.toLowerCase().indexOf(ownName.toLowerCase());
            if (idx != -1) {
                matchStart = idx;
                matchEnd   = idx + ownName.length();
            }
        }

        if (matchStart == -1) return message;

        // Rebuild text with highlighted segment
        int highlightColor = color.get();
        Text prefix  = Text.literal(plain.substring(0, matchStart));
        Text match   = Text.literal(plain.substring(matchStart, matchEnd))
                           .setStyle(Style.EMPTY.withColor(highlightColor));
        Text suffix  = Text.literal(plain.substring(matchEnd));
        Text rebuilt = Text.empty().append(prefix).append(match).append(suffix);

        // Play sound
        if (playSound.get() && mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
        }

        return rebuilt;
    }
}
