package io.th0rgal.oraxen.utils;

import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class VectorUtils {

    private VectorUtils() {
    }

    public static void rotateAroundAxisX(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        v.setY(y).setZ(z);
    }

    public static void rotateAroundAxisY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        v.setX(x).setZ(z);
    }

    public static Vector3f parseVector3f(String input) {
        if (input == null) {
            return null;
        }

        final String[] parts = input.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Vector3f(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
