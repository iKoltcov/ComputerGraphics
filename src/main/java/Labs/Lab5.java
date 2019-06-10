package Labs;

import Abstractions.LabAbstraction;
import Entities.Quad;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec2d;
import com.sun.javafx.geom.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class Lab5 extends LabAbstraction {
    private GL2 gl;
    private GLU glu;
    private int width, height;
    private final GLWindow glWindow;

    private Random random = new Random();
    private ArrayList<Thread> threads;

    private final double radius = 1.0;
    private final double stackStep = Math.PI * 0.005;
    private final double sectorStep = Math.PI * 0.01;

    private final double tableMax = 1.0;
    private final double tableStep = Math.PI * 0.005;
    private Vec2d[] table;

    private volatile int maxArrayCounter;
    private int totalPoint = 0;

    private double maxSquare;
    private ArrayList<Quad> quads;
    private Vec3d Camera;
    private Vec3d Target;
    private Vec3d LastEye, LastRay;

    public Lab5(int width, int height, GLWindow glWindow){
        this.width = width;
        this.height = height;
        this.glWindow = glWindow;

        Camera = new Vec3d(0.0, 0.0, 2.0);
        Target = new Vec3d(0.0, 0.0, 0.0);
        LastEye = new Vec3d(0.0, 0.0, 0.0);
        LastRay = new Vec3d(0.0, 0.0, 0.0);
        quads = new ArrayList<Quad>();
        maxArrayCounter = 0;

        for(int i = 0; i < 1.0 / stackStep; i++)
        {
            double h = i * (stackStep);

            double th = Math.asin(h);
            if(Double.isNaN(th)){
                continue;
            }

            double nextTh = Math.asin(h + stackStep);
            if(Double.isNaN(nextTh)){
                nextTh = Math.asin(1.0);
            }

            for(int j = 0; j < (2.0 * Math.PI) / sectorStep; j++){
                double fi = sectorStep * j;
                Vec3d A = new Vec3d(
                        Target.x + Math.cos(th) * Math.cos(fi),
                        Target.y + Math.cos(th) * Math.sin(fi),
                        Target.z + Math.sin(th));
                Vec3d B = new Vec3d(
                        Target.x + Math.cos(nextTh) * Math.cos(fi),
                        Target.y + Math.cos(nextTh) * Math.sin(fi),
                        Target.z + Math.sin(nextTh));
                Vec3d C = new Vec3d(
                        Target.x + Math.cos(th) * Math.cos(fi + sectorStep),
                        Target.y + Math.cos(th) * Math.sin(fi + sectorStep),
                        Target.z + Math.sin(th));
                Vec3d D = new Vec3d(
                        Target.x + Math.cos(nextTh) * Math.cos(fi + sectorStep),
                        Target.y + Math.cos(nextTh) * Math.sin(fi + sectorStep),
                        Target.z + Math.sin(nextTh));

                quads.add(new Quad(A, B, C, D, i, j));
            }
        }

        maxSquare = 0.0;
        for (Quad quad : quads) {
            quad.Square = distance(quad.A, quad.B) * distance(quad.A, quad.C) / 2.0
                        + distance(quad.D, quad.B) * distance(quad.D, quad.C) / 2.0;
            if(quad.Square > maxSquare){
                maxSquare = quad.Square;
            }
        }

        table = new Vec2d[(int)Math.round(tableMax / tableStep) + 1];
        for(int iterator = 0; iterator < table.length; iterator++){
            double currentValue = iterator * tableStep;
            double previousValue = iterator > 0 ? table[iterator - 1].y : 0.0;
            table[iterator] = new Vec2d(currentValue, (previousValue + function(currentValue, tableStep)));
        }
        table[table.length - 1] = new Vec2d(tableMax, table[table.length - 2].y + function(tableMax, tableStep));

        double maxValue = Double.MIN_VALUE;
        for (Vec2d row : table) {
            if(row.y > maxValue)
            {
                maxValue = row.y;
            }
        }

        for (Vec2d row : table) {
            row.y = (row.y / maxValue);
        }

        for (Quad quad : quads) {
            quad.kSquare = 1.0f;//maxSquare * 2.0 / quad.Square;
        }
    }

    private double function(double x, double delta){
        return Math.cos(x) * delta;
    }

    public void clear(){
        for (Quad quad : quads) {
            quad.CollisionsCount = 0;
            quad.Color = 1.0f;
        }
        maxArrayCounter = 0;
        totalPoint = 0;

        LastEye = new Vec3d();
        LastRay = new Vec3d();
    }

    private double cameraPosition = -Math.PI * 0.5;
    private void FrameLogic(){
        if(!isStop){
            cameraPosition += 0.01 % ((float)Math.PI * 2.0);
        }
        Camera.x = (Target.x + 2.5) * (float)Math.cos(cameraPosition);
        Camera.y = (Target.y + 2.5) * (float)Math.sin(cameraPosition);
        Camera.z = Target.z + Math.sin(1.0) * 2.01;
    }

    public void setGl(GLAutoDrawable drawable){
        if(gl == null){
            gl = drawable.getGL().getGL2();
        }

        if(glu == null){
            glu = GLU.createGLU(gl);
        }
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        setGl(drawable);
        gl.glViewport(0, 0, width, height);
        gl.glClearColor(0.2f, 0.2f, 0.3f, 1.0f);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDepthFunc(gl.GL_LESS);

        threads = new ArrayList<Thread>();
        for(int i = 0; i < 4; i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        if(isStop) {
                            continue;
                        }

                        addPoint();
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
    }

    private double interpolation(double x, Vec2d bigger, Vec2d smaller){
        return smaller.y + (bigger.y - smaller.y) / (bigger.x - smaller.x) * (x - smaller.x);
    }

    public synchronized void addPoint(){
        int minimalNumber = 0;
        double minimalDifferent = Double.MAX_VALUE;
        double randomValue = random.nextDouble();
        boolean isLarger = true;

        for(int iterator = 0; iterator < table.length; iterator++){
            double different = randomValue - table[iterator].x;
            double absDifferent = Math.abs(different);

            if(absDifferent < minimalDifferent){
                minimalDifferent = absDifferent;
                minimalNumber = iterator;
                isLarger = different > 0.0;
            }
        }

        Double value;
        if(isLarger){
            if(minimalNumber + 1 < table.length)
                value = interpolation(randomValue, table[minimalNumber], table[minimalNumber + 1]);
            else
                value = interpolation(randomValue, table[minimalNumber - 1], table[minimalNumber]);
        }
        else
        {
            if(minimalNumber > 0)
                value = interpolation(randomValue, table[minimalNumber - 1], table[minimalNumber]);
            else
                value = interpolation(randomValue, table[minimalNumber], table[minimalNumber + 1]);
        }

        float angleTh = (float)(value * Math.PI * 0.5);
        float angleFi = (float)(random.nextDouble() * Math.PI * 2.0);
        Vec3d origin = new Vec3d(0.0, 0.0, 0.0);
        Vec3d direction = new Vec3d(
                Math.cos(angleTh) * Math.cos(angleFi),
                Math.cos(angleTh) * Math.sin(angleFi),
                Math.sin(angleTh));
        direction.normalize();
        totalPoint++;

        Quad refQuad = new Quad(null, null, null, null, 0, 0);
        Double minDistance = null;
        for (Quad quad : quads) {
            Double collisionDistance = quad.DistanceToCollision(origin, direction);

            if( collisionDistance != null){
                if(minDistance == null || minDistance > collisionDistance){
                    minDistance = collisionDistance;
                    refQuad = quad;
                }
            }
        }

        if(maxArrayCounter == refQuad.CollisionsCount++){
            maxArrayCounter = refQuad.CollisionsCount;
            glWindow.setTitle(String.valueOf(maxArrayCounter) + "; " + String.valueOf(totalPoint));
        }

        for (Quad quad : quads) {
            quad.Color = (quad.CollisionsCount / (float)maxArrayCounter * quad.kSquare) * 0.5 + 0.5;
        }

        LastEye.set(origin);
        LastRay.set(origin);
        LastRay.add(direction);
        LastRay.mul(10.0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        for (Thread thread : threads) {
            thread.interrupt();
        };
        drawable.destroy();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        FrameLogic();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(60.0f, width / height, 0.1f, 1000.0f);
        glu.gluLookAt(Camera.x, Camera.y, Camera.z, Target.x, Target.y, Target.z, 0.0f, 0.0f, 1.0f);

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glBegin(gl.GL_QUADS);
        for (Quad quad : quads) {
            gl.glColor3d(quad.Color, quad.Color, quad.Color);
            gl.glVertex3d(quad.A.x, quad.A.y, quad.A.z);
            gl.glVertex3d(quad.B.x, quad.B.y, quad.B.z);
            gl.glVertex3d(quad.D.x, quad.D.y, quad.D.z);
            gl.glVertex3d(quad.C.x, quad.C.y, quad.C.z);
        }
        gl.glEnd();

        gl.glBegin(gl.GL_LINES);
            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3d(LastEye.x, LastEye.y, LastEye.z);
            gl.glVertex3d(LastRay.x, LastRay.y, LastRay.z);
        gl.glEnd();

//        gl.glColor3d(0.0f, 0.0f, 0.0f);
//        double k = 1.001;
//        for (Quad quad : quads) {
//            gl.glBegin(gl.GL_LINE_LOOP);
//                gl.glVertex3d(quad.A.sectorX * k, quad.A.sectorY * k, quad.A.z * k);
//                gl.glVertex3d(quad.B.sectorX * k, quad.B.sectorY * k, quad.B.z * k);
//                gl.glVertex3d(quad.D.sectorX * k, quad.D.sectorY * k, quad.D.z * k);
//                gl.glVertex3d(quad.C.sectorX * k, quad.C.sectorY * k, quad.C.z * k);
//            gl.glEnd();
//        }

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        double sizeCells = 2.0;
        gl.glBegin(gl.GL_QUADS);
        for (Quad quad : quads) {
            gl.glColor3d(quad.Color, quad.Color, quad.Color);
            gl.glVertex2d(quad.sectorX * sizeCells, quad.sectorY * sizeCells);
            gl.glVertex2d(quad.sectorX * sizeCells + sizeCells, quad.sectorY * sizeCells);
            gl.glVertex2d(quad.sectorX * sizeCells + sizeCells, quad.sectorY * sizeCells + sizeCells);
            gl.glVertex2d(quad.sectorX * sizeCells, quad.sectorY * sizeCells + sizeCells);
        }
        gl.glEnd();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
