package com.aicompanion.bot.area;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class SavedArea {
    public String name = "";
    public String type = "COMPOUND"; // COMPOUND, FARM, RANCH, SAFE_ZONE, RESTRICTED, GENERAL
    public String dimension = "minecraft:overworld";
    public int minY = -64;
    public int maxY = 320;
    // Polygon vertices in horizontal plane: each element is [x, z]
    public List<int[]> points = new ArrayList<>();

    // Transient cached Axis-Aligned Bounding Box for fast preliminary rejection
    private transient int minX = Integer.MAX_VALUE;
    private transient int maxX = Integer.MIN_VALUE;
    private transient int minZ = Integer.MAX_VALUE;
    private transient int maxZ = Integer.MIN_VALUE;
    private transient boolean aabbComputed = false;

    public SavedArea() {}

    public SavedArea(String name, String type, String dimension, int minY, int maxY, List<int[]> points) {
        this.name = name;
        this.type = (type == null || type.isBlank()) ? "COMPOUND" : type.toUpperCase();
        this.dimension = dimension;
        this.minY = minY;
        this.maxY = maxY;
        if (points != null) {
            this.points.addAll(points);
        }
        computeAABB();
    }

    public synchronized void computeAABB() {
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minZ = Integer.MAX_VALUE;
        maxZ = Integer.MIN_VALUE;
        for (int[] pt : points) {
            if (pt.length >= 2) {
                if (pt[0] < minX) minX = pt[0];
                if (pt[0] > maxX) maxX = pt[0];
                if (pt[1] < minZ) minZ = pt[1];
                if (pt[1] > maxZ) maxZ = pt[1];
            }
        }
        aabbComputed = true;
    }

    public int getMinX() { if (!aabbComputed) computeAABB(); return minX; }
    public int getMaxX() { if (!aabbComputed) computeAABB(); return maxX; }
    public int getMinZ() { if (!aabbComputed) computeAABB(); return minZ; }
    public int getMaxZ() { if (!aabbComputed) computeAABB(); return maxZ; }

    /**
     * Determines whether a 3D coordinate is strictly inside this polygon boundary.
     * Uses fast AABB rejection followed by Ray-Casting (Even-Odd) algorithm.
     */
    public boolean contains(double x, double y, double z) {
        if (y < minY || y > maxY) return false;
        if (points.size() < 3) return false;
        if (!aabbComputed) computeAABB();

        // 1. Fast AABB Check
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }

        // 2. Jordan Curve Ray-Casting Algorithm in X-Z plane
        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0];
            double zi = points.get(i)[1];
            double xj = points.get(j)[0];
            double zj = points.get(j)[1];

            boolean intersect = ((zi > z) != (zj > z)) &&
                    (x < (xj - xi) * (z - zi) / (zj - zi + 1e-9) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public boolean contains(Vec3 vec) {
        return contains(vec.x, vec.y, vec.z);
    }

    /**
     * Returns the geometric center (centroid) of the polygon.
     */
    public Vec3 getCenter() {
        if (points.isEmpty()) {
            return new Vec3(0, (minY + maxY) / 2.0, 0);
        }
        double sumX = 0;
        double sumZ = 0;
        for (int[] pt : points) {
            sumX += pt[0];
            sumZ += pt[1];
        }
        double avgX = sumX / points.size();
        double avgZ = sumZ / points.size();
        double centerY = Math.max(minY, Math.min(maxY, (minY + maxY) / 2.0));
        return new Vec3(avgX + 0.5, centerY, avgZ + 0.5);
    }

    /**
     * Calculates the approximate 2D polygon area in square blocks using the Shoelace formula.
     */
    public double getAreaSqMeters() {
        if (points.size() < 3) return 0;
        double area = 0;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            area += ((double) points.get(j)[0] * points.get(i)[1]) - ((double) points.get(i)[0] * points.get(j)[1]);
        }
        return Math.abs(area / 2.0);
    }

    /**
     * Spawns particle lines along all edges of the polygon boundary to render a glowing perimeter fence.
     */
    public void spawnPerimeterParticles(ServerLevel level, ParticleOptions particle) {
        if (points.size() < 2) return;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            int[] p1 = points.get(i);
            int[] p2 = points.get((i + 1) % n);

            double x1 = p1[0] + 0.5;
            double z1 = p1[1] + 0.5;
            double x2 = p2[0] + 0.5;
            double z2 = p2[1] + 0.5;

            // Ground level particles
            double y1 = Math.max(level.getMinY(), Math.min(level.getMaxY(), p1.length > 2 ? p1[2] : (minY + 2)));
            double y2 = Math.max(level.getMinY(), Math.min(level.getMaxY(), p2.length > 2 ? p2[2] : (minY + 2)));

            // Corner marker pillar
            level.sendParticles(ParticleTypes.END_ROD, x1, y1 + 1.0, z1, 2, 0.1, 0.8, 0.1, 0.02);

            // Interpolated line
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double dist = Math.sqrt(dx * dx + dz * dz);
            int steps = Math.max(1, (int) (dist / 0.75));

            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double px = x1 + t * dx;
                double py = y1 + t * dy + 0.25;
                double pz = z1 + t * dz;
                level.sendParticles(particle, px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }
}
