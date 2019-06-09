package Labs;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec3f;

import java.util.ArrayList;
import java.util.Random;

public class Lab2 extends LabAbstract {
    private GL2 gl;
    private GLU glu;
    private int width, height;
    private final GLWindow glWindow;

    private Random random = new Random();

    private Vec3f P0;
    private float R;


    private volatile int maxArrayCounter;
    private volatile long totalPoints;
    private int[][] arrayCounter;
    private ArrayList<Thread> threads;

    public Lab2(int width, int height, GLWindow glWindow){
        this.width = width;
        this.height = height;
        this.glWindow = glWindow;

        arrayCounter = new int[width][height];
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                arrayCounter[i][j] = 0;
        maxArrayCounter = 0;
        totalPoints = 0;
    }

    @Override
    public void clear() {
        arrayCounter = new int[width][height];
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                arrayCounter[i][j] = 0;
        maxArrayCounter = 0;

        glWindow.setTitle(String.valueOf(maxArrayCounter));
    }

    public synchronized void addPoint(){
        totalPoints++;

        float length = R * (float)Math.sqrt(random.nextDouble());
        float angle = 2.0f * (float)Math.PI * random.nextFloat();

        int x = (int)(P0.x + Math.cos(angle) * length);
        int y = (int)(P0.y + Math.sin(angle) * length);
        if(x >= 0 && x < width && y >= 0 && y < height){
            if(maxArrayCounter == arrayCounter[x][y]++){
                maxArrayCounter++;
                glWindow.setTitle(String.format("%d / %d", maxArrayCounter, totalPoints));
            }
        }
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

        R = Math.min(width, height) * 0.3f;
        P0 = new Vec3f(width * 0.5f, height * 0.5f, 0.0f);

        threads = new ArrayList<Thread>();
        for(int i = 0; i < 4; i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        if(isStop || totalPoints > 1000000000) {
                            glWindow.setTitle(String.format("%d / %d", maxArrayCounter, totalPoints));
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

    @Override
    public void dispose(GLAutoDrawable drawable) {
        for (Thread thread : threads) {
            thread.interrupt();
        };
        drawable.destroy();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(gl.GL_POINTS);
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                if(arrayCounter[i][j] > 0){
                    float color = ((float)arrayCounter[i][j] / (float)maxArrayCounter) * 0.5f + 0.5f;
                    if(color > 1.0f)
                        color = 1.0f;

                    gl.glColor3f(color, color, color);
                    gl.glVertex3f(i, j, 0);
                }
        gl.glEnd();

        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glColor3f( 1.0f, 1.0f, 1.0f);
        for(float angle = 0; angle < Math.PI * 2.0f; angle += Math.PI * 0.01f)
        {
            gl.glVertex3f((float)(P0.x + (Math.cos(angle) * R)), (float)(P0.y + (Math.sin(angle) * R)), 0.0f);
        }
        gl.glEnd();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
