/*
 * Adapted from LWJGL example
 */

package GUI;

import Backend.AxesUseCase;
import Graphics.Grapher;
import Graphics.RGBA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static Graphics.ImageTest.getImDims;
import static Graphics.ImageTest.readImage;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * A User Interface which uses GLFW
 * TODO: split into UI, Controller, Presenter
 */
public class GLGUI implements GUI {
    static int clicks;
    static int progID;

    static float zx;
    static float zy;
    static float mousex;
    static float mousey;

    static boolean textureTest;

    public String equation;
    public int imgDim;
    public Grapher grapher;
    public AxesUseCase auc;
    public String gType;

    public GLGUI(String eq, int imgDim) {
        this.equation = eq;
        this.imgDim = imgDim;
        textureTest = true;
    }
    public GLGUI(Grapher grapher, int imgDim) {
        this.imgDim = imgDim;
        this.grapher = grapher;
        this.equation = "x + y";
        textureTest = true;
        this.auc = new AxesUseCase();
    }

    public void setgType(String gType) {
        this.gType = gType;
    }

    public static void main(String[] args) throws IOException {
        textureTest = false;
        String eq = "(cos(x + y) + sin(x*y))/4 + 0.5";
        if (args.length > 0) {
            eq = args[0];
        }
        if (args.length > 1) {
            textureTest = true;
            System.out.println("testing texture");
        }
        GLGUI guiDemo = new GLGUI(eq, 800);
        guiDemo.initGL();
        System.out.println("Fin.");
    }

    /**
     * Converts an int[] RGBA data to a GL texture
     * @param pixels array representing image
     * @param iw width of input
     * @param ih height of input
     * @return the GL texture ID
     */
    public static int imgToTex(int[] pixels, int iw, int ih) {
        // Convert int[] RGBA to packed byte[] RGBA for OpenGL use
        ByteBuffer tbuf = ByteBuffer.allocateDirect(4 * iw * ih);
        byte[] pixbytes = new byte[4*iw*ih];
        for (int i = 0; i < iw*ih; i++) {
            RGBA rgba = new RGBA(pixels[i]);
            pixbytes[4*i] = (byte)(rgba.r);
            pixbytes[4*i+1] = (byte)(rgba.g);
            pixbytes[4*i+2] = (byte)(rgba.b);
            pixbytes[4*i+3] = (byte)(rgba.a);
        }
        tbuf.put(pixbytes);
        tbuf.flip();
        // Attach to GL texture, set filtering
        int tid = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tid);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, iw, ih, 0, GL_RGBA, GL_UNSIGNED_BYTE, tbuf);
        return tid;
    }

    /**
     * Compiles and links GL shader templates
     * @param eq an expression with x and y
     * @throws IOException
     */
    public static void makeShader(String eq) throws IOException {
        String vertShader;
        String fragShader;

        vertShader = new String(Files.readAllBytes(Paths.get("src/main/java/GUI/basicVertex.c")));
        fragShader = new String(Files.readAllBytes(Paths.get("src/main/java/GUI/demoFrag.c")));

        fragShader = fragShader.replace("[INSERT EQUATION HERE]", eq);

        if (textureTest) {
            fragShader = fragShader.replace("//[INSERT TEXTURE TEST]", "fragColor = texture(texTest, tc*wh);");
        }

        //System.out.println(fragShader);

        progID = glCreateProgram();
        int vsID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsID, vertShader);
        glCompileShader(vsID);
        if (glGetShaderi(vsID, GL_COMPILE_STATUS) != GL_TRUE) {
            System.out.println(glGetShaderInfoLog(vsID, glGetShaderi(vsID, GL_INFO_LOG_LENGTH)));
        }
        System.out.println("vs created");

        int fsID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsID, fragShader);
        glCompileShader(fsID);
        if (glGetShaderi(fsID, GL_COMPILE_STATUS) != GL_TRUE) {
            // Error compiling fragment shader
            System.out.println(glGetShaderInfoLog(fsID, glGetShaderi(fsID, GL_INFO_LOG_LENGTH)));
        } else {
            System.out.println("fs created");
        }

        glAttachShader(progID, vsID);
        glAttachShader(progID, fsID);
        glLinkProgram(progID);
    }

    private static void cursor_pos_callback(long l, double x, double y) {
        mousex = (float)(x-400)/200.f;
        mousey = (float)(y-400)/200.f;
        glUniform1f(0, mousex + zx);
        glUniform1f(1, mousey + zy);
    }

    public void initGUI() {
        try {
            initGL();
        } catch (IOException e) {
            System.out.println("Error reading assets!");
        }
    }

    /**
     * Initializes the GLFW and GL environments.
     * @throws IOException
     */
    public void initGL() throws IOException {
        glfwInit();
        long window = createWindow();
        glfwSetMouseButtonCallback(window, GLGUI::mouseCallback);
        glfwSetCursorPosCallback(window, GLGUI::cursor_pos_callback);

        FloatBuffer buffer = memAllocFloat(3 * 2 * 2);
        float[] vtest = {
                -0.9f, -0.9f, 0.9f, -0.9f, -0.9f, 0.9f,
                0.9f, -0.9f, -0.9f, 0.9f, 0.9f, 0.9f
        };
        buffer.put(vtest);

        buffer.flip();

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        int vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        makeShader(this.equation);
        glUseProgram(progID);
        memFree(buffer);

        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, GL_FLOAT, 0, 0L);
        glClearColor(0.1f, 0.2f, 0.3f, 0.0f);

        startLoop(window);
    }

    /**
     * Enters mainloop for UI window
     * @param window handle of the window
     */
    public void startLoop(long window) {
        int[] pixels;
        while (!glfwWindowShouldClose(window)) {
            float[] newO = {-mousex, mousey};
            this.grapher.setPos(newO);
            pixels = this.grapher.graph(this.imgDim, this.gType);
            imgToTex(pixels, this.imgDim, this.imgDim);

            glfwPollEvents();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            glDrawArrays(GL_TRIANGLES, 0, 6);

            glfwSwapBuffers(window);
        }
        glfwTerminate();
    }

    /**
     * Method that creates a GLFW window
     * @return window handle
     */
    private static long createWindow() {
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        long window = glfwCreateWindow(800, 800, "Intro2", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        createCapabilities();
        return window;
    }

    private static void mouseCallback(long win, int button, int action, int mods) {
        /* Print a message when the user pressed down a mouse button */
        if (action == GLFW_PRESS) {
            int tid = 0;
            try {
                int iw = getImDims("sampleOut3D.png")[0];
                int ih = getImDims("sampleOut3D.png")[1];
                tid = imgToTex(readImage("sampleOut3D.png"), iw,ih);
            } catch (Exception e) {
                System.out.println("Can't read image");
            }
            glUniform1i(3, tid);

            System.out.println("Pressed! " + clicks);
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                clicks += 1;
            } else {clicks -= 1;}
            glUniform1f(2, 1.f/(float)Math.pow(1.1f,clicks));

            zx += mousex;
            zy += mousey;
            glUniform1f(0, mousex + zx);
            glUniform1f(1, mousey + zy);
        }
    }
}