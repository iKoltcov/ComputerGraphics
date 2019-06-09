package Labs;

import Labs.Entities.Quad;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec3f;

import java.util.ArrayList;
import java.util.Random;

public class Lab3 extends LabAbstract {
    private GL2 gl;
    private GLU glu;
    private int width, height;
    private final GLWindow glWindow;

    private Random random = new Random();
    private ArrayList<Thread> threads;

    private final float RADIUS = 1.0f;
    private final double stackStep = Math.PI * 0.01;
    private final double sectorStep = Math.PI * 0.01;

    private volatile int maxArrayCounter;
    private int totalPoint = 0;

    private float maxSquare;
    private ArrayList<Quad> quads;
    private Vec3f Camera;
    private Vec3f Target;
    private Vec3f LastEye, LastRay;

    public Lab3(int width, int height, GLWindow glWindow){
        this.width = width;
        this.height = height;
        this.glWindow = glWindow;

        Camera = new Vec3f(0.0f, 0.0f, 2.0f);
        Target = new Vec3f(0.0f, 0.0f, 0.0f);
        LastEye = new Vec3f(0.0f, 0.0f, 0.0f);
        LastRay = new Vec3f(0.0f, 0.0f, 0.0f);
        quads = new ArrayList<Quad>();
        maxArrayCounter = 0;

        for(int i = 0; i < 2.0 / stackStep; i++)
        {
            double h = -1.0 + i * (stackStep);

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
                Vec3f A = new Vec3f(
                        Target.x + (float)(Math.cos(th) * Math.cos(fi)),
                        Target.y + (float)(Math.cos(th) * Math.sin(fi)),
                        Target.z + (float)Math.sin(th));
                Vec3f B = new Vec3f(
                        Target.x + (float)(Math.cos(nextTh) * Math.cos(fi)),
                        Target.y + (float)(Math.cos(nextTh) * Math.sin(fi)),
                        Target.z + (float)Math.sin(nextTh));
                Vec3f C = new Vec3f(
                        Target.x + (float)(Math.cos(th) * Math.cos(fi + sectorStep)),
                        Target.y + (float)(Math.cos(th) * Math.sin(fi + sectorStep)),
                        Target.z + (float)Math.sin(th));
                Vec3f D = new Vec3f(
                        Target.x + (float)(Math.cos(nextTh) * Math.cos(fi + sectorStep)),
                        Target.y + (float)(Math.cos(nextTh) * Math.sin(fi + sectorStep)),
                        Target.z + (float)Math.sin(nextTh));

                quads.add(new Quad(A, B, C, D, i, j));
            }
        }

        maxSquare = 0.0f;
        for (Quad quad : quads) {
            quad.Square = distance(quad.A, quad.B) * distance(quad.A, quad.C) / 2.0f
                    + distance(quad.D, quad.B) * distance(quad.D, quad.C) / 2.0f;
            if(quad.Square > maxSquare){
                maxSquare = quad.Square;
            }
        }

        for (Quad quad : quads) {
            quad.kSquare = maxSquare / quad.Square;
        }
    }

    @Override
    public void clear() {
        for (Quad quad : quads) {
            quad.CollisionsCount = 0;
            quad.Color = 1.0f;
        }
        maxArrayCounter = 0;
        totalPoint = 0;

        LastEye = new Vec3f();
        LastRay = new Vec3f();
    }

    private float cameraPosition = 0.0f;
    private void FrameLogic(){
        if(!isStop){
            cameraPosition = cameraPosition + 0.01f % ((float)Math.PI * 2.0f);
        }

        Camera.x = (Target.x + 2.5f) * (float)Math.cos(cameraPosition);
        Camera.y = (Target.y + 2.5f) * (float)Math.sin(cameraPosition);
        Camera.z = Target.z + (float)Math.sin(cameraPosition) * 2.0f;
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
        float h = (2.0f * random.nextFloat() - 1.0f) * RADIUS;
        float angleTh = (float)Math.asin(h / RADIUS);
        float angleFi = random.nextFloat() * ((float)Math.PI * 2.0f);

        Vec3f point = new Vec3f(Target.x, Target.y, Target.z);
        Vec3f direction = new Vec3f(
                point.x + (float)Math.cos(angleTh) * (float)Math.cos(angleFi) * RADIUS,
                point.y + (float)Math.cos(angleTh) * (float)Math.sin(angleFi) * RADIUS,
                point.z + (float)Math.sin(angleTh) * RADIUS);
        direction.normalize();
        totalPoint++;

        Quad refQuad = new Quad(null, null, null, null, 0, 0);
        Float minDistance = null;
        for (Quad quad : quads) {
            Float firstTriangle = rayInTriangle(quad.A, quad.B, quad.D, point, direction);
            Float secondTriangle = rayInTriangle(quad.A, quad.C, quad.D, point, direction);
            if( firstTriangle != null || secondTriangle != null){
                float distance = firstTriangle == null ? secondTriangle : firstTriangle;

                if(minDistance == null || minDistance > distance){
                    minDistance = distance;
                    refQuad = quad;
                }
            }
        }

        if(maxArrayCounter == refQuad.CollisionsCount++){
            maxArrayCounter = refQuad.CollisionsCount;
            glWindow.setTitle(String.valueOf(maxArrayCounter) + "; " + String.valueOf(totalPoint));
        }

        for (Quad quad : quads) {
            quad.Color = (quad.CollisionsCount / (float)maxArrayCounter * quad.kSquare) * 0.5f + 0.5f;
        }

        LastEye.set(point);
        LastRay.set(point);
        LastRay.add(direction);
        LastRay.mul(10.0f);
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
        glu.gluPerspective(60.0f, width/ height, 0.1f, 1000.0f);
        glu.gluLookAt(Camera.x, Camera.y, Camera.z, Target.x, Target.y, Target.z, 0.0f, 0.0f, 1.0f);

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glBegin(gl.GL_QUADS);
        for (Quad quad : quads) {
            gl.glColor3f(quad.Color, quad.Color, quad.Color);
            gl.glVertex3f(quad.A.x, quad.A.y, quad.A.z);
            gl.glVertex3f(quad.B.x, quad.B.y, quad.B.z);
            gl.glVertex3f(quad.D.x, quad.D.y, quad.D.z);
            gl.glVertex3f(quad.C.x, quad.C.y, quad.C.z);
        }
        gl.glEnd();

        gl.glBegin(gl.GL_LINES);
            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(LastEye.x, LastEye.y, LastEye.z);
            gl.glVertex3f(LastRay.x, LastRay.y, LastRay.z);
        gl.glEnd();

//        gl.glColor3f(0.0f, 0.0f, 0.0f);
//        float k = 1.001f;
//        for (Quad quad : quads) {
//            gl.glBegin(gl.GL_LINE_LOOP);
//                gl.glVertex3f(quad.A.x * k, quad.A.y * k, quad.A.z * k);
//                gl.glVertex3f(quad.B.x * k, quad.B.y * k, quad.B.z * k);
//                gl.glVertex3f(quad.D.x * k, quad.D.y * k, quad.D.z * k);
//                gl.glVertex3f(quad.C.x * k, quad.C.y * k, quad.C.z * k);
//            gl.glEnd();
//        }

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        float sizeCells = 2.0f;
        gl.glBegin(gl.GL_QUADS);
        for (Quad quad : quads) {
            gl.glColor3f(quad.Color, quad.Color, quad.Color);
            gl.glVertex2f(quad.y * sizeCells, quad.x * sizeCells);
            gl.glVertex2f(quad.y * sizeCells + sizeCells, quad.x * sizeCells);
            gl.glVertex2f(quad.y * sizeCells + sizeCells, quad.x * sizeCells + sizeCells);
            gl.glVertex2f(quad.y * sizeCells, quad.x * sizeCells + sizeCells);
        }
        gl.glEnd();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
