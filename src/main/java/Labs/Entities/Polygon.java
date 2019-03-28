package Labs.Entities;

import com.sun.javafx.geom.Vec3f;

public class Polygon {
    public Vec3f A;
    public Vec3f B;
    public Vec3f C;

    public float Color;
    public float Square;
    public float kSquare;

    public int CollisionsCount;

    public Polygon(Vec3f A, Vec3f B, Vec3f C){
        this.A = A;
        this.B = B;
        this.C = C;

        this.Color = 1.0f;
        this.CollisionsCount = 0;

        this.Square = 0.0f;
        this.kSquare = 0.0f;
    }
}
