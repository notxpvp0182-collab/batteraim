package com.betteraim.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Wraps Minecraft's native {@link SliderWidget} with min/max bounds,
 * configurable decimal places, and a value-change callback.
 *
 * Rendering is entirely vanilla – the slider inherits Minecraft's normal
 * grey-pill-with-thumb appearance without any custom shaders or styles.
 *
 * Message format:
 *   – When {@code label} is blank → just the formatted number, e.g. "6"
 *   – Otherwise                   → "Label: 6.00"
 */
@Environment(EnvType.CLIENT)
public class BetterAimSlider extends SliderWidget {

    private final String           label;
    private final double           minVal;
    private final double           maxVal;
    private final int              decimals;
    private final Consumer<Double> onChanged;

    /**
     * @param label     Prefix shown inside the slider (pass "" to show value only)
     * @param current   Initial value – clamped to [min, max]
     * @param min       Minimum real value
     * @param max       Maximum real value
     * @param decimals  Decimal places (0 for integers)
     * @param onChanged Callback invoked on every value change
     */
    public BetterAimSlider(int x, int y, int width, int height,
                           String label, double current,
                           double min, double max,
                           int decimals, Consumer<Double> onChanged) {
        super(x, y, width, height,
                Text.empty(),
                normalize(current, min, max));
        this.label     = label == null ? "" : label;
        this.minVal    = min;
        this.maxVal    = max;
        this.decimals  = Math.max(0, decimals);
        this.onChanged = onChanged;
        updateMessage();  // set initial display text
    }

    // ── SliderWidget contract ──────────────────────────────────────────────

    @Override
    protected void updateMessage() {
        double actual = getActualValue();
        String formatted = String.format("%." + decimals + "f", actual);
        String msg = label.isEmpty() ? formatted : (label + ": " + formatted);
        setMessage(Text.literal(msg));
    }

    @Override
    protected void applyValue() {
        if (onChanged != null) onChanged.accept(getActualValue());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Current value mapped back to the original [min, max] range. */
    public double getActualValue() {
        return minVal + this.value * (maxVal - minVal);
    }

    /** Push an external value into the slider (e.g. after a config reset). */
    public void setActualValue(double val) {
        this.value = normalize(val, minVal, maxVal);
        updateMessage();
    }

    private static double normalize(double v, double min, double max) {
        if (max == min) return 0.0;
        return Math.max(0.0, Math.min(1.0, (v - min) / (max - min)));
    }
}
