package com.betteraim.crosshair;

public enum CrosshairStyle {
    CROSS       ("cross",        "Cross"),
    DOT         ("dot",          "Dot"),
    CIRCLE      ("circle",       "Circle"),
    HOLLOW_CIRCLE("hollow_circle","Hollow Circle"),
    SMALL_BOX   ("small_box",   "Small Box"),
    LARGE_BOX   ("large_box",   "Large Box"),
    HOLLOW_BOX  ("hollow_box",  "Hollow Box"),
    FOUR_CORNERS("four_corners", "Four Corners"),
    DIAGONAL    ("diagonal",    "Diagonal X"),
    MINIMAL     ("minimal",     "Minimal"),
    ARROW       ("arrow",       "Arrow");

    public final String id;
    public final String displayName;

    CrosshairStyle(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static CrosshairStyle fromId(String id) {
        if (id == null) return CROSS;
        for (CrosshairStyle s : values()) {
            if (s.id.equals(id)) return s;
        }
        return CROSS;
    }

    public CrosshairStyle next() {
        CrosshairStyle[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }

    public CrosshairStyle prev() {
        CrosshairStyle[] vals = values();
        int idx = ordinal() - 1;
        if (idx < 0) idx = vals.length - 1;
        return vals[idx];
    }
}
