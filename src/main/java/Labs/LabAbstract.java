package Labs;

import com.jogamp.opengl.GLEventListener;
import com.sun.javafx.geom.Vec3f;

public abstract class LabAbstract implements GLEventListener {
    boolean isStop = false;

    public void toggleStop(){
        isStop = !isStop;
    };

    float distance(Vec3f A, Vec3f B){
        return (float)Math.sqrt( ((A.x - B.x) * (A.x - B.x)) + ((A.y - B.y) * (A.y - B.y)) + ((A.z - B.z) * (A.z - B.z)) );
    }

    Float rayInTriangle(Vec3f A, Vec3f B, Vec3f C, Vec3f point, Vec3f direction) {
        try {
            Vec3f ray = new Vec3f(direction.x - point.x, direction.y - point.y, direction.z - point.z);
            Vec3f rayToTriangle = new Vec3f(A.x - point.x, A.y - point.y, A.z - point.z);
            if (ray.dot(rayToTriangle) < 0.0f)
                return null;

            Vec3f BA = new Vec3f(B.x - A.x, B.y - A.y, B.z - A.z);
            Vec3f CA = new Vec3f(C.x - A.x, C.y - A.y, C.z - A.z);

            Vec3f N = new Vec3f();
            N.cross(BA, CA);

            float D = -(N.x * A.x) - (N.y * A.y) - (N.z * A.z);
            float k = -(N.x * point.x + N.y * point.y + N.z * point.z + D) / (N.x * direction.x + N.y * direction.y + N.z * direction.z);
            Vec3f P = new Vec3f(k * direction.x + point.x, k * direction.y + point.y, k * direction.z + point.z);

            if(pointInTriangle(A, B, C, P))
            {
                return distance(P, point);
            }
        }
        catch(Exception exception){
            return null;
        }

        return null;
    }

    private boolean pointInTriangle(Vec3f A, Vec3f B, Vec3f C, Vec3f P){
        Vec3f v0 = new Vec3f(C.x - A.x, C.y - A.y, C.z - A.z);
        Vec3f v1 = new Vec3f(B.x - A.x, B.y - A.y, B.z - A.z);
        Vec3f v2 = new Vec3f(P.x - A.x, P.y - A.y, P.z - A.z);

        float dot00 = v0.x*v0.x + v0.y*v0.y;
        float dot01 = v0.x*v1.x + v0.y*v1.y;
        float dot02 = v0.x*v2.x + v0.y*v2.y;
        float dot11 = v1.x*v1.x + v1.y*v1.y;
        float dot12 = v1.x*v2.x + v1.y*v2.y;

        float invDenom = 1.0f / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        if (u < 0) {
            return false;
        }
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (v > 0) && (u + v < 1.0f);
    }


    public abstract void addPoint();

    public abstract void clear();
}
