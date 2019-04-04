package Labs.Entities;

import com.sun.javafx.geom.Vec3f;

public class Quad {
    public Vec3f A;
    public Vec3f B;
    public Vec3f C;
    public Vec3f D;

    public float Color;
    public float Square;
    public float kSquare;

    public int CollisionsCount;

    public Quad(Vec3f A, Vec3f B, Vec3f C, Vec3f D){
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;

        this.Color = 1.0f;
        this.CollisionsCount = 0;

        this.Square = 0.0f;
        this.kSquare = 0.0f;
    }
}
