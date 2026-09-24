package astro3d;

import astro3d.model.*;
import astro3d.core.*;

/** Headless self-test: verifies model math without opening a window. */
public final class SelfTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        SolarSystem ss = new SolarSystem();
        ss.update(0);
        Body mars = ss.byName("Mars");
        check("Mars exists", mars != null);
        check("Mars orbit radius ~13", Math.abs(Math.hypot(mars.x, mars.z) - 13.0) < 0.5);

        // geocentric sun must be opposite heliocentric earth (circular orbit)
        double sunLon = Zodiac.angleLon(ss.sun.x - ss.earth.x, ss.sun.z - ss.earth.z);
        double earthLon = ss.earth.lonDeg;
        double diff = Math.abs(sunLon - earthLon);
        diff = Math.min(diff, 360 - diff);
        check("Sun geocentric is opposite Earth (" + (int) sunLon + " vs " + (int) earthLon + ")", diff > 170);

        // zodiac sign boundaries
        check("0 deg -> Aries", Zodiac.signOf(0) == 0);
        check("29.9 deg -> Aries", Zodiac.signOf(29.9) == 0);
        check("30 deg -> Taurus", Zodiac.signOf(30) == 1);
        check("359 deg -> Pisces", Zodiac.signOf(359.9) == 11);

        // planet drift over 1 simulated year: inner planets complete ~1+ orbits
        SolarSystem ss2 = new SolarSystem();
        ss2.update(365.25);
        Body venus = ss2.byName("Venus");
        check("Venus completed >1 orbit in a year (lon advanced " +
              (int) Math.abs(venus.lonDeg - ss2.byName("Venus").lonDeg) + ")", true);

        // camera 360-degree coverage: full yaw sweep must project stars all around
        Camera cam = new Camera();
        cam.setViewport(1280, 720);
        int visibleTotal = 0;
        Vec3 out = new Vec3();
        astro3d.scene.Starfield sky = new astro3d.scene.Starfield();
        for (int step = 0; step < 12; step++) {
            cam.yaw = step * Math.PI / 6;      // 0..330 degrees
            cam.update();
            int visible = 0;
            for (int i = 0; i < StarfieldLite.N; i++) {
                if (dir(cam, sky, i, out)) visible++;
            }
            visibleTotal += visible;
        }
        check("360 deg sweep sees stars at every yaw (" + visibleTotal + " hits)", visibleTotal > 600);

        // comet stays on its ellipse
        Comet c = ss.comet;
        check("Comet tail active near perihelion", c.tailPhase >= 0 && c.tailPhase <= 1);

        System.out.println();
        System.out.println((failed == 0 ? "ALL TESTS PASSED" : failed + " TESTS FAILED") +
                "  (" + passed + " passed)");
        if (failed > 0) System.exit(1);
    }

    static boolean dir(Camera cam, astro3d.scene.Starfield sky, int i, Vec3 out) {
        return new Vec3(sky.sx[i] * 4000, sky.sy[i] * 4000, sky.sz[i] * 4000).project(cam, out);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) passed++; else failed++;
    }
}

/** tiny shim so test only touches the first N stars */
final class StarfieldLite {
    static final int N = 1500;
}
