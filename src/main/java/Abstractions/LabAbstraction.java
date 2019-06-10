package Abstractions;

import com.jogamp.opengl.GLEventListener;
import com.sun.javafx.geom.Vec3d;

public abstract class LabAbstraction implements GLEventListener {
    protected boolean isStop = false;

    public void toggleStop(){
        isStop = !isStop;
    };

    public abstract void addPoint();

    public abstract void clear();
}
