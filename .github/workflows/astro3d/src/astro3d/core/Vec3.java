package astro3d.core;

/** Minimal 3D double-precision vector. */
public final class Vec3 {
    public double x, y, z;

    public Vec3() {}
    public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public Vec3(Vec3 o) { this.x = o.x; this.y = o.y; this.z = o.z; }

    public Vec3 set(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
    public Vec3 set(Vec3 o) { this.x = o.x; this.y = o.y; this.z = o.z; return this; }
    public Vec3 copy() { return new Vec3(this); }

    public Vec3 add(Vec3 o) { x += o.x; y += o.y; z += o.z; return this; }
    public Vec3 sub(Vec3 o) { x -= o.x; y -= o.y; z -= o.z; return this; }
    public Vec3 scale(double s) { x *= s; y *= s; z *= s; return this; }
    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }

    public double len() { return Math.sqrt(x * x + y * y + z * z); }
    public Vec3 normalize() {
        double l = len();
        if (l < 1e-12) { x = 0; y = 0; z = 0; return this; }
        return scale(1.0 / l);
    }

    /** Rotate around X axis. */
    public Vec3 rotX(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        double ny = y * c - z * s, nz = y * s + z * c;
        y = ny; z = nz; return this;
    }

    /** Rotate around Y axis. */
    public Vec3 rotY(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        double nx = x * c + z * s, nz = -x * s + z * c;
        x = nx; z = nz; return this;
    }

    /** Rotate around Z axis. */
    public Vec3 rotZ(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        double nx = x * c - y * s, ny = x * s + y * c;
        x = nx; y = ny; return this;
    }

    /** Project world point into camera space, then perspective-project.
     *  Returns true if visible (in front of camera, within clip). */
    public boolean project(Camera cam, Vec3 out) {
        // world -> camera space
        double dx = x - cam.px, dy = y - cam.py, dz = z - cam.pz;
        double cx = cam.m00 * dx + cam.m01 * dy + cam.m02 * dz;
        double cy = cam.m10 * dx + cam.m11 * dy + cam.m12 * dz;
        double cz = cam.m20 * dx + cam.m21 * dy + cam.m22 * dz;
        if (cz < 0.05) return false;                       // behind camera
        double inv = cam.focal / cz;
        out.x = cx * inv;
        out.y = cy * inv;
        out.z = cz;                                        // depth for sorting
        return true;
    }
}
