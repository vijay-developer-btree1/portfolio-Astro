package astro3d.model;

import java.awt.Color;

/** A celestial body: planet, sun or moon with astrological attributes. */
public class Body {
    public final String name;         // e.g. "Mars"
    public final String sanskrit;     // e.g. "Mangal"
    public final String sign;         // current zodiac sign name
    public final double aAU;          // semi-major axis (AU) - orbit radius
    public final double periodDays;   // orbital period
    public final double phase0;       // initial angle (rad)
    public final double radius;       // draw radius (scene units)
    public final Color color;
    public final Color glow;
    public final double ringInner, ringOuter;   // 0 = no ring
    public final double tilt;                    // visual tilt
    public final boolean isSun;
    public final double inclination;             // orbit inclination (rad)
    public final double node;                    // ascending node

    /** Live computed state. */
    public double x, y, z;            // scene position
    public double lonDeg;             // heliocentric ecliptic longitude (deg)

    public Body(String name, String sanskrit, double aAU, double periodDays, double phase0,
                double radius, Color color, Color glow,
                double ringInner, double ringOuter, double tilt,
                boolean isSun, double incDeg, double nodeDeg) {
        this.name = name;
        this.sanskrit = sanskrit;
        this.aAU = aAU;
        this.periodDays = periodDays;
        this.phase0 = phase0;
        this.radius = radius;
        this.color = color;
        this.glow = glow;
        this.ringInner = ringInner;
        this.ringOuter = ringOuter;
        this.tilt = tilt;
        this.isSun = isSun;
        this.inclination = Math.toRadians(incDeg);
        this.node = Math.toRadians(nodeDeg);
        this.sign = "Aries"; // replaced by Zodiac for display
    }

    /** Advance along circular orbit (good visual approximation). */
    public void update(double simDays) {
        double ang = phase0 + 2 * Math.PI * simDays / periodDays;
        // position in orbital plane
        double ox = aAU * Math.cos(ang);
        double oz = aAU * Math.sin(ang);
        double oy = 0;
        // inclination about node line
        double ci = Math.cos(inclination), si = Math.sin(inclination);
        double y1 = oy * ci - oz * si;
        double z1 = oy * si + oz * ci;
        // rotate by ascending node around Y
        double cn = Math.cos(node), sn = Math.sin(node);
        double x2 = ox * cn + z1 * sn;
        double z2 = -ox * sn + z1 * cn;
        this.x = x2;
        this.y = y1;
        this.z = z2;
        this.lonDeg = Math.toDegrees(Math.atan2(z2, x2));
        if (this.lonDeg < 0) this.lonDeg += 360;
    }
}
