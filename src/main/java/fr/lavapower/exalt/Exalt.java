package fr.lavapower.exalt;

import fr.lavapower.exalt.exceptions.IllegalComponentException;
import fr.lavapower.exalt.render.Camera;
import fr.lavapower.exalt.input.Input;
import fr.lavapower.exalt.render.Shader;
import fr.lavapower.exalt.input.Key;
import fr.lavapower.exalt.utils.Position;
import fr.lavapower.exalt.utils.Size;
import fr.lavapower.exalt.utils.Timer;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Exalt
{
    private static final GLFWErrorCallback errorCallback = GLFWErrorCallback.createThrow();

    private long window;
    private float width;
    private float height;
    private int fps_max = 60;
    private int fps = 0;
    private boolean debug = false;
    private boolean exitOnEsc = true;
    private boolean vSync = true;
    private Input input;
    private World world;
    private Camera camera;

    private void init(String title, float width, float height, boolean fullscreen) {
        glfwSetErrorCallback(errorCallback);

        if(!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        window = glfwCreateWindow((int) width, (int) height, title, fullscreen ? glfwGetPrimaryMonitor() : 0, 0);
        if(window == 0)
        {
            glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window");
        }
        glfwShowWindow(window);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        input = new Input(window);
        world = new World();
        world.exalt = this;
        camera = new Camera(width, height);
        this.width = width;
        this.height = height;
    }

    // Constructors
    public Exalt() { init("Exalt Window", 640, 480, false); }
    public Exalt(String title) { init(title, 640, 480, false); }
    public Exalt(String title, Size size) { init(title, size.width, size.height, false); }
    public Exalt(String title, float width, float height) { init(title, width, height, false); }
    public Exalt(String title, float width, float height, boolean fullscreen) { init(title, width, height, fullscreen); }

    //Size methods
    public Size getSize() { return new Size(width, height); }

    public void setSize(float width, float height) {
        glfwSetWindowSize(window, (int) width, (int) height);
        this.width = width;
        this.height = height;
    }
    public void setSize(Size size) { setSize(size.width, size.height); }

    //Pos method
    public Position getPosition() {
        IntBuffer xBuff = MemoryUtil.memAllocInt(1);
        IntBuffer yBuff = MemoryUtil.memAllocInt(1);

        glfwGetWindowPos(window, xBuff, yBuff);

        int x = xBuff.get();
        int y = yBuff.get();

        MemoryUtil.memFree(xBuff);
        MemoryUtil.memFree(yBuff);

        return new Position(x, y);
    }

    public void setPosition(int x, int y) { glfwSetWindowPos(window, x, y); }
    public void setPosition(Position pos) { glfwSetWindowPos(window, (int) pos.x, (int) pos.y); }
    public void center() {
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        Size size = getSize();
        if(vidMode != null)
            glfwSetWindowPos(window, (int)(vidMode.width() - size.width) / 2, (int)(vidMode.height() - size.height) / 2);
    }

    //FPS
    public int getFPSMax() { return fps_max; }
    public int getCurrentFPS() { return fps; }
    public void setFPSMax(int fpsMax) { fps_max = fpsMax; }

    //Debug
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    //ExitOnEsc
    public boolean isExitOnEsc() { return exitOnEsc; }
    public void setExitOnEsc(boolean exitOnEsc) { this.exitOnEsc = exitOnEsc; }

    //vSync
    public boolean isvSync() { return vSync; }
    public void setvSync(boolean vSync) { this.vSync = vSync; }

    //World
    public World getWorld() { return world; }
    public void setWorld(World world) {
        world.exalt = this;
        this.world = world;
    }

    //Camera
    public Camera getCamera() { return camera; }

    //Others publics mathods
    public Input getInput() { return input; }
    public void stop() { glfwSetWindowShouldClose(window, true); }

    //Others private methods
    private void update() {
        input.update();
        glfwPollEvents();
    }

    //RUN
    public void run() throws IllegalComponentException
    {
        glEnable(GL_TEXTURE_2D);

        if(vSync)
            glfwSwapInterval(1);

        Shader shader = new Shader("shader");

        double frame_time = 0;
        int frames = 0;
        double time = Timer.getTime();
        double unprocessed = 0;
        double passed = 0;

        while(!glfwWindowShouldClose(window)) {
            passed = Timer.getTime() - time;
            time = Timer.getTime();
            unprocessed += passed;
            frame_time += passed;
            if (unprocessed >= 1./fps_max) {
                unprocessed-=1./fps_max;

                if(input.isKeyPressed(Key.ESCAPE) && exitOnEsc) {
                    stop();
                }

                world.update((float)(1./fps_max));
                update();

                if(frame_time >= 1.0) {
                    frame_time = 0;
                    fps = frames;
                    frames = 0;
                    if(debug)
                        System.out.println("FPS : "+fps);
                }

                glClear(GL_COLOR_BUFFER_BIT);

                world.render(shader, camera);

                glfwSwapBuffers(window);
                frames++;
            }
        }

        glfwDestroyWindow(window);
        Callbacks.glfwFreeCallbacks(window);
        glfwTerminate();
        errorCallback.free();
    }
}