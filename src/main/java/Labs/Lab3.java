package Labs;

import Labs.Entities.Polygon;
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


    private float radius = 1.0f;
    private volatile int maxArrayCounter;
    private float maxSquare;
    private ArrayList<Polygon> polygons;
    private Vec3f Camera;
    private Vec3f Target;

    public Lab3(int width, int height, GLWindow glWindow){
        this.width = width;
        this.height = height;
        this.glWindow = glWindow;

        Camera = new Vec3f(0.0f, 0.0f, 2.0f);
        Target = new Vec3f(0.0f, 0.0f, 0.0f);
        polygons = new ArrayList<Polygon>();
        maxArrayCounter = 0;

        float step = (float)Math.PI * 0.02f;
        for(float th = 0.0f; th < Math.PI; th += step)
        {
            for (float fi = 0.0f; fi < 2.0f * Math.PI; fi += step) {
                Vec3f A = new Vec3f(
                        Target.x + radius * (float)(Math.sin(th) * Math.cos(fi)),
                        Target.y + radius * (float)(Math.sin(th) * Math.sin(fi)),
                        Target.z + radius * (float)Math.cos(th));
                Vec3f B1 = new Vec3f(
                        Target.x + radius * (float)(Math.sin(th + step) * Math.cos(fi)),
                        Target.y + radius * (float)(Math.sin(th + step) * Math.sin(fi)),
                        Target.z + radius * (float)Math.cos(th + step));
                Vec3f B2 = new Vec3f(
                        Target.x + radius * (float)(Math.sin(th) * Math.cos(fi + step)),
                        Target.y + radius * (float)(Math.sin(th) * Math.sin(fi + step)),
                        Target.z + radius * (float)Math.cos(th));
                Vec3f C = new Vec3f(
                        Target.x + radius * (float)(Math.sin(th + step) * Math.cos(fi + step)),
                        Target.y + radius * (float)(Math.sin(th + step) * Math.sin(fi + step)),
                        Target.z + radius * (float)Math.cos(th + step));

                polygons.add(new Polygon(A, B1, C));
                polygons.add(new Polygon(A, B2, C));
            }
        }

        maxSquare = 0.0f;
        for (Polygon polygon : polygons) {
            polygon.Square = distance(polygon.A, polygon.B) * distance(polygon.A, polygon.C) / 2.0f;
            if(polygon.Square > maxSquare){
                maxSquare = polygon.Square;
            }
        }

        for (Polygon polygon : polygons) {
            polygon.kSquare = polygon.Square / maxSquare;
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
        Camera.z = Target.z + (float)Math.sin(cameraPosition) * 1.5f;
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
        Vec3f eye = new Vec3f(0.0f, 0.0f, 0.0f);
        Vec3f ray = new Vec3f(
                eye.x + (float)Math.cos(angleTh) * (float)Math.sin(angleFi) * radius,
                eye.y + (float)Math.sin(angleTh) * (float)Math.sin(angleFi) * radius,
                eye.z + (float)random.nextFloat());

        for (Polygon polygon : polygons) {
            if(rayInTriangle(polygon.A, polygon.B, polygon.C, eye, ray)){
                if(maxArrayCounter == polygon.CollisionsCount++){
                    maxArrayCounter++;
                    glWindow.setTitle(String.valueOf(maxArrayCounter));
                }
            }
        }

        for (Polygon polygon : polygons) {
            polygon.Color = (polygon.CollisionsCount / (float)maxArrayCounter * polygon.kSquare) * 0.5f + 0.5f;
        }
    }

    private static boolean rayInTriangle(Vec3f A, Vec3f B, Vec3f C, Vec3f eye, Vec3f ray) {
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

        if ((u >= 0) && (v >= 0) && (u + v < 1.0f))
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
        gl.glLoadIdentity();

        gl.glBegin(gl.GL_TRIANGLES);
        for (Polygon polygon : polygons) {
            gl.glColor3f(polygon.Color, polygon.Color, polygon.Color);
            gl.glVertex3f(polygon.A.x, polygon.A.y, polygon.A.z);
            gl.glVertex3f(polygon.B.x, polygon.B.y, polygon.B.z);
            gl.glVertex3f(polygon.C.x, polygon.C.y, polygon.C.z);
        }
        gl.glEnd();

//        gl.glColor3f(0.0f, 0.0f, 0.0f);
//        float k = 1.01f;
//        for (Polygon polygon : polygons) {
//            gl.glBegin(gl.GL_LINE_LOOP);
//                gl.glVertex3f(polygon.A.x * k, polygon.A.y * k, polygon.A.z * k);
//                gl.glVertex3f(polygon.B.x * k, polygon.B.y * k, polygon.B.z * k);
//                gl.glVertex3f(polygon.C.x * k, polygon.C.y * k, polygon.C.z * k);
//            gl.glEnd();
//        }

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
