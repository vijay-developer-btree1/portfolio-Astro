package astro3d.model;

import java.awt.Color;

/** Elliptical comet with an animated ion + dust tail. */
public final class Comet {
    public double x, y, z;
    public double lonDeg = 0;

    // ellipse parameters (scene units)
    final double a = 38, e = 0.72;
    public double tailPhase = 0;
    public Color headColor = new Color(170, 240, 255);
    public Color tailColor = new Color(120, 200, 255, 90);

    /** Perihelion direction unit vector (normalized at t=0 angle basis). */
    double nx = Math.cos(0.8), nz = Math.sin(0.8);

    public void update(double simDays) {
        // faster near perihelion (Kepler-ish sweep approximation)
        double n = 2 * Math.PI / (365.25 * 6);          // ~6-year period
        double M = n * simDays;
        // solve Kepler's equation (2 iterations Newton)
        double E = M;
        for (int i = 0; i < 3; i++) E = E - (E - e * Math.sin(E) - M) / (1 - e * Math.cos(E));
        double b = a * Math.sqrt(1 - e * e);
        double ox = a * (Math.cos(E) - e);
        double oz = b * Math.sin(E);
        // rotate orbit by fixed angle
        double c = 0.8, s = Math.sin(0.8);
        x = ox * c - oz * s;
        z = ox * s + oz * c;
        y = Math.sin(E * 0.5) * 2.2;
        lonDeg = Math.toDegrees(Math.atan2(z, x));
        if (lonDeg < 0) lonDeg += 360;
        double dist = Math.sqrt(x * x + z * z);
        tailPhase = Math.max(0, 1.0 - dist / (a * 1.6));  // longer tail near sun
    }
}
