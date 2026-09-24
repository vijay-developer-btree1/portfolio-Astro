package astro3d.ui;

import astro3d.model.*;

import java.awt.*;
import java.util.Locale;

/**
 * Next-gen astrology HUD: live geocentric chart, zodiac wheel,
 * planet table with sign + degree + ruler + element.
 */
public final class Hud {
    final SolarSystem ss;

    public Hud(SolarSystem ss) { this.ss = ss; }

    public void render(Graphics2D g, int w, int h, double simDays, double fps) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawTitleBar(g, w, simDays, fps);
        drawZodiacWheel(g, w, h);
        drawPlanetTable(g, w, h);
        drawFooter(g, w, h, simDays);
    }

    void drawTitleBar(Graphics2D g, int w, double simDays, double fps) {
        g.setColor(new Color(8, 6, 24, 170));
        g.fillRoundRect(14, 12, 330, 74, 14, 14);
        g.setColor(new Color(150, 130, 255, 90));
        g.drawRoundRect(14, 12, 330, 74, 14, 14);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(new Color(255, 220, 130));
        g.drawString("✧ ASTRO 3D — Celestial Observatory", 26, 38);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 255, 220));
        g.drawString("360° free-look · drag to orbit · wheel to zoom", 26, 58);
        g.setColor(new Color(150, 255, 200, 200));
        g.drawString(String.format(Locale.US, "Sim day %.1f   ·   %.0f FPS", simDays, fps), 26, 76);
    }

    /** Big animated zodiac wheel bottom-right with live planet markers. */
    void drawZodiacWheel(Graphics2D g, int w, int h) {
        int R = 96;
        int cx = w - R - 30, cy = h - R - 46;

        // disc
        g.setColor(new Color(10, 8, 30, 190));
        g.fillOval(cx - R, cy - R, R * 2, R * 2);
        g.setColor(new Color(160, 140, 255, 120));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(cx - R, cy - R, R * 2, R * 2);
        g.drawOval(cx - R + 16, cy - R + 16, (R - 16) * 2, (R - 16) * 2);

        for (int i = 0; i < 12; i++) {
            double a0 = Math.toRadians(i * 30 - 90);
            // spokes
            g.setColor(new Color(140, 130, 230, 80));
            g.drawLine(cx, cy, (int) (cx + Math.cos(a0) * R), (int) (cy + Math.sin(a0) * R));
            // glyph
            double am = Math.toRadians(i * 30 - 75);
            int gx = (int) (cx + Math.cos(am) * (R - 10));
            int gy = (int) (cy + Math.sin(am) * (R - 10));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.setColor(elementColor(i));
            g.drawString(Zodiac.GLYPHS[i], gx - 6, gy + 5);
        }

        // planet markers at their geocentric longitudes
        for (Body b : ss.bodies) {
            if (b.isSun) continue;
            double lon = Zodiac.geocentricLon(b, ss.sun);
            double a = Math.toRadians(lon - 90);
            int px = (int) (cx + Math.cos(a) * (R - 26));
            int py = (int) (cy + Math.sin(a) * (R - 26));
            g.setColor(b.color);
            g.fillOval(px - 4, py - 4, 8, 8);
            g.setColor(new Color(255, 255, 255, 170));
            g.drawOval(px - 4, py - 4, 8, 8);
        }
        // sun marker
        double slon = Zodiac.geocentricLon(new FakeBody(ss.sun.x, ss.sun.y, ss.sun.z), ss.sun);
        double sa = Math.toRadians(slon - 90);
        int sx = (int) (cx + Math.cos(sa) * (R - 26)), sy = (int) (cy + Math.sin(sa) * (R - 26));
        g.setColor(new Color(255, 210, 80));
        g.fillOval(sx - 5, sy - 5, 10, 10);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.setColor(new Color(190, 185, 255, 200));
        g.drawString("GEOCENTRIC WHEEL", cx - 52, cy + R + 16);
    }

    void drawPlanetTable(Graphics2D g, int w, int h) {
        int x = w - 285, y = 100;
        g.setColor(new Color(8, 6, 24, 175));
        g.fillRoundRect(x, y, 268, 262, 12, 12);
        g.setColor(new Color(150, 130, 255, 80));
        g.drawRoundRect(x, y, 268, 262, 12, 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(new Color(255, 220, 130));
        g.drawString("PLANETARY POSITIONS", x + 14, y + 20);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        int ry = y + 40;
        for (Body b : ss.bodies) {
            if (b.isSun) continue;
            double lon = Zodiac.geocentricLon(b, ss.sun);
            int s = Zodiac.signOf(lon);
            g.setColor(b.color);
            g.fillOval(x + 16, ry - 8, 7, 7);
            g.setColor(new Color(230, 230, 255));
            g.drawString(String.format(Locale.US, "%-9s", b.name), x + 30, ry);
            g.setColor(elementColor(s));
            g.drawString(Zodiac.GLYPHS[s] + " " + Zodiac.SIGNS[s], x + 96, ry);
            g.setColor(new Color(180, 190, 255, 210));
            double d = Zodiac.degreeInSign(lon);
            g.drawString(String.format(Locale.US, "%4.1f°  %s", d, Zodiac.RULERS[s]), x + 176, ry);
            ry += 18;
        }
        // Sun + Moon rows
        double slon = Zodiac.angleLon(ss.sun.x - ss.earth.x, ss.sun.z - ss.earth.z);
        int s = Zodiac.signOf(slon);
        g.setColor(new Color(255, 210, 80));
        g.fillOval(x + 16, ry - 8, 7, 7);
        g.setColor(new Color(230, 230, 255));
        g.drawString(String.format(Locale.US, "%-9s", "Sun"), x + 30, ry);
        g.setColor(elementColor(s));
        g.drawString(Zodiac.GLYPHS[s] + " " + Zodiac.SIGNS[s], x + 96, ry);
        g.setColor(new Color(180, 190, 255, 210));
        g.drawString(String.format(Locale.US, "%4.1f°  %s", Zodiac.degreeInSign(slon), Zodiac.RULERS[s]), x + 176, ry);
        ry += 18;

        double mlon = Zodiac.angleLon(ss.moon.x - ss.earth.x, ss.moon.z - ss.earth.z);
        s = Zodiac.signOf(mlon);
        g.setColor(new Color(220, 220, 235));
        g.fillOval(x + 16, ry - 8, 7, 7);
        g.setColor(new Color(230, 230, 255));
        g.drawString(String.format(Locale.US, "%-9s", "Moon"), x + 30, ry);
        g.setColor(elementColor(s));
        g.drawString(Zodiac.GLYPHS[s] + " " + Zodiac.SIGNS[s], x + 96, ry);
        g.setColor(new Color(180, 190, 255, 210));
        g.drawString(String.format(Locale.US, "%4.1f°", Zodiac.degreeInSign(mlon)), x + 176, ry);
    }

    void drawFooter(Graphics2D g, int w, int h, double simDays) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(new Color(190, 195, 255, 190));
        g.drawString("Keys: 1-9 focus planet · F follow · O orbits · B belt · C constellations · Space pause · R reset · +/- speed", 16, h - 18);
        String mode = Focus.get() == null ? "free view" : ("following " + Focus.get().name);
        g.setColor(new Color(150, 255, 200, 210));
        g.drawString("Mode: " + mode, 16, h - 36);
    }

    static Color elementColor(int signIdx) {
        return switch (signIdx % 4) {
            case 0 -> new Color(255, 150, 90);    // fire
            case 1 -> new Color(190, 235, 150);   // earth
            case 2 -> new Color(150, 210, 255);   // air
            default -> new Color(170, 160, 255);  // water
        };
    }

    /** Lightweight body view for the sun marker. */
    static final class FakeBody extends Body {
        FakeBody(double x, double y, double z) {
            super("Sun", "Surya", 0, 1, 0, 0, Color.WHITE, Color.WHITE, 0, 0, 0, true, 0, 0);
            this.x = x; this.y = y; this.z = z;
        }
    }
}
