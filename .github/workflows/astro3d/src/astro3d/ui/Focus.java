package astro3d.ui;

import astro3d.model.Body;

/** Currently focused/followed body (shared between HUD and input). */
public final class Focus {
    private static Body target;

    public static Body get() { return target; }
    public static void set(Body b) { target = b; }
    public static void clear() { target = null; }
}
