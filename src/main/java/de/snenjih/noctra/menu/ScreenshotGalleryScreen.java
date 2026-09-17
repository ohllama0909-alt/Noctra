package de.snenjih.noctra.menu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenshotGalleryScreen extends Screen {

    private enum Mode { GRID, PREVIEW }

    private static final int COLUMNS      = 4;
    private static final int GAP          = 8;
    private static final int PADDING      = 16;
    private static final int HEADER_H     = 30;
    private static final int LABEL_H      = 13;
    private static final int SCROLL_SPEED = 24;

    private final Screen parent;

    private List<ScreenshotEntry> entries = new ArrayList<>();

    // Thumb textures: key = entry index, value = pre-scaled texture (exactly thumbW×thumbH)
    private final Map<Integer, Identifier> thumbTextures = new HashMap<>();

    private Mode mode = Mode.GRID;

    // Grid layout — recalculated in init()
    private int thumbW, thumbH, rowH;
    // Used to detect window resize and invalidate thumbs
    private int prevThumbW = -1;

    // Scroll position (vertical pixels)
    private float scrollOffset = 0f;
    private boolean isDragging = false;

    // Preview state
    private int        previewIndex = 0;
    private Identifier previewTexId = null;
    private int        previewTexW  = 1;
    private int        previewTexH  = 1;

    public ScreenshotGalleryScreen(Screen parent) {
        super(Text.translatable("noctra.screenshots.title"));
        this.parent = parent;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        entries = loadEntries();
        computeLayout();

        if (thumbW != prevThumbW) {
            // Window resized or first open — thumbnails at wrong size
            destroyAllThumbs();
            prevThumbW = thumbW;
        }

        if (mode == Mode.PREVIEW) {
            // Reload preview scaled to new screen dimensions
            loadPreview(previewIndex);
        }

        clampScroll();
    }

    @Override
    public void close() {
        if (mode == Mode.PREVIEW) {
            // ESC in preview → back to grid
            mode = Mode.GRID;
            destroyPreview();
            return;
        }
        destroyAllTextures();
        assert client != null;
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void computeLayout() {
        int usable = width - 2 * PADDING - (COLUMNS - 1) * GAP;
        thumbW = Math.max(1, usable / COLUMNS);
        thumbH = thumbW * 9 / 16;
        rowH   = thumbH + GAP + LABEL_H;
    }

    private void clampScroll() {
        int rows    = entries.isEmpty() ? 0 : (int) Math.ceil((float) entries.size() / COLUMNS);
        float maxS  = Math.max(0f, rows * rowH - (height - HEADER_H));
        scrollOffset = MathHelper.clamp(scrollOffset, 0f, maxS);
    }

    // -------------------------------------------------------------------------
    // Screenshot directory
    // -------------------------------------------------------------------------

    private List<ScreenshotEntry> loadEntries() {
        List<ScreenshotEntry> list = new ArrayList<>();
        Path dir = FabricLoader.getInstance().getGameDir().resolve("screenshots");
        if (!Files.isDirectory(dir)) return list;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir,
                p -> p.getFileName().toString().toLowerCase().endsWith(".png"))) {
            for (Path p : ds) {
                try {
                    long mod = Files.getLastModifiedTime(p).toMillis();
                    list.add(new ScreenshotEntry(p, p.getFileName().toString(), mod));
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
        list.sort(Comparator.comparingLong(ScreenshotEntry::lastModified).reversed());
        return list;
    }

    // -------------------------------------------------------------------------
    // Texture management
    // -------------------------------------------------------------------------

    /** Lazily loads and returns the thumbnail for entry[index], or null on error. */
    private Identifier getOrLoadThumb(int index) {
        if (thumbTextures.containsKey(index)) return thumbTextures.get(index);

        ScreenshotEntry entry = entries.get(index);
        Identifier id = Identifier.of("noctra", "screenshot_thumb_" + index);
        try (InputStream in = Files.newInputStream(entry.path())) {
            NativeImage full  = NativeImage.read(in);
            NativeImage thumb = createThumbImage(full);
            full.close();
            MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(() -> "noctra:thumb_" + index, thumb));
            thumbTextures.put(index, id);
        } catch (IOException e) {
            return null;
        }
        return id;
    }

    /**
     * Scales src to fit thumbW×thumbH (nearest-neighbour, letterboxed with black fill).
     * Returns a new NativeImage of exactly thumbW×thumbH; source is not closed.
     */
    private NativeImage createThumbImage(NativeImage src) {
        int sw = src.getWidth(), sh = src.getHeight();
        float aspect = (float) sw / sh;
        int dw, dh;
        if (aspect >= (float) thumbW / thumbH) {
            dw = thumbW;
            dh = Math.max(1, (int) (thumbW / aspect));
        } else {
            dh = thumbH;
            dw = Math.max(1, (int) (thumbH * aspect));
        }
        int ox = (thumbW - dw) / 2;
        int oy = (thumbH - dh) / 2;

        NativeImage dst = new NativeImage(NativeImage.Format.RGBA, thumbW, thumbH, false);
        for (int y = 0; y < thumbH; y++)
            for (int x = 0; x < thumbW; x++)
                dst.setColorArgb(x, y, 0xFF000000);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx = Math.min(x * sw / dw, sw - 1);
                int sy = Math.min(y * sh / dh, sh - 1);
                dst.setColorArgb(ox + x, oy + y, src.getColorArgb(sx, sy));
            }
        }
        return dst;
    }

    private void loadPreview(int index) {
        destroyPreview();

        ScreenshotEntry entry = entries.get(index);
        if (!Files.exists(entry.path())) {
            mode = Mode.GRID;
            return;
        }

        int margin = 30;
        int maxW = Math.max(1, width  - margin * 2);
        int maxH = Math.max(1, height - margin * 2);

        Identifier id = Identifier.of("noctra", "screenshot_preview");
        try (InputStream in = Files.newInputStream(entry.path())) {
            NativeImage full    = NativeImage.read(in);
            NativeImage preview = createScaledImage(full, maxW, maxH);
            full.close();
            previewTexW = preview.getWidth();
            previewTexH = preview.getHeight();
            MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(() -> "noctra:preview", preview));
            previewTexId = id;
        } catch (IOException e) {
            mode = Mode.GRID;
        }
    }

    /**
     * Scales src to fit within maxW×maxH, preserving aspect ratio.
     * Returns a new NativeImage; source is not closed.
     */
    private NativeImage createScaledImage(NativeImage src, int maxW, int maxH) {
        int sw = src.getWidth(), sh = src.getHeight();
        float scale = Math.min(1.0f, Math.min((float) maxW / sw, (float) maxH / sh));
        int dw = Math.max(1, (int) (sw * scale));
        int dh = Math.max(1, (int) (sh * scale));

        NativeImage dst = new NativeImage(NativeImage.Format.RGBA, dw, dh, false);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx = Math.min(x * sw / dw, sw - 1);
                int sy = Math.min(y * sh / dh, sh - 1);
                dst.setColorArgb(x, y, src.getColorArgb(sx, sy));
            }
        }
        return dst;
    }

    private void destroyPreview() {
        if (previewTexId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(previewTexId);
            previewTexId = null;
        }
    }

    private void destroyAllThumbs() {
        var tm = MinecraftClient.getInstance().getTextureManager();
        thumbTextures.values().forEach(tm::destroyTexture);
        thumbTextures.clear();
    }

    private void destroyAllTextures() {
        destroyAllThumbs();
        destroyPreview();
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xFF0A0A0A);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.translatable("noctra.screenshots.title"),
            width / 2, 10, 0xFFFFFFFF);

        if (mode == Mode.GRID) renderGrid(ctx, mouseX, mouseY);
        else                   renderPreview(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderGrid(DrawContext ctx, int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.screenshots.empty"),
                width / 2, height / 2, 0xFF888888);
            return;
        }

        int startY = HEADER_H - (int) scrollOffset;

        for (int i = 0; i < entries.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x   = PADDING + col * (thumbW + GAP);
            int y   = startY + row * rowH;

            if (y + thumbH < 0 || y > height) continue;

            boolean hovered = mouseX >= x && mouseX < x + thumbW
                           && mouseY >= y && mouseY < y + thumbH;

            ctx.fill(x, y, x + thumbW, y + thumbH, hovered ? 0xFF2A2A4E : 0xFF16213E);

            Identifier texId = getOrLoadThumb(i);
            if (texId != null) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texId,
                    x, y, 0f, 0f, thumbW, thumbH, thumbW, thumbH);
            }

            ctx.drawStrokedRectangle(x, y, thumbW, thumbH,
                hovered ? 0xFF6666AA : 0xFF333366);

            // Filename below thumbnail
            ctx.drawTextWithShadow(textRenderer,
                truncate(entries.get(i).filename(), thumbW - 4),
                x + 2, y + thumbH + 2, 0xFF888888);
        }
    }

    private void renderPreview(DrawContext ctx) {
        if (previewTexId == null) return;

        int imgX = (width  - previewTexW) / 2;
        int imgY = (height - previewTexH) / 2;

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, previewTexId,
            imgX, imgY, 0f, 0f, previewTexW, previewTexH, previewTexW, previewTexH);

        // Filename overlay (top-left of image)
        String name = entries.get(previewIndex).filename();
        int labelW = textRenderer.getWidth(name) + 8;
        ctx.fill(imgX, imgY, imgX + labelW, imgY + 14, 0xAA000000);
        ctx.drawTextWithShadow(textRenderer, name, imgX + 4, imgY + 3, 0xFFFFFFFF);

        // Counter (bottom-centre)
        ctx.drawCenteredTextWithShadow(textRenderer,
            (previewIndex + 1) + " / " + entries.size(),
            width / 2, height - 18, 0xFFAAAAAA);

        // Navigation arrows
        if (previewIndex > 0)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("◀"),
                imgX - 14, height / 2, 0xFFCCCCCC);
        if (previewIndex < entries.size() - 1)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("▶"),
                imgX + previewTexW + 14, height / 2, 0xFFCCCCCC);

        // Close hint (top-right)
        ctx.drawTextWithShadow(textRenderer,
            Text.translatable("noctra.screenshots.close"),
            width - 18, 10, 0xFFCCCCCC);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (click.button() != 0 || releaseOnly) return super.mouseClicked(click, releaseOnly);

        if (mode == Mode.GRID) {
            isDragging = true;
            int hit = hitTestGrid(click.x(), click.y());
            if (hit >= 0) {
                previewIndex = hit;
                loadPreview(hit);
                mode = Mode.PREVIEW;
                isDragging = false;
                return true;
            }
        } else {
            // Close button (top-right corner)
            if (click.x() >= width - 28 && click.y() <= 22) {
                mode = Mode.GRID;
                destroyPreview();
                return true;
            }
            // Left arrow
            if (previewIndex > 0 && click.x() < (width - previewTexW) / 2 - 4
                    && click.y() >= height / 2 - 10 && click.y() <= height / 2 + 10) {
                previewIndex--;
                loadPreview(previewIndex);
                return true;
            }
            // Right arrow
            if (previewIndex < entries.size() - 1
                    && click.x() > (width + previewTexW) / 2 + 4
                    && click.y() >= height / 2 - 10 && click.y() <= height / 2 + 10) {
                previewIndex++;
                loadPreview(previewIndex);
                return true;
            }
        }
        return super.mouseClicked(click, releaseOnly);
    }

    @Override
    public boolean mouseReleased(Click click) {
        isDragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && isDragging && mode == Mode.GRID) {
            scrollOffset -= (float) deltaY;
            clampScroll();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    // Try the 3-argument signature; if 1.21.11 uses 4 args this method simply won't be called
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mode == Mode.GRID) {
            scrollOffset -= (float) amount * SCROLL_SPEED;
            clampScroll();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int hitTestGrid(double mx, double my) {
        int startY = HEADER_H - (int) scrollOffset;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x   = PADDING + col * (thumbW + GAP);
            int y   = startY + row * rowH;
            if (mx >= x && mx < x + thumbW && my >= y && my < y + thumbH) return i;
        }
        return -1;
    }

    private String truncate(String text, int maxPx) {
        if (textRenderer.getWidth(text) <= maxPx) return text;
        while (!text.isEmpty() && textRenderer.getWidth(text + "…") > maxPx)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }
}
