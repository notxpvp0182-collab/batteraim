package com.betteraim.gui;

import com.betteraim.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Full-screen colour picker using Minecraft's native button / text-field widgets.
 *
 * Layout (all positions are computed responsively):
 *   ┌─────────────────────────┐
 *   │   SV square             │  big square: saturation (X) × value (Y)
 *   ├─────────────────────────┤
 *   │   Hue bar               │  rainbow gradient strip
 *   │   Alpha bar             │  checkerboard + colour strip
 *   ├─────────────────────────┤
 *   │  Old ██  New ██  #RRGGBB│
 *   │  [Cancel]      [Done]   │
 *   └─────────────────────────┘
 */
@Environment(EnvType.CLIENT)
public class ColorPickerScreen extends Screen {

    private static final int PAD = 10;

    private final Screen    parent;
    private final int       initialRgb;   // 24-bit
    private final float     initialAlpha; // 0-1
    private final Consumer<Integer> onDone; // receives packed ARGB

    // Current colour state
    private float hue = 0f, sat = 1f, val = 1f, alpha = 1f;

    // Widget areas (pixel coords in GUI-scaled space)
    private int svX, svY, svSize;          // saturation-value square
    private int hueX, hueY, hueW, hueH;   // hue bar
    private int alphaX, alphaY, alphaW;    // alpha bar
    private int previewOldX, previewNewX, previewY, previewSz;

    // Interaction state
    private boolean draggingSV    = false;
    private boolean draggingHue   = false;
    private boolean draggingAlpha = false;

    // Hex input widget
    private TextFieldWidget hexField;

    public ColorPickerScreen(Screen parent, int currentArgb, Consumer<Integer> onDone) {
        super(Text.translatable("screen.better-aim.color_picker.title"));
        this.parent      = parent;
        this.onDone      = onDone;
        // Decompose the incoming ARGB
        this.initialRgb   = currentArgb & 0xFFFFFF;
        this.initialAlpha = ColorUtil.af(currentArgb);

        float[] hsv = ColorUtil.rgbToHsv(
                ColorUtil.red(currentArgb),
                ColorUtil.green(currentArgb),
                ColorUtil.blue(currentArgb));
        this.hue   = hsv[0];
        this.sat   = hsv[1];
        this.val   = hsv[2];
        this.alpha = this.initialAlpha;
    }

    @Override
    protected void init() {
        computeLayout();

        // Hex input
        hexField = new TextFieldWidget(textRenderer,
                svX, hueY + hueH * 2 + PAD,
                100, 20,
                Text.translatable("screen.better-aim.color_picker.hex"));
        hexField.setMaxLength(6);
        hexField.setText(ColorUtil.toHex(currentRgb()));
        hexField.setChangedListener(this::onHexChanged);
        addDrawableChild(hexField);

        // Done button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.better-aim.color_picker.done"),
                btn -> finish())
                .dimensions(width / 2 + PAD, height - 30, 90, 20)
                .build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.better-aim.color_picker.cancel"),
                btn -> close())
                .dimensions(width / 2 - 90 - PAD, height - 30, 90, 20)
                .build());

        // Reset button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.better-aim.color_picker.reset"),
                btn -> resetToInitial())
                .dimensions(width / 2 - 45, height - 30, 90, 20)
                .build());
    }

    private void computeLayout() {
        int usable = Math.min(width - PAD * 4, 280);
        svSize = Math.min(usable, height - 180);
        svX = (width - usable) / 2;
        svY = 30;

        hueX = svX;      hueY = svY + svSize + PAD;
        hueW = usable;   hueH = 14;

        alphaX = svX;    alphaY = hueY + hueH + 6;
        alphaW = usable;

        previewSz = 20;
        previewY  = alphaY + hueH + 10;
        previewOldX = svX;
        previewNewX = svX + previewSz + 6;
    }

    // ── Rendering ──────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawSVSquare(context);
        drawHueBar(context);
        drawAlphaBar(context);
        drawPreviews(context);
        drawSVHandle(context);
        drawHueHandle(context);
        drawAlphaHandle(context);
        super.render(context, mouseX, mouseY, delta);
        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.better-aim.color_picker.title"),
                width / 2, 8, 0xFFFFFF);
    }

    private void drawSVSquare(DrawContext context) {
        // Draw saturation-value square as a grid of coloured fills.
        // Step by 4 px for acceptable performance.
        int step = Math.max(1, svSize / 64);
        for (int px = 0; px < svSize; px += step) {
            float s = (float) px / svSize;
            for (int py = 0; py < svSize; py += step) {
                float v = 1f - (float) py / svSize;
                int rgb  = ColorUtil.hsvToRgb(hue, s, v);
                int argb = 0xFF000000 | rgb;
                context.fill(svX + px, svY + py, svX + px + step, svY + py + step, argb);
            }
        }
        // Border
        context.fill(svX - 1, svY - 1, svX + svSize + 1, svY, 0xFF888888);
        context.fill(svX - 1, svY + svSize, svX + svSize + 1, svY + svSize + 1, 0xFF888888);
        context.fill(svX - 1, svY, svX, svY + svSize, 0xFF888888);
        context.fill(svX + svSize, svY, svX + svSize + 1, svY + svSize, 0xFF888888);
    }

    private void drawHueBar(DrawContext context) {
        int step = Math.max(1, hueW / 72);
        for (int px = 0; px < hueW; px += step) {
            float h = (float) px / hueW * 360f;
            int rgb  = ColorUtil.hsvToRgb(h, 1f, 1f);
            context.fill(hueX + px, hueY, hueX + px + step, hueY + hueH, 0xFF000000 | rgb);
        }
    }

    private void drawAlphaBar(DrawContext context) {
        // Checkerboard background
        int cellSize = hueH / 2;
        for (int px = 0; px < alphaW; px += cellSize) {
            int row = (px / cellSize) % 2;
            context.fill(alphaX + px, alphaY,
                    Math.min(alphaX + px + cellSize, alphaX + alphaW), alphaY + hueH,
                    row == 0 ? 0xFFAAAAAA : 0xFF666666);
        }
        // Colour overlay
        int curRgb = currentRgb();
        for (int px = 0; px < alphaW; px++) {
            float a = (float) px / alphaW;
            int aInt = (int)(a * 255);
            context.fill(alphaX + px, alphaY, alphaX + px + 1, alphaY + hueH,
                    (aInt << 24) | curRgb);
        }
    }

    private void drawPreviews(DrawContext context) {
        context.fill(previewOldX, previewY, previewOldX + previewSz, previewY + previewSz,
                ColorUtil.withAlpha(initialRgb, (int)(initialAlpha * 255)));
        context.fill(previewNewX, previewY, previewNewX + previewSz, previewY + previewSz,
                ColorUtil.withAlpha(currentRgb(), (int)(alpha * 255)));
    }

    private void drawSVHandle(DrawContext context) {
        int hx = svX + (int)(sat * svSize);
        int hy = svY + (int)((1f - val) * svSize);
        context.fill(hx - 3, hy - 1, hx + 4, hy + 2, 0xFFFFFFFF);
        context.fill(hx - 1, hy - 3, hx + 2, hy + 4, 0xFFFFFFFF);
    }

    private void drawHueHandle(DrawContext context) {
        int hx = hueX + (int)(hue / 360f * hueW);
        context.fill(hx - 1, hueY - 2, hx + 2, hueY + hueH + 2, 0xFFFFFFFF);
    }

    private void drawAlphaHandle(DrawContext context) {
        int ax = alphaX + (int)(alpha * alphaW);
        context.fill(ax - 1, alphaY - 2, ax + 2, alphaY + hueH + 2, 0xFFFFFFFF);
    }

    // ── Input ──────────────────────────────────────────────────────────────

        draggingSV = draggingHue = draggingAlpha = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSV)    { applySV(mx, my);    return true; }
        if (draggingHue)   { applyHue(mx, my);   return true; }
        if (draggingAlpha) { applyAlpha(mx, my); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private boolean inSV(double mx, double my) {
        return mx >= svX && mx < svX + svSize && my >= svY && my < svY + svSize;
    }
    private boolean inHue(double mx, double my) {
        return mx >= hueX && mx < hueX + hueW && my >= hueY && my < hueY + hueH;
    }
    private boolean inAlpha(double mx, double my) {
        return mx >= alphaX && mx < alphaX + alphaW && my >= alphaY && my < alphaY + hueH;
    }

    private void applySV(double mx, double my) {
        sat = (float) Math.max(0, Math.min(1, (mx - svX) / svSize));
        val = 1f - (float) Math.max(0, Math.min(1, (my - svY) / svSize));
        refreshHex();
    }
    private void applyHue(double mx, double my) {
        hue = (float) Math.max(0, Math.min(359.9, ((mx - hueX) / hueW) * 360));
        refreshHex();
    }
    private void applyAlpha(double mx, double my) {
        alpha = (float) Math.max(0, Math.min(1, (mx - alphaX) / alphaW));
    }

    private void onHexChanged(String hex) {
        if (hex.length() == 6) {
            int rgb = ColorUtil.fromHex(hex, -1);
            if (rgb >= 0) {
                float[] hsv = ColorUtil.rgbToHsv(
                        (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                hue = hsv[0]; sat = hsv[1]; val = hsv[2];
            }
        }
    }

    private void refreshHex() {
        if (hexField != null) hexField.setText(ColorUtil.toHex(currentRgb()));
    }

    private int currentRgb() { return ColorUtil.hsvToRgb(hue, sat, val); }

    private void resetToInitial() {
        float[] hsv = ColorUtil.rgbToHsv(
                (initialRgb >> 16) & 0xFF, (initialRgb >> 8) & 0xFF, initialRgb & 0xFF);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2]; alpha = initialAlpha;
        refreshHex();
    }

    private void finish() {
        if (onDone != null) {
            onDone.accept(ColorUtil.withAlpha(currentRgb(), (int)(alpha * 255)));
        }
        close();
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
