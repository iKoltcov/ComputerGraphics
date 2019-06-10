package Helpers;

import com.sun.javafx.geom.Vec3d;

public class MathHelper {
    public static double distance(Vec3d A, Vec3d B){
        Vec3d dif = new Vec3d(A);
        dif.sub(B);
        return dif.length();
    }
}
