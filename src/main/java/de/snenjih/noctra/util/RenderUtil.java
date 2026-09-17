package de.snenjih.noctra.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Rendering utilities for 1.21.11.
 * In this version WorldRenderer.drawBox() no longer exists as a static helper.
 * LINES vertex format: POSITION_COLOR_NORMAL_LINE_WIDTH — each line segment needs 2 vertices,
 * each with vertex + color + normal + lineWidth calls.
 */
public final class RenderUtil {

    private RenderUtil() {}

    /**
     * Returns the RenderLayer for lines (POSITION_COLOR_NORMAL_LINE_WIDTH).
     */
    public static RenderLayer linesLayer() {
        return RenderLayers.LINES;
    }

    /**
     * Draws a single line segment between two world-relative positions.
     * The normal points from start to end (used for line direction).
     */
    public static void drawLine(MatrixStack matrices, VertexConsumer buf,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a,
                                 float lineWidth) {
        var entry = matrices.peek();
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);
        int ai = (int) (a * 255);

        buf.vertex(entry, (float) x1, (float) y1, (float) z1)
           .color(ri, gi, bi, ai)
           .normal(entry, nx, ny, nz)
           .lineWidth(lineWidth);
        buf.vertex(entry, (float) x2, (float) y2, (float) z2)
           .color(ri, gi, bi, ai)
           .normal(entry, nx, ny, nz)
           .lineWidth(lineWidth);
    }

    /**
     * Draws a wireframe box using 12 edges.
     */
    public static void drawBox(MatrixStack matrices, VertexConsumer buf,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float r, float g, float b, float a) {
        drawBox(matrices, buf, x1, y1, z1, x2, y2, z2, r, g, b, a, 1.0f);
    }

    /**
     * Draws a wireframe box using 12 edges with configurable line width.
     */
    public static void drawBox(MatrixStack matrices, VertexConsumer buf,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float r, float g, float b, float a, float lineWidth) {
        // Bottom face
        drawLine(matrices, buf, x1, y1, z1, x2, y1, z1, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y1, z1, x2, y1, z2, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y1, z2, x1, y1, z2, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x1, y1, z2, x1, y1, z1, r, g, b, a, lineWidth);
        // Top face
        drawLine(matrices, buf, x1, y2, z1, x2, y2, z1, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y2, z1, x2, y2, z2, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y2, z2, x1, y2, z2, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x1, y2, z2, x1, y2, z1, r, g, b, a, lineWidth);
        // Vertical edges
        drawLine(matrices, buf, x1, y1, z1, x1, y2, z1, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y1, z1, x2, y2, z1, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x2, y1, z2, x2, y2, z2, r, g, b, a, lineWidth);
        drawLine(matrices, buf, x1, y1, z2, x1, y2, z2, r, g, b, a, lineWidth);
    }
}
