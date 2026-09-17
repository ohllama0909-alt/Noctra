package de.snenjih.noctra.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class NotificationManager {

    public enum Type { INFO, SUCCESS, ERROR }

    private static final int MAX_VISIBLE  = 4;
    private static final int DISPLAY_TICKS = 60;  // 3 s at 20 tps
    private static final int FADE_TICKS   = 15;
    private static final int NOTIF_W      = 160;
    private static final int NOTIF_H      = 18;
    private static final int GAP          = 3;
    private static final int MARGIN       = 6;

    private static final Deque<Entry> QUEUE = new ArrayDeque<>();

    private static final class Entry {
        final String text;
        final Type   type;
        int remaining = DISPLAY_TICKS;

        Entry(String text, Type type) {
            this.text = text;
            this.type = type;
        }
    }

    private NotificationManager() {}

    public static void push(String text, Type type) {
        if (QUEUE.size() >= MAX_VISIBLE) QUEUE.pollFirst(); // drop oldest when full
        QUEUE.addLast(new Entry(text, type));
    }

    /** Call once per client tick. */
    public static void tick() {
        QUEUE.removeIf(e -> --e.remaining <= 0);
    }

    /** Call from HUD render callback. */
    public static void render(DrawContext ctx, int screenW, int screenH) {
        if (QUEUE.isEmpty()) return;
        int y = screenH - MARGIN;
        for (Iterator<Entry> it = QUEUE.descendingIterator(); it.hasNext(); ) {
            Entry e = it.next();
            y -= NOTIF_H;
            float fade  = e.remaining < FADE_TICKS ? e.remaining / (float) FADE_TICKS : 1f;
            int   alpha = (int) (fade * 220);
            renderEntry(ctx, e, screenW - MARGIN - NOTIF_W, y, alpha);
            y -= GAP;
        }
    }

    private static void renderEntry(DrawContext ctx, Entry e, int x, int y, int alpha) {
        int bgRgb = switch (e.type) {
            case SUCCESS -> 0x1A3D1A;
            case ERROR   -> 0x3D1A1A;
            default      -> 0x1A1A1A;
        };
        int textRgb = switch (e.type) {
            case SUCCESS -> 0x44FF88;
            case ERROR   -> 0xFF5555;
            default      -> 0xEEEEEE;
        };

        ctx.fill(x, y, x + NOTIF_W, y + NOTIF_H, (alpha << 24) | bgRgb);
        ctx.drawStrokedRectangle(x, y, NOTIF_W, NOTIF_H, (alpha << 24) | 0x444444);

        String display = e.text.length() > 26 ? e.text.substring(0, 24) + "…" : e.text;
        ctx.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                display,
                x + 5, y + 5,
                (alpha << 24) | textRgb
        );
    }
}
