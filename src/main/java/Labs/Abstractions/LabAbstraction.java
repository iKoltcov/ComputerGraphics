package Labs.Abstractions;

import com.jogamp.opengl.GLEventListener;
import com.sun.javafx.geom.Vec3d;

public abstract class LabAbstraction implements GLEventListener {
    protected boolean isStop = false;

    public void toggleStop(){
        isStop = !isStop;
    };

    public abstract void addPoint();

    public abstract void clear();

    protected double distance(Vec3d A, Vec3d B){
        Vec3d dif = new Vec3d(A);
        dif.sub(B);
        return dif.length();
    }
}
