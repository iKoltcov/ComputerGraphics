package Interfaces;

import com.sun.javafx.geom.Vec3d;

public interface ICollided {
    Double DistanceToCollision(Vec3d origin, Vec3d direction);
}
