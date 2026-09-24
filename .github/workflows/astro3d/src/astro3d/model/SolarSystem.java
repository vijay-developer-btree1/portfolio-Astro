package astro3d.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The whole scene graph of astronomical objects. */
public final class SolarSystem {
    public final List<Body> bodies = new ArrayList<>();
    public final Body sun;
    public final Body moon;
    public final Body earth;
    public final Comet comet;
    public final List<double[]> asteroidBelt = new ArrayList<>(); // {x,y,z}
    public double simDays = 0;

    public SolarSystem() {
        sun = add(new Body("Sun", "surya", 0, 1, 0, 4.2,
                new Color(255, 200, 60), new Color(255, 140, 0),
                0, 0, 0, true, 0, 0));

        earth = add(new Body("Earth", "Prithvi", 10.0, 365.25, 0.6, 1.15,
                new Color(70, 130, 240), new Color(120, 180, 255),
                0, 0, 0.41, false, 0, 0));

        moon = new Body("Moon", "Chandra", 0, 27.32, 1.1, 0.32,
                new Color(210, 210, 220), new Color(240, 240, 255),
                0, 0, 0, false, 0, 0);

        add(new Body("Mercury", "Budha", 5.2, 87.97, 2.4, 0.5,
                new Color(169, 169, 169), new Color(220, 220, 220),
                0, 0, 0, false, 7.0, 48));
        add(new Body("Venus", "Shukra", 7.0, 224.7, 4.1, 0.95,
                new Color(240, 200, 140), new Color(255, 230, 180),
                0, 0, 0.05, false, 3.4, 76));
        add(new Body("Mars", "Mangal", 13.0, 686.98, 5.2, 0.7,
                new Color(220, 90, 50), new Color(255, 120, 70),
                0, 0, 0.44, false, 1.85, 49));
        add(new Body("Jupiter", "Guru", 18.0, 4332.59, 1.9, 2.4,
                new Color(215, 170, 110), new Color(255, 220, 170),
                0, 0, 0.05, false, 1.3, 100));
        add(new Body("Saturn", "Shani", 24.0, 10759.22, 3.3, 2.0,
                new Color(220, 200, 140), new Color(255, 240, 190),
                3.0, 4.6, 0.47, false, 2.5, 113));
        add(new Body("Uranus", "Varuna", 30.0, 30688.5, 0.2, 1.5,
                new Color(140, 225, 230), new Color(200, 250, 255),
                0, 0, 1.71, false, 0.77, 74));
        add(new Body("Neptune", "Varuna II", 35.5, 60182, 2.9, 1.45,
                new Color(70, 100, 230), new Color(120, 150, 255),
                0, 0, 0.49, false, 1.77, 131));

        comet = new Comet();
        makeAsteroidBelt();
    }

    private Body add(Body b) { bodies.add(b); return b; }

    private void makeAsteroidBelt() {
        Random r = new Random(42);
        for (int i = 0; i < 900; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double rad = 15.5 + r.nextGaussian() * 1.2;
            double y = (r.nextGaussian() * 0.35);
            double speed = 0.014;
            asteroidBelt.add(new double[]{ Math.cos(a) * rad, y, Math.sin(a) * rad, speed });
        }
    }

    public void update(double dtDays) {
        simDays += dtDays;
        for (Body b : bodies) b.update(simDays);
        // Moon orbits Earth
        double ang = 1.1 + 2 * Math.PI * simDays / moon.periodDays;
        double md = 1.9;
        moon.x = earth.x + Math.cos(ang) * md;
        moon.y = earth.y + Math.sin(ang * 0.9) * 0.3;
        moon.z = earth.z + Math.sin(ang) * md;
        moon.lonDeg = Math.toDegrees(Math.atan2(moon.z, moon.x));
        if (moon.lonDeg < 0) moon.lonDeg += 360;
        comet.update(simDays);
        // asteroids: slow prograde rotation of the belt
        // (positions recomputed each frame in renderer for cheapness)
    }

    /** Get body by name, e.g. focusPlanet("Mars"). */
    public Body byName(String n) {
        for (Body b : bodies) if (b.name.equalsIgnoreCase(n)) return b;
        return null;
    }
}
