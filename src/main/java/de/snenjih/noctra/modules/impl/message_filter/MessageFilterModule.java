package de.snenjih.noctra.modules.impl.message_filter;

import de.snenjih.noctra.config.FilterConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.regex.PatternSyntaxException;

public class MessageFilterModule extends BaseModule {

    public static MessageFilterModule INSTANCE;

    private final ModuleSetting<Boolean> useRegex;
    private final ModuleSetting<Boolean> showCounter;

    private FilterConfig filterConfig;
    private int suppressedCount = 0;

    public MessageFilterModule() {
        super(
            "message_filter",
            "Message Filter",
            "Hides chat messages matching your filter list.",
            ModuleCategory.CHAT,
            Identifier.of("noctra", "modules/message_filter")
        );
        useRegex     = addSetting(new BooleanSetting("use_regex",     "Use Regex",          false));
        showCounter  = addSetting(new BooleanSetting("show_counter",  "Show Filter Count",  true));
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        filterConfig = new FilterConfig();
        filterConfig.load();
        suppressedCount = 0;
    }

    @Override
    public void onDisable() {
        INSTANCE = null;
        suppressedCount = 0;
    }

    public FilterConfig getFilterConfig() { return filterConfig; }

    @Override
    public ActionResult onReceiveChat(Text message) {
        if (filterConfig == null) return ActionResult.PASS;
        List<String> pats = filterConfig.getPatterns();
        if (pats.isEmpty()) return ActionResult.PASS;

        String plain = message.getString();
        for (String pattern : pats) {
            boolean matched;
            if (useRegex.get()) {
                try {
                    matched = plain.matches("(?i).*" + pattern + ".*");
                } catch (PatternSyntaxException e) {
                    matched = plain.toLowerCase().contains(pattern.toLowerCase());
                }
            } else {
                matched = plain.toLowerCase().contains(pattern.toLowerCase());
            }
            if (matched) {
                suppressedCount++;
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onRenderHud(DrawContext ctx, float tickDelta) {
        if (!showCounter.get() || suppressedCount == 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String text = "§7[Filter] §f" + suppressedCount + " blocked";
        int x = mc.getWindow().getScaledWidth() / 2
                - mc.textRenderer.getWidth(text) / 2;
        int y = mc.getWindow().getScaledHeight() - 48;
        ctx.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
