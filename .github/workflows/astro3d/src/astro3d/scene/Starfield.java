package astro3d.scene;

import astro3d.model.Zodiac;

import java.util.Random;

/**
 * Celestial sphere: 6000 stars, milky-way band, colored nebulae,
 * zodiac constellations and random meteors. Rendered with an
 * infinite-distance sky so camera rotation feels like real 360 VR.
 */
public final class Starfield {
    public static final int STAR_COUNT = 6000;

    public final float[] sx = new float[STAR_COUNT];   // unit-sphere direction
    public final float[] sy = new float[STAR_COUNT];
    public final float[] sz = new float[STAR_COUNT];
    public final float[] mag = new float[STAR_COUNT];  // 0..1 brightness
    public final float[] hue = new float[STAR_COUNT];  // 0..1 color tint
    public final float[] tw = new float[STAR_COUNT];   // twinkle phase

    // Milky way band: dense stars near a great circle
    final float[] mx = new float[2600];
    final float[] my = new float[2600];
    final float[] mz = new float[2600];
    final float[] mmag = new float[2600];

    // Nebula puffs (drawn as big soft translucent circles on the sky)
    public final float[] nebX = new float[8];
    public final float[] nebY = new float[8];
    public final float[] nebZ = new float[8];
    public final float[] nebR = new float[8];
    public final int[] nebRGB = new int[8];

    // Meteors
    public final float[] metX = new float[6], metY = new float[6], metZ = new float[6];
    public final float[] metLife = new float[6];
    final Random rnd = new Random(7);

    // Zodiac constellation stars (rough classical shapes)
    public double[][][] zodiacLines;  // [sign][point]{x,y,z} unit vectors
    public String[] zodiacNames;

    public Starfield() {
        Random r = new Random(1234);
        for (int i = 0; i < STAR_COUNT; i++) {
            // uniform on sphere
            double u = r.nextDouble() * 2 - 1;
            double th = r.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            sx[i] = (float) (s * Math.cos(th));
            sy[i] = (float) u;
            sz[i] = (float) (s * Math.sin(th));
            mag[i] = (float) Math.pow(r.nextDouble(), 2.2);
            hue[i] = r.nextFloat();
            tw[i] = r.nextFloat() * (float) (Math.PI * 2);
        }
        // milky way: concentrate near plane tilted ~60°
        double tilt = Math.toRadians(60);
        for (int i = 0; i < mx.length; i++) {
            double th = r.nextDouble() * Math.PI * 2;
            double spread = r.nextGaussian() * 0.16;
            double x = Math.cos(th), z = Math.sin(th), y = spread;
            // tilt plane
            double y2 = y * Math.cos(tilt) - z * Math.sin(tilt);
            double z2 = y * Math.sin(tilt) + z * Math.cos(tilt);
            mx[i] = (float) x; my[i] = (float) y2; mz[i] = (float) z2;
            mmag[i] = (float) (0.25 + r.nextDouble() * 0.75);
        }
        // nebulae
        int[][] cols = {
            {120, 60, 200}, {220, 60, 120}, {40, 120, 220},
            {200, 90, 40}, {60, 200, 160}, {170, 60, 220},
            {220, 140, 60}, {90, 60, 220}
        };
        for (int i = 0; i < nebX.length; i++) {
            double u = r.nextDouble() * 2 - 1;
            double th = r.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            nebX[i] = (float) (s * Math.cos(th));
            nebY[i] = (float) u;
            nebZ[i] = (float) (s * Math.sin(th));
            nebR[i] = 0.10f + r.nextFloat() * 0.16f;
            nebRGB[i] = cols[i][0] << 16 | cols[i][1] << 8 | cols[i][2];
        }
        buildZodiacShapes();
    }

    private void buildZodiacShapes() {
        // Rough constellation line segments per sign (lon/lat degrees)
        double[][][] defs = {
            {{20, 8}, {25, 15}, {28, 5}, {35, 10}},                    // Aries
            {{60, 10}, {68, 18}, {75, 8}, {82, 14}},                   // Taurus
            {{95, 15}, {100, 22}, {108, 12}, {112, 20}, {118, 10}},    // Gemini
            {{120, 25}, {128, 18}, {135, 10}, {140, 20}},              // Cancer
            {{145, 12}, {152, 20}, {158, 8}, {165, 15}},               // Leo
            {{175, 5}, {182, 12}, {190, 2}, {196, 10}},                // Virgo
            {{210, -5}, {218, 8}, {225, -2}, {232, 6}},                // Libra
            {{240, -10}, {248, -2}, {255, -12}, {262, -4}},            // Scorpio
            {{270, -15}, {278, -5}, {285, -18}, {292, -8}},            // Sagittarius
            {{300, 10}, {308, 18}, {315, 6}, {322, 14}},               // Capricorn
            {{330, -8}, {338, 5}, {345, -15}, {352, 2}},               // Aquarius
            {{5, 5}, {12, 12}, {18, 0}, {25, 8}}                       // Pisces
        };
        zodiacNames = new String[12];
        System.arraycopy(Zodiac.SIGNS, 0, zodiacNames, 0, 12);
        zodiacLines = new double[defs.length][][];
        for (int s = 0; s < defs.length; s++) {
            zodiacLines[s] = new double[defs[s].length][3];
            for (int i = 0; i < defs[s].length; i++) {
                double lon = Math.toRadians(defs[s][i][0]);
                double lat = Math.toRadians(defs[s][i][1]);
                zodiacLines[s][i][0] = Math.cos(lat) * Math.cos(lon);
                zodiacLines[s][i][1] = Math.sin(lat);
                zodiacLines[s][i][2] = Math.cos(lat) * Math.sin(lon);
            }
        }
    }

    public void tick(double dt) {
        for (int i = 0; i < metLife.length; i++) {
            metLife[i] -= dt * 0.35;
            if (metLife[i] <= 0 && rnd.nextDouble() < 0.008) {
                // spawn new meteor
                double u = rnd.nextDouble() * 2 - 1;
                double th = rnd.nextDouble() * Math.PI * 2;
                double s = Math.sqrt(1 - u * u);
                metX[i] = (float) (s * Math.cos(th));
                metY[i] = (float) u;
                metZ[i] = (float) (s * Math.sin(th));
                metLife[i] = 1.0f;
            }
        }
    }
}
