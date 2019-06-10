package Labs.Abstractions;

import com.sun.javafx.geom.Vec3d;

public abstract class ContainsTriangleAbstraction {
    protected Double rayInTriangle(Vec3d A, Vec3d B, Vec3d C, Vec3d point, Vec3d direction) {
        if(!pointsValidation(A, B, C)){
            return null;
        }

        Vec3d normal = new Vec3d();
        normal.cross(new Vec3d(B.x - A.x, B.y - A.y, B.z - A.z), new Vec3d(C.x - A.x, C.y - A.y, C.z - A.z));

        double D = -(normal.x * A.x) - (normal.y * A.y) - (normal.z * A.z);
        double k = (point.dot(normal) - D) / direction.dot(normal);
        if(k < 0)
        {
            return null;
        }

        Vec3d P = new Vec3d(k * direction.x + point.x, k * direction.y + point.y, k * direction.z + point.z);

        if(pointInTriangle(A, B, C, P))
        {
            return distance(P, point);
        }

        return null;
    }

    private double distance(Vec3d A, Vec3d B){
        Vec3d dif = new Vec3d(A);
        dif.sub(B);
        return dif.length();
    }

    private boolean pointsValidation(Vec3d A, Vec3d B, Vec3d C){
        Vec3d ab = new Vec3d(A);
        ab.sub(B);
        if(ab.length() < 1e-5){
            return false;
        }

        Vec3d ac = new Vec3d(A);
        ac.sub(C);
        if(ac.length() < 1e-5){
            return false;
        }

        Vec3d bc = new Vec3d(B);
        bc.sub(C);
        if(bc.length() < 1e-5){
            return false;
        }

        return true;
    }

    private boolean pointInTriangle(Vec3d A, Vec3d B, Vec3d C, Vec3d P){
        Vec3d v0 = new Vec3d(C.x - A.x, C.y - A.y, C.z - A.z);
        Vec3d v1 = new Vec3d(B.x - A.x, B.y - A.y, B.z - A.z);
        Vec3d v2 = new Vec3d(P.x - A.x, P.y - A.y, P.z - A.z);

        double dot00 = v0.x*v0.x + v0.y*v0.y;
        double dot01 = v0.x*v1.x + v0.y*v1.y;
        double dot02 = v0.x*v2.x + v0.y*v2.y;
        double dot11 = v1.x*v1.x + v1.y*v1.y;
        double dot12 = v1.x*v2.x + v1.y*v2.y;

        double invDenom = 1.0f / (dot00 * dot11 - dot01 * dot01);
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        if (u < 0) {
            return false;
        }
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (v > 0) && (u + v < 1.0f);
    }
}
