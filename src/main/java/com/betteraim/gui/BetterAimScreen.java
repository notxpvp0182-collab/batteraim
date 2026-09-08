package com.betteraim.gui;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.crosshair.CrosshairStyle;
import com.betteraim.gui.widget.BetterAimSlider;
import com.betteraim.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Main Better AIM configuration screen.
 *
 * Visual style  : native Minecraft widgets only – ButtonWidget, SliderWidget,
 *                 text via DrawContext.  No custom shaders or modern UI elements.
 * Layout        : fully responsive – computed from actual screen/GUI dimensions.
 * Organisation  : tab bar (top) + paginated two-column settings (middle) +
 *                 navigation/done bar (bottom).
 */
@Environment(EnvType.CLIENT)
public class BetterAimScreen extends Screen {

    // ── Tabs ──────────────────────────────────────────────────────────────
    private enum Tab {
        CROSSHAIR("Crosshair"),
        TARGET("Target"),
        HITBOX("Hitbox"),
        HUD("HUD"),
        GENERAL("General");

        final String label;
        Tab(String label) { this.label = label; }
    }

    // ── Layout constants ──────────────────────────────────────────────────
    private static final int TAB_H     = 20;
    private static final int ROW_H     = 24;
    private static final int PAD_SIDE  = 8;
    private static final int PAD_TOP   = 12;
    private static final int BOTTOM_H  = 34;
    private static final int BTN_H     = 20;
    private static final int MAX_W     = 480;

    // ── State ─────────────────────────────────────────────────────────────
    private final Screen parent;
    private Tab activeTab   = Tab.CROSSHAIR;
    private int currentPage = 0;
    private int totalPages  = 1;

    // ── Computed layout ───────────────────────────────────────────────────
    private int contentX, contentW;
    private int contentY, contentH;
    private int rowsPerPage;

    public BetterAimScreen(Screen parent) {
        super(Text.literal("Better AIM"));
        this.parent = parent;
    }

    // ── Screen lifecycle ───────────────────────────────────────────────────

    @Override
    protected void init() {
        computeLayout();
        rebuildAll();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 2, 0xFFFFFF);

        // Active-tab underline
        Tab[] tabs = Tab.values();
        int tabW = contentW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == activeTab) {
                int bx = contentX + i * tabW;
                context.fill(bx, PAD_TOP + TAB_H - 2, bx + tabW - 2, PAD_TOP + TAB_H, 0xFFAAAAAA);
                break;
            }
        }

        // Row labels for the current page
        List<RowDef> rows = buildRows(activeTab);
        int startIdx = currentPage * rowsPerPage;
        int endIdx   = Math.min(startIdx + rowsPerPage, rows.size());

        for (int i = startIdx; i < endIdx; i++) {
            int y = contentY + (i - startIdx) * ROW_H + 6;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(rows.get(i).label()),
                    contentX, y, 0xFFFFFF);
        }

        // Page counter
        if (totalPages > 1) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Page " + (currentPage + 1) + " / " + totalPages),
                    width / 2, height - BOTTOM_H + (BOTTOM_H - 9) / 2, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Layout ────────────────────────────────────────────────────────────

    private void computeLayout() {
        contentW     = Math.min(width - PAD_SIDE * 2, MAX_W);
        contentX     = (width - contentW) / 2;
        contentY     = PAD_TOP + TAB_H + 6;
        contentH     = height - contentY - BOTTOM_H;
        rowsPerPage  = Math.max(3, contentH / ROW_H);
    }

    private void rebuildAll() {
        clearChildren();
        addTabBar();
        addContentWidgets();
        addBottomBar();
    }

    // ── Tab bar ───────────────────────────────────────────────────────────

    private void addTabBar() {
        Tab[] tabs = Tab.values();
        int tabW = contentW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];
            int bx = contentX + i * tabW;
            int bw = (i == tabs.length - 1) ? (contentX + contentW - bx) : tabW;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(tab.label),
                    btn -> { activeTab = tab; currentPage = 0; rebuildAll(); })
                    .dimensions(bx, PAD_TOP, bw - 2, TAB_H)
                    .build());
        }
    }

    // ── Content area ──────────────────────────────────────────────────────

    private void addContentWidgets() {
        List<RowDef> rows = buildRows(activeTab);
        totalPages  = Math.max(1, (rows.size() + rowsPerPage - 1) / rowsPerPage);
        currentPage = Math.min(currentPage, totalPages - 1);

        int startIdx = currentPage * rowsPerPage;
        int endIdx   = Math.min(startIdx + rowsPerPage, rows.size());

        // Right-column: half content width, starting from centre
        int ctrlW = contentW / 2 - 4;
        int ctrlX = contentX + contentW / 2 + 4;

        for (int i = startIdx; i < endIdx; i++) {
            int y = contentY + (i - startIdx) * ROW_H + 2;
            rows.get(i).addWidget(this, ctrlX, y, ctrlW, BTN_H);
        }
    }

    // ── Bottom navigation bar ─────────────────────────────────────────────

    private void addBottomBar() {
        int by = height - BOTTOM_H + (BOTTOM_H - BTN_H) / 2;

        if (totalPages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"),
                    btn -> { if (currentPage > 0) { currentPage--; rebuildAll(); } })
                    .dimensions(contentX, by, 58, BTN_H).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"),
                    btn -> { if (currentPage < totalPages - 1) { currentPage++; rebuildAll(); } })
                    .dimensions(contentX + contentW - 58, by, 58, BTN_H).build());
        }

        // Reset-tab button
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Tab"),
                btn -> { resetTab(); rebuildAll(); })
                .dimensions(contentX + contentW / 2 - 98, by, 90, BTN_H).build());

        // Done button
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> close())
                .dimensions(contentX + contentW / 2 + 8, by, 90, BTN_H).build());
    }

    // ── Row definition ─────────────────────────────────────────────────────

    @FunctionalInterface
    interface WidgetAdder {
        void add(BetterAimScreen screen, int x, int y, int w, int h);
    }

    record RowDef(String label, WidgetAdder adder) {
        void addWidget(BetterAimScreen screen, int x, int y, int w, int h) {
            adder.add(screen, x, y, w, h);
        }
    }

    // ── Row builders per tab ───────────────────────────────────────────────

    private List<RowDef> buildRows(Tab tab) {
        BetterAimConfig cfg = ConfigManager.getConfig();
        List<RowDef> rows   = new ArrayList<>();
        switch (tab) {
            case CROSSHAIR -> buildCrosshairRows(rows, cfg);
            case TARGET    -> buildTargetRows(rows, cfg);
            case HITBOX    -> buildHitboxRows(rows, cfg);
            case HUD       -> buildHudRows(rows, cfg);
            case GENERAL   -> buildGeneralRows(rows, cfg);
        }
        return rows;
    }

    // ─── Crosshair ────────────────────────────────────────────────────────

    private void buildCrosshairRows(List<RowDef> rows, BetterAimConfig cfg) {
        // Normal crosshair
        rows.add(section("Normal Crosshair"));
        rows.add(toggle("  Enable",
                () -> cfg.normalCrosshair.enabled,
                v  -> cfg.normalCrosshair.enabled = v));
        rows.add(styleRow("  Style", cfg.normalCrosshair));
        rows.add(sliderInt("  Size",      () -> cfg.normalCrosshair.size,      1, 32, v -> cfg.normalCrosshair.size      = v));
        rows.add(sliderInt("  Thickness", () -> cfg.normalCrosshair.thickness, 1, 16, v -> cfg.normalCrosshair.thickness = v));
        rows.add(sliderInt("  Gap",       () -> cfg.normalCrosshair.gap,       0, 20, v -> cfg.normalCrosshair.gap       = v));
        rows.add(sliderF("  Opacity",     () -> (double) cfg.normalCrosshair.opacity, 0.05, 1.0, 2, v -> cfg.normalCrosshair.opacity = (float) v));
        rows.add(colorRow("  Color",
                () -> ColorUtil.withAlpha(cfg.normalCrosshair.color, (int)(cfg.normalCrosshair.opacity * 255)),
                argb -> { cfg.normalCrosshair.color = argb & 0xFFFFFF; cfg.normalCrosshair.opacity = ColorUtil.af(argb); }));

        // Hitable crosshair
        rows.add(section("Hitable Crosshair"));
        rows.add(toggle("  Enable",
                () -> cfg.hitableCrosshair.enabled,
                v  -> cfg.hitableCrosshair.enabled = v));
        rows.add(styleRow("  Style", cfg.hitableCrosshair));
        rows.add(sliderInt("  Size",      () -> cfg.hitableCrosshair.size,      1, 32, v -> cfg.hitableCrosshair.size      = v));
        rows.add(sliderInt("  Thickness", () -> cfg.hitableCrosshair.thickness, 1, 16, v -> cfg.hitableCrosshair.thickness = v));
        rows.add(sliderInt("  Gap",       () -> cfg.hitableCrosshair.gap,       0, 20, v -> cfg.hitableCrosshair.gap       = v));
        rows.add(sliderF("  Opacity",     () -> (double) cfg.hitableCrosshair.opacity, 0.05, 1.0, 2, v -> cfg.hitableCrosshair.opacity = (float) v));
        rows.add(colorRow("  Color",
                () -> ColorUtil.withAlpha(cfg.hitableCrosshair.color, (int)(cfg.hitableCrosshair.opacity * 255)),
                argb -> { cfg.hitableCrosshair.color = argb & 0xFFFFFF; cfg.hitableCrosshair.opacity = ColorUtil.af(argb); }));

        // Perspective
        rows.add(section("Camera Perspective"));
        rows.add(toggle("  First Person",  () -> cfg.crosshairFirstPerson, v -> cfg.crosshairFirstPerson = v));
        rows.add(toggle("  Third Person",  () -> cfg.crosshairThirdPerson, v -> cfg.crosshairThirdPerson = v));
        rows.add(toggle("  Other Cameras", () -> cfg.crosshairOtherCamera, v -> cfg.crosshairOtherCamera = v));
    }

    // ─── Target ───────────────────────────────────────────────────────────

    private void buildTargetRows(List<RowDef> rows, BetterAimConfig cfg) {
        rows.add(section("Player Highlight"));
        rows.add(toggle("  Enable", () -> cfg.playerHighlight.enabled, v -> cfg.playerHighlight.enabled = v));
        rows.add(sliderF("  Opacity", () -> (double) cfg.playerHighlight.outlineOpacity, 0.0, 1.0, 2,
                v -> cfg.playerHighlight.outlineOpacity = (float) v));
        rows.add(colorRow("  Outline Color",
                () -> ColorUtil.withAlpha(cfg.playerHighlight.outlineColor, (int)(cfg.playerHighlight.outlineOpacity * 255)),
                argb -> { cfg.playerHighlight.outlineColor = argb & 0xFFFFFF; cfg.playerHighlight.outlineOpacity = ColorUtil.af(argb); }));
        rows.add(sliderF("  Outline Width", () -> (double) cfg.playerHighlight.outlineWidth, 0.5, 8.0, 1,
                v -> cfg.playerHighlight.outlineWidth = (float) v));

        rows.add(section("Mob Highlight"));
        rows.add(toggle("  Enable", () -> cfg.mobHighlight.enabled, v -> cfg.mobHighlight.enabled = v));
        rows.add(sliderF("  Opacity", () -> (double) cfg.mobHighlight.outlineOpacity, 0.0, 1.0, 2,
                v -> cfg.mobHighlight.outlineOpacity = (float) v));
        rows.add(colorRow("  Outline Color",
                () -> ColorUtil.withAlpha(cfg.mobHighlight.outlineColor, (int)(cfg.mobHighlight.outlineOpacity * 255)),
                argb -> { cfg.mobHighlight.outlineColor = argb & 0xFFFFFF; cfg.mobHighlight.outlineOpacity = ColorUtil.af(argb); }));

        rows.add(section("Hit Feedback"));
        rows.add(toggle("  Enable",             () -> cfg.hitFeedback.enabled,           v -> cfg.hitFeedback.enabled = v));
        rows.add(toggle("  Crosshair Feedback",  () -> cfg.hitFeedback.crosshairFeedback, v -> cfg.hitFeedback.crosshairFeedback = v));
        rows.add(toggle("  Highlight Feedback",  () -> cfg.hitFeedback.highlightFeedback, v -> cfg.hitFeedback.highlightFeedback = v));
        rows.add(toggle("  HUD Feedback",        () -> cfg.hitFeedback.hudFeedback,       v -> cfg.hitFeedback.hudFeedback = v));
        rows.add(colorRow("  Hit Color",
                () -> ColorUtil.withAlpha(cfg.hitFeedback.color, (int)(cfg.hitFeedback.opacity * 255)),
                argb -> { cfg.hitFeedback.color = argb & 0xFFFFFF; cfg.hitFeedback.opacity = ColorUtil.af(argb); }));
        rows.add(sliderInt("  Duration (ticks)", () -> cfg.hitFeedback.durationTicks, 1, 40,
                v -> cfg.hitFeedback.durationTicks = v));
    }

    // ─── Hitbox ───────────────────────────────────────────────────────────

    private void buildHitboxRows(List<RowDef> rows, BetterAimConfig cfg) {
        rows.add(section("Hitbox"));
        rows.add(toggle("  Enable",       () -> cfg.hitbox.enabled,     v -> cfg.hitbox.enabled = v));
        rows.add(toggle("  Show Players", () -> cfg.hitbox.showPlayers, v -> cfg.hitbox.showPlayers = v));
        rows.add(toggle("  Show Mobs",    () -> cfg.hitbox.showMobs,    v -> cfg.hitbox.showMobs = v));
        rows.add(colorRow("  Line Color",
                () -> ColorUtil.withAlpha(cfg.hitbox.color, (int)(cfg.hitbox.opacity * 255)),
                argb -> { cfg.hitbox.color = argb & 0xFFFFFF; cfg.hitbox.opacity = ColorUtil.af(argb); }));
        rows.add(sliderF("  Line Width", () -> (double) cfg.hitbox.lineWidth, 0.5, 8.0, 1,
                v -> cfg.hitbox.lineWidth = (float) v));
        rows.add(toggle("  Fill", () -> cfg.hitbox.fill, v -> cfg.hitbox.fill = v));
        rows.add(colorRow("  Fill Color",
                () -> ColorUtil.withAlpha(cfg.hitbox.fillColor, (int)(cfg.hitbox.fillOpacity * 255)),
                argb -> { cfg.hitbox.fillColor = argb & 0xFFFFFF; cfg.hitbox.fillOpacity = ColorUtil.af(argb); }));
    }

    // ─── HUD ──────────────────────────────────────────────────────────────

    private void buildHudRows(List<RowDef> rows, BetterAimConfig cfg) {
        rows.add(section("Target HUD"));
        rows.add(toggle("  Enable",     () -> cfg.targetHud.enabled,     v -> cfg.targetHud.enabled = v));
        rows.add(toggle("  Show Label", () -> cfg.targetHud.showLabel,   v -> cfg.targetHud.showLabel = v));
        rows.add(toggle("  Show Name",  () -> cfg.targetHud.showName,    v -> cfg.targetHud.showName = v));
        rows.add(toggle("  3D Preview", () -> cfg.targetHud.showPreview, v -> cfg.targetHud.showPreview = v));
        rows.add(sliderF("  Scale", () -> (double) cfg.targetHud.scale, 0.5, 2.5, 1,
                v -> cfg.targetHud.scale = (float) v));

        rows.add(section("Background"));
        rows.add(toggle("  Show Background", () -> cfg.targetHud.background, v -> cfg.targetHud.background = v));
        rows.add(colorRow("  BG Color",
                () -> ColorUtil.withAlpha(cfg.targetHud.bgColor, (int)(cfg.targetHud.bgOpacity * 255)),
                argb -> { cfg.targetHud.bgColor = argb & 0xFFFFFF; cfg.targetHud.bgOpacity = ColorUtil.af(argb); }));

        rows.add(section("Border"));
        rows.add(toggle("  Show Border", () -> cfg.targetHud.border, v -> cfg.targetHud.border = v));
        rows.add(colorRow("  Border Color",
                () -> ColorUtil.withAlpha(cfg.targetHud.borderColor, (int)(cfg.targetHud.borderOpacity * 255)),
                argb -> { cfg.targetHud.borderColor = argb & 0xFFFFFF; cfg.targetHud.borderOpacity = ColorUtil.af(argb); }));
        rows.add(sliderInt("  Border Width", () -> cfg.targetHud.borderWidth, 1, 6,
                v -> cfg.targetHud.borderWidth = v));

        rows.add(section("Text / Position"));
        rows.add(colorRow("  Text Color",
                () -> ColorUtil.withAlpha(cfg.targetHud.textColor, (int)(cfg.targetHud.textOpacity * 255)),
                argb -> { cfg.targetHud.textColor = argb & 0xFFFFFF; cfg.targetHud.textOpacity = ColorUtil.af(argb); }));
        // Position: -1 = auto, otherwise pixel coordinate
        rows.add(sliderInt("  Pos X (-1=auto)", () -> cfg.targetHud.posX, -1, 1000,
                v -> cfg.targetHud.posX = v));
        rows.add(sliderInt("  Pos Y (-1=auto)", () -> cfg.targetHud.posY, -1, 600,
                v -> cfg.targetHud.posY = v));
    }

    // ─── General ──────────────────────────────────────────────────────────

    private void buildGeneralRows(List<RowDef> rows, BetterAimConfig cfg) {
        rows.add(toggle("Better AIM Enabled", () -> cfg.enabled, v -> cfg.enabled = v));

        rows.add(section("Keybinds  (change in Controls)"));
        rows.add(info("Open Config: M (default)"));
        rows.add(info("Toggle Crosshair: unbound"));
        rows.add(info("Toggle Target HUD: unbound"));
        rows.add(info("Toggle Highlight: unbound"));
        rows.add(info("Toggle Hitbox: unbound"));

        rows.add(section("Reset"));
        rows.add(new RowDef("", (scr, x, y, w, h) ->
                scr.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Reset All Settings"),
                        btn -> { ConfigManager.resetAll(); rebuildAll(); })
                        .dimensions(x, y, w, h).build())));
    }

    // ── Row factory helpers ────────────────────────────────────────────────

    /** Toggle button (ON / OFF). */
    private RowDef toggle(String label, BooleanGetter getter, Consumer<Boolean> setter) {
        return new RowDef(label, (scr, x, y, w, h) -> {
            ButtonWidget btn = ButtonWidget.builder(
                    onOff(getter.get()),
                    b -> {
                        boolean nv = !getter.get();
                        setter.accept(nv);
                        save();
                        b.setMessage(onOff(nv));
                    })
                    .dimensions(x, y, w, h).build();
            scr.addDrawableChild(btn);
        });
    }

    /** Float slider. */
    private RowDef sliderF(String label, java.util.function.DoubleSupplier getter,
                           double min, double max, int dec, Consumer<Double> setter) {
        return new RowDef(label, (scr, x, y, w, h) -> {
            BetterAimSlider sl = new BetterAimSlider(x, y, w, h,
                    "", getter.getAsDouble(), min, max, dec,
                    v -> { setter.accept(v); save(); });
            scr.addDrawableChild(sl);
        });
    }

    /** Integer slider. */
    private RowDef sliderInt(String label, java.util.function.IntSupplier getter,
                             int min, int max, Consumer<Integer> setter) {
        return new RowDef(label, (scr, x, y, w, h) -> {
            BetterAimSlider sl = new BetterAimSlider(x, y, w, h,
                    "", getter.getAsInt(), min, max, 0,
                    v -> { setter.accept((int) Math.round(v)); save(); });
            scr.addDrawableChild(sl);
        });
    }

    /** Cycle button for CrosshairStyle. */
    private RowDef styleRow(String label, BetterAimConfig.CrosshairSettings settings) {
        return new RowDef(label, (scr, x, y, w, h) -> {
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal("< " + settings.getStyle().displayName + " >"),
                    b -> {
                        CrosshairStyle next = settings.getStyle().next();
                        settings.style = next.id;
                        save();
                        b.setMessage(Text.literal("< " + next.displayName + " >"));
                    })
                    .dimensions(x, y, w, h).build();
            scr.addDrawableChild(btn);
        });
    }

    /**
     * Color button – opens the colour picker.
     * Uses an {@link IntSupplier} so the picker always opens with the CURRENT
     * config colour, not the value captured at row-build time.
     */
    private RowDef colorRow(String label, IntSupplier currentArgb, Consumer<Integer> onChanged) {
        return new RowDef(label, (scr, x, y, w, h) -> {
            ButtonWidget btn = ButtonWidget.builder(
                    colorBtnText(currentArgb.getAsInt()),
                    b -> {
                        if (scr.client == null) return;
                        int argb = currentArgb.getAsInt();
                        scr.client.setScreen(new ColorPickerScreen(
                                scr,
                                argb,
                                result -> {
                                    onChanged.accept(result);
                                    save();
                                    b.setMessage(colorBtnText(result));
                                }));
                    })
                    .dimensions(x, y, w, h).build();
            scr.addDrawableChild(btn);
        });
    }

    /** Non-interactive section header. */
    private RowDef section(String text) {
        return new RowDef("-- " + text + " --", (scr, x, y, w, h) -> {});
    }

    /** Non-interactive info text (right column shows nothing). */
    private RowDef info(String text) {
        return new RowDef("  " + text, (scr, x, y, w, h) -> {});
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    @FunctionalInterface
    interface BooleanGetter { boolean get(); }

    private static Text onOff(boolean v)       { return Text.literal(v ? "ON" : "OFF"); }
    private static Text colorBtnText(int argb) { return Text.literal("# " + ColorUtil.toHex(argb & 0xFFFFFF)); }
    private static void save()                 { ConfigManager.save(); }

    private void resetTab() {
        BetterAimConfig cfg = ConfigManager.getConfig();
        switch (activeTab) {
            case CROSSHAIR -> {
                cfg.normalCrosshair  = BetterAimConfig.CrosshairSettings.defaultNormal();
                cfg.hitableCrosshair = BetterAimConfig.CrosshairSettings.defaultHitable();
                cfg.crosshairFirstPerson = true;
                cfg.crosshairThirdPerson = cfg.crosshairOtherCamera = false;
            }
            case TARGET -> {
                cfg.playerHighlight = BetterAimConfig.HighlightSettings.defaultPlayer();
                cfg.mobHighlight    = BetterAimConfig.HighlightSettings.defaultMob();
                cfg.hitFeedback     = new BetterAimConfig.HitFeedbackSettings();
            }
            case HITBOX  -> cfg.hitbox    = new BetterAimConfig.HitboxSettings();
            case HUD     -> cfg.targetHud = new BetterAimConfig.TargetHudSettings();
            case GENERAL -> cfg.enabled   = true;
        }
        save();
    }
}
