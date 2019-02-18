package Labs;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.sun.javafx.geom.Vec3f;

import java.util.Random;

public class Lab1 implements GLEventListener {
    private GL2 gl;
    private GLU glu;
    private int width, height;

    private Random random = new Random();

    private Vec3f P0, P1, P2;

    private int maxArrayCounter;
    private int[][] arrayCounter;
    private Thread thread;

    public Lab1(int width, int height){
        this.width = width;
        this.height = height;

        arrayCounter = new int[width][height];
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                arrayCounter[i][j] = 0;
        maxArrayCounter = 0;
    }

    private synchronized void addPoint(){
        float randomA = random.nextFloat();
        float randomB = random.nextFloat();

        if(randomA + randomB > 1){
            randomA = 1 - randomA;
            randomB = 1 - randomB;
        }

        Vec3f A = new Vec3f(P1.x, P1.y, P1.z);
        A.sub(P0);
        A.mul(randomA);

        Vec3f B = new Vec3f(P2.x, P2.y, P2.z);
        B.sub(P0);
        B.mul(randomB);

        Vec3f newPoint = new Vec3f(A.x, A.y, A.z);
        newPoint.add(B);
        newPoint.add(P0);

        int x = (int)newPoint.x;
        int y = (int)newPoint.y;
        if(x >= 0 && x < width && y >= 0 && y < height){
            if(maxArrayCounter == arrayCounter[x][y]++){
                maxArrayCounter++;
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

        float radius = Math.min(width, height) * 0.5f;
        float xCenter = width * 0.5f,
              yCenter = height * 0.5f;

        float alpha1 = random.nextInt(180);
        float alpha2 = alpha1 + 90;
        float alpha3 = alpha2 + 90;

        P0 = new Vec3f((float)(xCenter + (Math.cos(alpha1) * radius)), (float)(yCenter + (Math.sin(alpha1) * radius)), 0.0f);
        P1 = new Vec3f((float)(xCenter + (Math.cos(alpha2) * radius)), (float)(yCenter + (Math.sin(alpha2) * radius)), 0.0f);
        P2 = new Vec3f((float)(xCenter + (Math.cos(alpha3) * radius)), (float)(yCenter + (Math.sin(alpha3) * radius)), 0.0f);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!thread.isInterrupted())
                    addPoint();
            }
        });
        thread.start();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        thread.interrupt();
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
                    float color = ((float)arrayCounter[i][j] / (float)maxArrayCounter);
                    if(color > 1.0f)
                        color = 1.0f;

                    gl.glColor3f(color, color, color);
                    gl.glVertex3f(i, j, 0);
                }
        gl.glEnd();

        gl.glColor3f( 1.0f, 1.0f, 1.0f );
        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glVertex3f(P0.x, P0.y, P0.z);
            gl.glVertex3f(P1.x, P1.y, P1.z);
            gl.glVertex3f(P2.x, P2.y, P2.z);
        gl.glEnd();

        gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
