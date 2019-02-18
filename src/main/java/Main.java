import Labs.Lab1;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
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
        glWindow.setTitle("Lab1");

        glWindow.addGLEventListener(new Lab1(WINDOW_WIDTH, WINDOW_HEIGHT, glWindow));
        glWindow.addWindowListener(new WindowAdapter() {
            public void windowDestroyNotify(WindowEvent e) {
                glWindow.getAnimator().stop();
                System.exit(0);
            }
        });

        glWindow.getAnimator().start();
    }
}
