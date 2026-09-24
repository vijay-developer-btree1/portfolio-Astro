package astro3d.core;

/**
 * Orbit camera. Full 360-degree azimuth / -89..+89 elevation,
 * keyboard + mouse driven, perspective projection.
 */
public final class Camera {
    public double px, py, pz;          // position
    public double yaw, pitch;          // radians
    public double dist = 60;           // orbit radius from target
    public double tx, ty, tz;          // target point
    public double focal = 900;         // pixel focal length
    public int w = 1280, h = 720;

    public double minDist = 4, maxDist = 260;
    public double minPitch = -1.53, maxPitch = 1.53;

    /** Camera basis matrix (world -> camera). */
    public double m00, m01, m02;
    public double m10, m11, m12;
    public double m20, m21, m22;

    public Camera() { update(); }

    public void setViewport(int w, int h) {
        this.w = w; this.h = h;
        focal = 0.9 * Math.max(w, h);
    }

    public void orbit(double dYaw, double dPitch) {
        yaw += dYaw;
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch + dPitch));
        update();
    }

    public void zoom(double factor) {
        dist = Math.max(minDist, Math.min(maxDist, dist * factor));
        update();
    }

    public void pan(double dx, double dy) {
        // pan in camera plane
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double s = dist * 0.0016;
        tx -= cy * dx * s;
        tz += sy * dx * s;
        ty += dy * s * 1.2;
        update();
    }

    /** Recompute position + rotation matrix from yaw/pitch/dist around target. */
    public final void update() {
        px = tx - dist * Math.cos(pitch) * Math.sin(yaw);
        py = ty + dist * Math.sin(pitch);
        pz = tz - dist * Math.cos(pitch) * Math.cos(yaw);

        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);

        // rows of world->camera rotation (camera looks +z in camera space)
        m00 = cy;          m01 = 0;    m02 = sy;
        m10 = -sp * sy;    m11 = cp;   m12 = sp * cy;
        m20 = -cp * sy;    m21 = -sp;  m22 = cp * cy;
    }

    public void lookAt(Vec3 target) { tx = target.x; ty = target.y; tz = target.z; update(); }
}
