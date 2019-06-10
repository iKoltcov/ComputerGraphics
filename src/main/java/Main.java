import Labs.*;
import Labs.Abstractions.LabAbstraction;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

public class Main {
    public static int WINDOW_WIDTH = 1024;
    public static int WINDOW_HEIGHT = 768;

    public static void main(String[] args) {
        GLProfile glProfile = GLProfile.get(new String[] { GLProfile.GL2 }, true);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        final GLWindow glWindow = GLWindow.create(glCapabilities);
        glWindow.setAnimator(new FPSAnimator(glWindow, 60, true));
        glWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        glWindow.setVisible(true);

        final LabAbstraction lab = new Lab5(WINDOW_WIDTH, WINDOW_HEIGHT, glWindow);

        glWindow.addGLEventListener(lab);
        glWindow.addWindowListener(new WindowAdapter() {
            public void windowDestroyNotify(WindowEvent e) {
                glWindow.getAnimator().stop();
                System.exit(0);
            }
        });

        glWindow.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if(e.VK_SPACE == e.getKeyCode())
                    lab.toggleStop();

                if(e.VK_ENTER == e.getKeyCode())
                    lab.addPoint();

                if(e.VK_R == e.getKeyCode())
                    lab.clear();
            }
        });

        glWindow.getAnimator().start();
    }
}
