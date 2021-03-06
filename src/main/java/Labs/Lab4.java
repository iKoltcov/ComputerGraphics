package Labs;

import Abstractions.LabAbstraction;
import Entities.QuadEntity;
import Helpers.MathHelper;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class Lab4 extends LabAbstraction {
    private GL2 gl;
    private GLU glu;
    private int width, height;
    private final GLWindow glWindow;

    private Random random = new Random();
    private ArrayList<Thread> threads;

    private final double radius = 1.0f;
    private final double stackStep = Math.PI * 0.005;
    private final double sectorStep = Math.PI * 0.01;

    private double maxSquare;
    private volatile int maxArrayCounter;
    private int totalPoint = 0;

    private ArrayList<QuadEntity> quads;
    private Vec3d Camera;
    private Vec3d Target;
    private Vec3d LastEye, LastRay;

    public Lab4(int width, int height, GLWindow glWindow){
        this.width = width;
        this.height = height;
        this.glWindow = glWindow;

        Camera = new Vec3d(0.0, 0.0, 2.0);
        Target = new Vec3d(0.0, 0.0, 0.0);
        LastEye = new Vec3d(0.0, 0.0, 0.0);
        LastRay = new Vec3d(0.0, 0.0, 0.0);
        quads = new ArrayList<QuadEntity>();
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

                quads.add(new QuadEntity(A, B, C, D, i, j));
            }
        }

        maxSquare = 0.0;
        for (QuadEntity quad : quads) {
            quad.Square = MathHelper.distance(quad.A, quad.B) * MathHelper.distance(quad.A, quad.C) / 2.0
                        + MathHelper.distance(quad.D, quad.B) * MathHelper.distance(quad.D, quad.C) / 2.0;
            if(quad.Square > maxSquare){
                maxSquare = quad.Square;
            }
        }

        for (QuadEntity quad : quads) {
            quad.kSquare = maxSquare / quad.Square;
        }
    }

    public void clear(){
        for (QuadEntity quad : quads) {
            quad.CollisionsCount = 0;
            quad.Color = 1.0;
        }
        maxArrayCounter = 0;
        totalPoint = 0;

        LastEye = new Vec3d();
        LastRay = new Vec3d();
    }

    private double cameraPosition = -1.57;
    private void FrameLogic(){
        if(!isStop){
            cameraPosition += 0.01 % (Math.PI * 2.0);
        }

        Camera.x = (Target.x + 2.5) * Math.cos(cameraPosition);
        Camera.y = (Target.y + 2.5) * Math.sin(cameraPosition);
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

    public synchronized void addPoint(){
        double h = (2.0 * random.nextDouble() - 1.0) * radius;
        double angleTh = Math.asin(h / radius);
        double angleFi = random.nextDouble() * Math.PI * 2.0;

        Vec3d origin = new Vec3d(Target.x, Target.y, Target.z);
        Vec3d direction = new Vec3d(
                origin.x + Math.cos(angleTh) * Math.cos(angleFi) * radius,
                origin.y + Math.cos(angleTh) * Math.sin(angleFi) * radius,
                origin.z + Math.sin(angleTh) * radius);
        direction.add(new Vec3d(0.0, 0.0, 1.0));
        direction.normalize();

        totalPoint++;
        QuadEntity refQuad = null;
        Double minDistance = null;
        for (QuadEntity quad : quads) {
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

        for (QuadEntity quad : quads) {
            if(maxArrayCounter != 0) {
                quad.Color = (quad.CollisionsCount / (float) maxArrayCounter * quad.kSquare) * 0.5 + 0.5;
            }
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
        for (QuadEntity quad : quads) {
            gl.glColor3d(quad.Color, quad.Color, quad.Color);
            gl.glVertex3d(quad.A.x, quad.A.y, quad.A.z);
            gl.glVertex3d(quad.B.x, quad.B.y, quad.B.z);
            gl.glVertex3d(quad.D.x, quad.D.y, quad.D.z);
            gl.glVertex3d(quad.C.x, quad.C.y, quad.C.z);
        }
        gl.glEnd();

        gl.glBegin(gl.GL_LINES);
            gl.glColor3d(0.0f, 1.0f, 0.0f);
            gl.glVertex3d(LastEye.x, LastEye.y, LastEye.z);
            gl.glVertex3d(LastRay.x, LastRay.y, LastRay.z);
        gl.glEnd();

//        gl.glColor3d(0.0f, 0.0f, 0.0f);
//        double k = 1.001d;
//        for (QuadEntity quad : quads) {
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

        double sizeCells = 2.0f;
        gl.glBegin(gl.GL_QUADS);
        for (QuadEntity quad : quads) {
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
