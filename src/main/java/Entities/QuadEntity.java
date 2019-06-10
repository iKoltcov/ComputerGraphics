package Entities;

import Abstractions.ContainsTriangleAbstraction;
import Helpers.MathHelper;
import Interfaces.ICollided;
import com.sun.javafx.geom.Vec3d;

public class QuadEntity extends ContainsTriangleAbstraction implements ICollided {
    public Vec3d A;
    public Vec3d B;
    public Vec3d C;
    public Vec3d D;

    public double Color;
    public double Square;
    public double kSquare;

    public int CollisionsCount;

    public int sectorX;
    public int sectorY;

    public QuadEntity(Vec3d A, Vec3d B, Vec3d C, Vec3d D){
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;

        this.Color = 1.0f;
        this.CollisionsCount = 0;

        this.Square = 0.0f;
        this.kSquare = 0.0f;
    }

    public QuadEntity(Vec3d A, Vec3d B, Vec3d C, Vec3d D, int sectorX, int sectorY){
        this(A, B, C, D);
        this.sectorX = sectorX;
        this.sectorY = sectorY;
    }

    @Override
    public Double DistanceToCollision(Vec3d origin, Vec3d direction) {
        Double firstTriangle = rayInTriangle(A, B, C, origin, direction);
        Double secondTriangle = rayInTriangle(B, C, D, origin, direction);

        return firstTriangle == null ? secondTriangle : firstTriangle;
    }
}
