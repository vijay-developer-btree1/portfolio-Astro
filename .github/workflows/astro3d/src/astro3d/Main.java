package astro3d;

import astro3d.core.Camera;
import astro3d.model.Body;
import astro3d.model.SolarSystem;
import astro3d.scene.Renderer;
import astro3d.scene.Starfield;
import astro3d.ui.Focus;
import astro3d.ui.Hud;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ✧ ASTRO 3D — Next-generation astrology space experience.
 * Pure Java, zero dependencies. 360° free-look 3D solar system,
 * zodiac constellations, live geocentric chart, milky way, nebulae,
 * comet, asteroid belt, meteors.
 *
 * Run:  java -cp bin astro3d.Main
 */
public final class Main extends JPanel implements ActionListener, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
    final SolarSystem ss = new SolarSystem();
    final Starfield sky = new Starfield();
    final Renderer renderer = new Renderer(ss, sky);
    final Camera cam = new Camera();
    final Hud hud = new Hud(ss);

    final Timer timer = new Timer(16, this);   // ~60 FPS
    long lastNanos = System.nanoTime();
    double fps = 60;

    // input state
    int lastMX, lastMY;
    boolean dragging;
    boolean paused;
    boolean showOrbits = true, showBelt = true, showConst = true;
    double timeScale = 3.0;                    // sim days per second
    Body follow;

    // FPS smoothing
    double fpsAcc;
    int fpsFrames;
    long fpsT0 = System.nanoTime();

    // screenshot flash
    boolean flash;

    public Main() {
        setBackground(new Color(3, 2, 12));
        setPreferredSize(new Dimension(1280, 720));
        cam.setViewport(1280, 720);
        cam.dist = 70;
        cam.pitch = 0.5;
        cam.update();

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
        addKeyListener(this);

        timer.start();
    }

    // ---------- game loop ----------

    @Override public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1e9;
        lastNanos = now;
        fps = fps * 0.95 + (1.0 / Math.max(dt, 1e-3)) * 0.05;

        if (!paused) {
            double dd = dt * timeScale;
            ss.update(dd);
        }
        sky.tick(dt);

        if (follow != null) {
            cam.tx = follow.x; cam.ty = follow.y; cam.tz = follow.z;
        }
        cam.update();
        repaint();
    }

    // ---------- painting ----------

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        cam.setViewport(getWidth(), getHeight());

        // deep space gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(4, 2, 16),
                0, getHeight(), new Color(10, 4, 28));
        g.setPaint(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        renderer.render(g, cam, lastNanos / 1e9, showOrbits, showBelt, showConst);

        hud.render(g, getWidth(), getHeight(), ss.simDays, fps);

        if (flash) {
            g.setColor(new Color(255, 255, 255, 90));
            g.fillRect(0, 0, getWidth(), getHeight());
            flash = false;
        }
    }

    // ---------- mouse ----------

    @Override public void mousePressed(MouseEvent e) {
        lastMX = e.getX(); lastMY = e.getY();
        dragging = true;
        requestFocusInWindow();
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (!dragging) return;
        int dx = e.getX() - lastMX, dy = e.getY() - lastMY;
        lastMX = e.getX(); lastMY = e.getY();
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
            cam.pan(-dx, dy);
        } else {
            cam.orbit(dx * 0.0055, dy * 0.0045);   // full 360° yaw
        }
    }

    @Override public void mouseReleased(MouseEvent e) { dragging = false; }
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        cam.zoom(e.getWheelRotation() > 0 ? 1.12 : 0.89);
    }
    @Override public void mouseClicked(MouseEvent e) {}

    // ---------- keys ----------

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE -> paused = !paused;
            case KeyEvent.VK_O -> showOrbits = !showOrbits;
            case KeyEvent.VK_B -> showBelt = !showBelt;
            case KeyEvent.VK_C -> showConst = !showConst;
            case KeyEvent.VK_R -> resetView();
            case KeyEvent.VK_F -> follow = Focus.get();
            case KeyEvent.VK_ESCAPE -> { follow = null; Focus.clear(); resetView(); }
            case KeyEvent.VK_P -> screenshot();
            case KeyEvent.VK_LEFT -> cam.orbit(-0.12, 0);
            case KeyEvent.VK_RIGHT -> cam.orbit(0.12, 0);
            case KeyEvent.VK_UP -> cam.orbit(0, 0.08);
            case KeyEvent.VK_DOWN -> cam.orbit(0, -0.08);
            case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS, KeyEvent.VK_ADD -> timeScale = Math.min(200, timeScale * 1.5);
            case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> timeScale = Math.max(0.05, timeScale / 1.5);
            default -> {
                // 1..9,0 focus planets
                int idx = -1;
                if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9)
                    idx = e.getKeyCode() - KeyEvent.VK_1;
                else if (e.getKeyCode() == KeyEvent.VK_0) idx = 9;
                if (idx >= 0) {
                    var planets = ss.bodies.stream().filter(b -> !b.isSun).toList();
                    if (idx < planets.size()) {
                        Body b = planets.get(idx);
                        Focus.set(b);
                        follow = b;
                    }
                }
            }
        }
    }

    void resetView() {
        cam.tx = cam.ty = cam.tz = 0;
        cam.dist = 70;
        cam.pitch = 0.5;
        cam.yaw = 0.6;
        cam.update();
    }

    void screenshot() {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        paint(img.getGraphics());
        String name = "astro3d-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png";
        try {
            ImageIO.write(img, "png", new File(name));
            flash = true;
            System.out.println("Saved screenshot: " + new File(name).getAbsolutePath());
        } catch (Exception ex) {
            System.out.println("Screenshot failed: " + ex);
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // ---------- entry ----------

    public static void main(String[] args) {
        if (args.length >= 2 && args[0].equals("--screenshot")) {
            renderScreenshot(args[1], args.length > 2 ? Integer.parseInt(args[2]) : 1600);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("✧ ASTRO 3D — 360° Celestial Observatory");
            Main panel = new Main();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    /** Renders a still frame off-screen: --screenshot <file.png> [width]. */
    static void renderScreenshot(String file, int width) {
        Main m = new Main();
        int height = width * 9 / 16;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        m.setSize(width, height);
        m.cam.setViewport(width, height);
        m.cam.update();
        m.ss.update(0);
        m.paint(img.getGraphics());
        try {
            ImageIO.write(img, "png", new File(file));
            System.out.println("Saved: " + new File(file).getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Screenshot failed: " + ex);
            System.exit(1);
        }
        System.exit(0);
    }
}
