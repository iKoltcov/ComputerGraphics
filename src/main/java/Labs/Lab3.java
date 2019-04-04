package Labs;

import Labs.Entities.Quad;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec3f;

import java.util.ArrayList;
import java.util.Random;

public class Lab3 implements GLEventListener {
    private GL2 gl;
    private GLU glu;
    private int width, height;
    private final GLWindow glWindow;

    private Random random = new Random();
    private ArrayList<Thread> threads;

    private final float RADIUS = 1.0f;
    private final float STEP = (float)Math.PI * 0.01f;

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
        quads = new ArrayList<Quad>();
        maxArrayCounter = 0;

        int i = 0;
        for(float th = 0.0f; th < Math.PI; th += STEP, i++)
        {
            int j = 0;
            for (float fi = 0.0f; fi < 2.0f * Math.PI; fi += STEP, j++) {
                Vec3f A = new Vec3f(
                        Target.x + RADIUS * (float)(Math.sin(th) * Math.cos(fi)),
                        Target.y + RADIUS * (float)(Math.sin(th) * Math.sin(fi)),
                        Target.z + RADIUS * (float)Math.cos(th));
                Vec3f B = new Vec3f(
                        Target.x + RADIUS * (float)(Math.sin(th + STEP) * Math.cos(fi)),
                        Target.y + RADIUS * (float)(Math.sin(th + STEP) * Math.sin(fi)),
                        Target.z + RADIUS * (float)Math.cos(th + STEP));
                Vec3f C = new Vec3f(
                        Target.x + RADIUS * (float)(Math.sin(th) * Math.cos(fi + STEP)),
                        Target.y + RADIUS * (float)(Math.sin(th) * Math.sin(fi + STEP)),
                        Target.z + RADIUS * (float)Math.cos(th));
                Vec3f D = new Vec3f(
                        Target.x + RADIUS * (float)(Math.sin(th + STEP) * Math.cos(fi + STEP)),
                        Target.y + RADIUS * (float)(Math.sin(th + STEP) * Math.sin(fi + STEP)),
                        Target.z + RADIUS * (float)Math.cos(th + STEP));

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
            quad.kSquare = quad.Square / maxSquare;
        }
    }

    private static float distance(Vec3f A, Vec3f B){
        return (float)Math.sqrt( ((A.x - B.x) * (A.x - B.x)) + ((A.y - B.y) * (A.y - B.y)) + ((A.z - B.z) * (A.z - B.z)) );
    }

    private float cameraPosition = 0.0f;
    private void FrameLogic(){
        cameraPosition = cameraPosition + 0.01f % ((float)Math.PI * 2.0f);
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
                        addPoint();
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
    }

    private synchronized void addPoint(){
        float angleTh = random.nextFloat() * ((float)Math.PI);
        float angleFi = random.nextFloat() * ((float)Math.PI * 2.0f);
        Vec3f eye = new Vec3f(Target.x, Target.y, Target.z);
        Vec3f ray = new Vec3f(
                eye.x + (float)Math.sin(angleTh) * (float)Math.cos(angleFi) * RADIUS,
                eye.y + (float)Math.sin(angleTh) * (float)Math.sin(angleFi) * RADIUS,
                eye.z + (float)Math.cos(angleTh));
        totalPoint++;

        for (Quad quad : quads) {
            if(rayInTriangle(quad.A, quad.B, quad.C, eye, ray) || rayInTriangle(quad.B, quad.C, quad.D, eye, ray))
                if(rayInTriangle(quad.A, quad.B, quad.C, eye, ray) || rayInTriangle(quad.B, quad.C, quad.D, eye, ray)){

                if(maxArrayCounter == quad.CollisionsCount++){
                    maxArrayCounter++;
                    glWindow.setTitle(String.valueOf(maxArrayCounter) + "; " + String.valueOf(totalPoint));
                }
            }
        }

        for (Quad quad : quads) {
            quad.Color = (quad.CollisionsCount / (float)maxArrayCounter/* * quad.kSquare*/) * 0.5f + 0.5f;
        }

        LastEye = new Vec3f(eye.x, eye.y, eye.z);
        LastRay = new Vec3f(ray.x, ray.y, ray.z);
        LastRay.mul(3.0f);
    }

    private static boolean rayInTriangle(Vec3f A, Vec3f B, Vec3f C, Vec3f eye, Vec3f ray) {
        if(new Vec3f(ray.x - eye.x, ray.y - eye.y, ray.z - eye.z).dot(new Vec3f(A.x - eye.x, A.y - eye.y, A.z - eye.z)) < 0.0f)
            return false;

        Vec3f BA = new Vec3f();
        BA.sub(B, A);
        Vec3f CA = new Vec3f();
        CA.sub(C, A);

        Vec3f N = new Vec3f();
        N.cross(BA, CA);

        float D = -(N.x * A.x) - (N.y * A.y) - (N.z * A.z);
        float k = -(N.x * eye.x + N.y * eye.y + N.z * eye.z + D) / (N.x * ray.x + N.y * ray.y + N.z * ray.z);
        Vec3f P = new Vec3f(k * ray.x + eye.x, k * ray.y + eye.y, k * ray.z + eye.z);

        Vec3f v0 = CA;
        Vec3f v1 = BA;
        Vec3f v2 = new Vec3f();
        v2.sub(P, A);

        float dot00 = v0.dot(v0);
        float dot01 = v0.dot(v1);
        float dot02 = v0.dot(v2);
        float dot11 = v1.dot(v1);
        float dot12 = v1.dot(v2);

        float invDenom = 1.0f / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        if ((u >= 0) && (v >= 0) && (u + v <= 1.0f))
        {
            return true;
        }

        return false;
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
            gl.glVertex2f(quad.x * sizeCells, quad.y * sizeCells);
            gl.glVertex2f(quad.x * sizeCells + sizeCells, quad.y * sizeCells);
            gl.glVertex2f(quad.x * sizeCells + sizeCells, quad.y * sizeCells + sizeCells);
            gl.glVertex2f(quad.x * sizeCells, quad.y * sizeCells + sizeCells);
        }
        gl.glEnd();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
