package astro3d.scene;

import astro3d.core.Camera;
import astro3d.core.Vec3;
import astro3d.model.Zodiac;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * Software 3D renderer: draws the sky sphere, orbits, bodies with glow,
 * rings, the comet with tail and the asteroid belt. Everything is drawn
 * with painter's-algorithm depth sorting for correctness and speed.
 */
public final class Renderer {
    final Starfield sky;
    final astro3d.model.SolarSystem ss;
    final Vec3 tmp = new Vec3();
    final Vec3 proj = new Vec3();

    public Renderer(astro3d.model.SolarSystem ss, Starfield sky) {
        this.ss = ss;
        this.sky = sky;
    }

    public void render(Graphics2D g, Camera cam, double time, boolean showOrbits, boolean showBelt, boolean showConst) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawNebulae(g, cam, time);
        drawStars(g, cam, time);
        drawMilkyWay(g, cam);
        if (showConst) drawZodiac(g, cam);
        drawMeteors(g, cam, time);

        // depth sort bodies (far -> near)
        var list = new java.util.ArrayList<astro3d.model.Body>(ss.bodies);
        list.sort((a, b) -> Double.compare(depth(b, cam), depth(a, cam)));

        if (showOrbits) drawOrbits(g, cam);
        if (showBelt) drawBelt(g, cam, time);

        for (var b : list) {
            if (b.isSun) drawSun(g, cam, b, time);
            else drawBody(g, cam, b);
        }
        drawMoon(g, cam);
        drawComet(g, cam, time);
    }

    double depth(astro3d.model.Body b, Camera cam) {
        double dx = b.x - cam.px, dy = b.y - cam.py, dz = b.z - cam.pz;
        return dx * (cam.m20) + dy * (cam.m21) + dz * (cam.m22);
    }

    boolean projBody(double wx, double wy, double wz, Camera cam, Vec3 out) {
        return tmp.set(wx, wy, wz).project(cam, out);
    }

    // ---------- sky ----------

    void drawStars(Graphics2D g, Camera cam, double time) {
        int w = cam.w, h = cam.h;
        for (int i = 0; i < Starfield.STAR_COUNT; i++) {
            if (!projBody(sky.sx[i] * 4000, sky.sy[i] * 4000, sky.sz[i] * 4000, cam, proj)) continue;
            double px = proj.x + w / 2.0, py = proj.y + h / 2.0;
            if (px < -20 || px > w + 20 || py < -20 || py > h + 20) continue;
            float br = sky.mag[i] * (0.72f + 0.28f * (float) Math.sin(time * 1.7 + sky.tw[i]));
            int size = sky.mag[i] > 0.75 ? 2 : 1;
            int c = starColor(sky.hue[i], br);
            g.setColor(new Color(c, true));
            g.fillRect((int) px, (int) py, size, size);
            if (sky.mag[i] > 0.92) {   // bright stars get a cross sparkle
                g.setColor(new Color(c & 0x40FFFFFF, true));
                g.drawLine((int) px - 3, (int) py, (int) px + 3, (int) py);
                g.drawLine((int) px, (int) py - 3, (int) px, (int) py + 3);
            }
        }
    }

    int starColor(float hue, float br) {
        int r, gr, b;
        if (hue < 0.25f)      { r = 170; gr = 190; b = 255; }  // blue-white
        else if (hue < 0.5f)  { r = 255; gr = 255; b = 255; }  // white
        else if (hue < 0.75f) { r = 255; gr = 230; b = 180; }  // warm
        else                  { r = 255; gr = 190; b = 150; }  // orange
        int a = (int) (255 * Math.min(1, 0.25 + br));
        r = (int) Math.min(255, r * (0.4 + br));
        gr = (int) Math.min(255, gr * (0.4 + br));
        b = (int) Math.min(255, b * (0.4 + br));
        return a << 24 | r << 16 | gr << 8 | b;
    }

    void drawMilkyWay(Graphics2D g, Camera cam) {
        g.setColor(new Color(255, 255, 255, 40));
        for (int i = 0; i < sky.mx.length; i += 2) {
            if (!projBody(sky.mx[i] * 4000, sky.my[i] * 4000, sky.mz[i] * 4000, cam, proj)) continue;
            int px = (int) (proj.x + cam.w / 2.0), py = (int) (proj.y + cam.h / 2.0);
            if (px < 0 || px >= cam.w || py < 0 || py >= cam.h) continue;
            int a = (int) (sky.mmag[i] * 90);
            g.setColor(new Color(210, 215, 255, a));
            g.fillRect(px, py, 1, 1);
        }
    }

    void drawNebulae(Graphics2D g, Camera cam, double time) {
        for (int i = 0; i < sky.nebX.length; i++) {
            if (!projBody(sky.nebX[i] * 4000, sky.nebY[i] * 4000, sky.nebZ[i] * 4000, cam, proj)) continue;
            double r = sky.nebR[i] * cam.focal * 4.5;
            int px = (int) (proj.x + cam.w / 2.0), py = (int) (proj.y + cam.h / 2.0);
            int rgb = sky.nebRGB[i];
            int alpha = 26 + (int) (8 * Math.sin(time * 0.4 + i));
            for (int ring = 4; ring >= 1; ring--) {
                float f = ring / 4.0f;
                g.setColor(new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                        (int) (alpha * f)));
                int rr = (int) (r * f * 0.5 + r * 0.5);
                g.fillOval((int) (px - rr), (int) (py - rr), rr * 2, rr * 2);
            }
        }
    }

    void drawMeteors(Graphics2D g, Camera cam, double time) {
        for (int i = 0; i < sky.metX.length; i++) {
            float life = sky.metLife[i];
            if (life <= 0) continue;
            // streak: from point toward origin of sky sphere
            double fx = sky.metX[i] * 4000, fy = sky.metY[i] * 4000, fz = sky.metZ[i] * 4000;
            if (!projBody(fx, fy, fz, cam, proj)) continue;
            int x2 = (int) (proj.x + cam.w / 2.0), y2 = (int) (proj.y + cam.h / 2.0);
            // tail direction approximated by projecting a slightly rotated point
            double ang = 0.035 * life;
            Vec3 d = tmp.set(fx, fy, fz).copy().rotY(ang);
            if (!projBody(d.x, d.y, d.z, cam, proj)) continue;
            int x1 = (int) (proj.x + cam.w / 2.0), y1 = (int) (proj.y + cam.h / 2.0);
            int a = (int) (200 * life);
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 255, 255, a));
            g.drawLine(x1, y1, x2, y2);
            g.setColor(new Color(180, 220, 255, a / 2));
            g.setStroke(new BasicStroke(4f));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    void drawZodiac(Graphics2D g, Camera cam) {
        for (int s = 0; s < sky.zodiacLines.length; s++) {
            double[][] pts = sky.zodiacLines[s];
            int[] pxs = new int[pts.length], pys = new int[pts.length];
            boolean ok = true;
            for (int i = 0; i < pts.length; i++) {
                if (!projBody(pts[i][0] * 4000, pts[i][1] * 4000, pts[i][2] * 4000, cam, proj)) { ok = false; break; }
                pxs[i] = (int) (proj.x + cam.w / 2.0);
                pys[i] = (int) (proj.y + cam.h / 2.0);
            }
            if (!ok) continue;
            g.setColor(new Color(140, 120, 255, 60));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                    new float[]{5, 6}, 0));
            for (int i = 0; i + 1 < pts.length; i++) g.drawLine(pxs[i], pys[i], pxs[i + 1], pys[i + 1]);
            // constellation stars
            g.setColor(new Color(200, 190, 255, 200));
            for (int i = 0; i < pts.length; i++) {
                g.fillOval(pxs[i] - 2, pys[i] - 2, 4, 4);
            }
            // sign glyph at centroid
            int cx = 0, cy = 0;
            for (int i = 0; i < pts.length; i++) { cx += pxs[i]; cy += pys[i]; }
            cx /= pts.length; cy /= pts.length;
            g.setColor(new Color(180, 170, 255, 90));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            g.drawString(Zodiac.GLYPHS[s], cx + 8, cy - 8);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ---------- orbits & belt ----------

    void drawOrbits(Graphics2D g, Camera cam) {
        for (var b : ss.bodies) {
            if (b.isSun) continue;
            g.setColor(new Color(b.color.getRed(), b.color.getGreen(), b.color.getBlue(), 45));
            drawEllipseOrbit(g, cam, b);
        }
    }

    /** Draw the orbit as a polyline of 128 points (handles inclination + node). */
    void drawEllipseOrbit(Graphics2D g, Camera cam, astro3d.model.Body b) {
        int N = 128;
        int prevX = 0, prevY = 0;
        boolean started = false;
        double ci = Math.cos(b.inclination), si = Math.sin(b.inclination);
        double cn = Math.cos(b.node), sn = Math.sin(b.node);
        for (int i = 0; i <= N; i++) {
            double ang = i * 2 * Math.PI / N;
            double ox = b.aAU * Math.cos(ang), oz = b.aAU * Math.sin(ang);
            double y1 = -oz * si, z1 = oz * ci;
            double x2 = ox * cn + z1 * sn, z2 = -ox * sn + z1 * cn;
            if (!projBody(x2, y1, z2, cam, proj)) { started = false; continue; }
            int px = (int) (proj.x + cam.w / 2.0), py = (int) (proj.y + cam.h / 2.0);
            if (started) g.drawLine(prevX, prevY, px, py);
            started = true; prevX = px; prevY = py;
        }
    }

    void drawBelt(Graphics2D g, Camera cam, double time) {
        g.setColor(new Color(180, 170, 150, 130));
        var r = new java.util.Random(42);
        for (int i = 0; i < 900; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double rad = 15.5 + r.nextGaussian() * 1.2;
            double y = r.nextGaussian() * 0.35;
            double rot = time * 0.014;
            double x = Math.cos(a + rot) * rad;
            double z = Math.sin(a + rot) * rad;
            if (!projBody(x, y, z, cam, proj)) continue;
            int px = (int) (proj.x + cam.w / 2.0), py = (int) (proj.y + cam.h / 2.0);
            if (px < 0 || px >= cam.w || py < 0 || py >= cam.h) continue;
            g.fillRect(px, py, 1, 1);
        }
    }

    // ---------- bodies ----------

    void drawSun(Graphics2D g, Camera cam, astro3d.model.Body b, double time) {
        if (!projBody(b.x, b.y, b.z, cam, proj)) return;
        int cx = (int) (proj.x + cam.w / 2.0), cy = (int) (proj.y + cam.h / 2.0);
        double scale = cam.focal / Math.max(1, proj.z);
        int r = (int) Math.max(6, b.radius * scale);

        // pulsing corona
        double pulse = 1 + 0.06 * Math.sin(time * 2.2);
        for (int ring = 7; ring >= 1; ring--) {
            float f = ring / 7.0f;
            int rr = (int) (r * 3.2 * f * pulse);
            g.setColor(new Color(255, 120 + (int) (80 * f), 0, (int) (26 * (1 - f) + 6)));
            g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
        // core
        g.setColor(new Color(255, 245, 200));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(255, 200, 60, 160));
        g.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);

        // sun rays
        g.setColor(new Color(255, 220, 120, 70));
        for (int i = 0; i < 12; i++) {
            double a = time * 0.3 + i * Math.PI / 6;
            int len = (int) (r * 3.6);
            g.drawLine(cx, cy, (int) (cx + Math.cos(a) * len), (int) (cy + Math.sin(a) * len));
        }
        label(g, cx, cy - r - 14, "SUN · Surya", new Color(255, 220, 120));
    }

    void drawBody(Graphics2D g, Camera cam, astro3d.model.Body b) {
        if (!projBody(b.x, b.y, b.z, cam, proj)) return;
        int cx = (int) (proj.x + cam.w / 2.0), cy = (int) (proj.y + cam.h / 2.0);
        double scale = cam.focal / Math.max(1, proj.z);
        int r = (int) Math.max(2, b.radius * scale);

        // soft glow
        for (int ring = 3; ring >= 1; ring--) {
            float f = ring / 3.0f;
            int rr = (int) (r * (1.6 + f * 1.4));
            g.setColor(new Color(b.glow.getRed(), b.glow.getGreen(), b.glow.getBlue(), (int) (30 * f + 8)));
            g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }

        // shaded sphere: lit from the sun direction
        double sdx = ss.sun.x - b.x, sdy = ss.sun.y - b.y, sdz = ss.sun.z - b.z;
        double sl = Math.sqrt(sdx * sdx + sdy * sdy + sdz * sdz);
        sdx /= sl; sdy /= sl; sdz /= sl;
        // light dir in screen space approx: offset highlight toward sun's screen pos
        if (projBody(ss.sun.x, ss.sun.y, ss.sun.z, cam, proj)) {
            int sx = (int) (proj.x + cam.w / 2.0), sy = (int) (proj.y + cam.h / 2.0);
            double lx = sx - cx, ly = sy - cy;
            double ll = Math.sqrt(lx * lx + ly * ly) + 1e-6;
            lx /= ll; ly /= ll;
            int steps = 6;
            for (int i = steps; i >= 0; i--) {
                float f = i / (float) steps;                 // 1 = full color, 0 = dark
                int rr = (int) (r * (1.0 - 0.0));            // same radius, offset center
                int ox = (int) (cx + lx * r * 0.42 * (1 - f));
                int oy = (int) (cy + ly * r * 0.42 * (1 - f));
                int cr = (int) (b.color.getRed() * (0.25 + 0.75 * f));
                int cg = (int) (b.color.getGreen() * (0.25 + 0.75 * f));
                int cb = (int) (b.color.getBlue() * (0.25 + 0.75 * f));
                g.setColor(new Color(cr, cg, cb));
                g.fillOval(ox - rr, oy - rr, rr * 2, rr * 2);
            }
        } else {
            g.setColor(b.color);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        // rings
        if (b.ringOuter > 0) {
            double ri = b.ringInner * scale, ro = b.ringOuter * scale;
            g.setColor(new Color(230, 210, 160, 110));
            g.setStroke(new BasicStroke(Math.max(1f, (float) ((ro - ri) * 0.5))));
            double sq = Math.abs(Math.sin(b.tilt)) * 0.85 + 0.15;
            g.drawOval((int) (cx - ro), (int) (cy - ro * sq), (int) (ro * 2), (int) (ro * 2 * sq));
            g.setStroke(new BasicStroke(1f));
        }

        // selection halo
        if (selectedName != null && selectedName.equals(b.name)) {
            g.setColor(new Color(255, 255, 255, 140));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6, 5}, 0));
            g.drawOval(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2);
            g.setStroke(new BasicStroke(1f));
        }

        String lbl = b.name + " · " + b.sanskrit;
        label(g, cx, cy - r - 10, lbl, mix(b.color, Color.WHITE, 0.55f));
        String zodi = Zodiac.formatLon(geoLon(b));
        label(g, cx, cy - r + 8 + r, zodi, new Color(190, 200, 255, 210), 11);
    }

    void drawMoon(Graphics2D g, Camera cam) {
        var b = ss.moon;
        if (!projBody(b.x, b.y, b.z, cam, proj)) return;
        int cx = (int) (proj.x + cam.w / 2.0), cy = (int) (proj.y + cam.h / 2.0);
        double scale = cam.focal / Math.max(1, proj.z);
        int r = (int) Math.max(1, b.radius * scale);
        g.setColor(new Color(225, 225, 235));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(150, 150, 165, 160));
        g.fillOval((int) (cx - r * 0.45), (int) (cy - r * 0.3), (int) (r * 0.7), (int) (r * 0.7));
        if (cam.dist < 30) label(g, cx, cy - r - 6, "Moon · Chandra", new Color(220, 220, 255), 11);
    }

    void drawComet(Graphics2D g, Camera cam, double time) {
        var c = ss.comet;
        if (!projBody(c.x, c.y, c.z, cam, proj)) return;
        int cx = (int) (proj.x + cam.w / 2.0), cy = (int) (proj.y + cam.h / 2.0);
        double scale = cam.focal / Math.max(1, proj.z);
        int r = (int) Math.max(2, 0.55 * scale);

        // tail: points away from sun on screen
        int tx = cx, ty = cy;
        if (projBody(ss.sun.x, ss.sun.y, ss.sun.z, cam, proj)) {
            int sx = (int) (proj.x + cam.w / 2.0), sy = (int) (proj.y + cam.h / 2.0);
            double dx = cx - sx, dy = cy - sy;
            double dl = Math.sqrt(dx * dx + dy * dy) + 1e-6;
            int tl = (int) (60 + 340 * c.tailPhase);
            for (int i = 0; i < 26; i++) {
                float f = i / 26.0f;
                int px = (int) (cx + dx / dl * tl * f);
                int py = (int) (cy + dy / dl * tl * f);
                int spread = (int) (6 + 26 * f);
                g.setColor(new Color(120, 200, 255, (int) (80 * (1 - f) * c.tailPhase)));
                g.fillOval(px - spread / 2, py - spread / 2, spread, spread);
            }
        }
        // head glow
        g.setColor(new Color(170, 240, 255, 220));
        g.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);
        g.setColor(Color.WHITE);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        label(g, cx, cy - r - 10, "Comet · Dhoomaketu", new Color(160, 230, 255), 11);
    }

    // ---------- helpers ----------

    double geoLon(astro3d.model.Body b) {
        var s = ss.sun;
        return Zodiac.geocentricLon(b, s);
    }

    public String selectedName;

    void label(Graphics2D g, int x, int y, String s, Color c) { label(g, x, y, s, c, 12); }

    void label(Graphics2D g, int x, int y, String s, Color c, int size) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
        var fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x - w / 2 - 4, y - fm.getAscent() - 1, w + 8, fm.getHeight() + 2, 6, 6);
        g.setColor(c);
        g.drawString(s, x - w / 2, y);
    }

    static Color mix(Color a, Color b, float t) {
        int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }
}
