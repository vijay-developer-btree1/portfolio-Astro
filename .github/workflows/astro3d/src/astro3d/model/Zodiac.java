package astro3d.model;

import java.util.Locale;

/** Zodiac definitions + geocentric longitude computation. */
public final class Zodiac {
    public static final String[] SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };
    public static final String[] GLYPHS = {
        "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"
    };
    public static final String[] RULERS = {
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    };
    public static final String[] ELEMENTS = {
        "Fire", "Earth", "Air", "Water", "Fire", "Earth", "Air", "Water", "Fire", "Earth", "Air", "Water"
    };

    /** Geocentric ecliptic longitude of a heliocentric planet (circular orbits), degrees. */
    public static double geocentricLon(Body p, Body sun) {
        double px = p.x - sun.x;
        double pz = p.z - sun.z;
        double py = p.y - sun.y;
        double lon = Math.toDegrees(Math.atan2(pz, px));
        // crude latitude correction ignored; fine for visualization
        return (lon + 360.0) % 360.0;
    }

    /** Ecliptic longitude of direction (dx,dz) as seen by an observer. */
    public static double angleLon(double dx, double dz) {
        return (Math.toDegrees(Math.atan2(dz, dx)) + 360.0) % 360.0;
    }

    /** Sign index for an ecliptic longitude (0=Aries). */
    public static int signOf(double lonDeg) {
        return (int) Math.floor((((lonDeg % 360) + 360) % 360) / 30.0) % 12;
    }

    /** Degrees within sign 0..30. */
    public static double degreeInSign(double lonDeg) {
        return (((lonDeg % 360) + 360) % 360) % 30.0;
    }

    /** Full label like "Scorpio 14°32'". */
    public static String formatLon(double lonDeg) {
        int s = signOf(lonDeg);
        double d = degreeInSign(lonDeg);
        int deg = (int) d;
        int min = (int) Math.round((d - deg) * 60);
        if (min == 60) { deg++; min = 0; }
        return String.format(Locale.US, "%s %d°%02d'", SIGNS[s], deg, min);
    }
}
