/*
 * Adapted from LWJGL examples
 * Copyright 2012-2021 Lightweight Java Game Library
 * BSD 3-clause License
 * License terms: https://www.lwjgl.org/license
 */

package GUI;

import Graphics.Grapher;
import Graphics.RGBA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * A User Interface which uses GLFW
 * TODO: split into UI, Controller, Presenter
 */
public class GLGUI implements GUI {
    static int progID;

    static float zx;
    static float zy;
    static float mousex;
    static float mousey;
    static boolean dragMove = false;
    static boolean prevDragMove = false;
    static float prevMouseX = 0;
    static float prevMouseY = 0;
    static float initialMouseX = 0;
    static float initialMouseY = 0;
    static float changeInMouseX = 0;
    static float changeInMouseY = 0;
    static float graphScale = 5;
    static float scaleInterval = 1.1f;

    static boolean textureTest;

    private final String equation;
    private final int imgDim;

    private GUIHelper guiHelper;

    public GLGUI(String eq, int imgDim) {
        this.equation = eq;
        this.imgDim = imgDim;
        textureTest = true;
    }
    public GLGUI(Grapher grapher, int imgDim) {
        this.imgDim = imgDim;
        this.guiHelper = new GUIHelper(grapher, imgDim);
        this.equation = "x + y";
        textureTest = true;
    }

    public void setgType(String gType) {
        guiHelper.setgType(gType);
    }

    public static void main(String[] args) throws IOException {
        textureTest = false;
//        String eq = JOptionPane.showInputDialog(null, "Enter"); // Has to be implicit for now.
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
     *   set to uniform 0
     * @param pixels array representing image
     * @param iw width of input
     * @param ih height of input
     */
    private static void imgToTex(int[] pixels, int iw, int ih) {
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
    }

    /**
     * Compiles and links GL shader templates
     * @param eq an expression with x and y
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

        progID = glCreateProgram();
        int vsID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vsID, vertShader);
        glCompileShader(vsID);
        if (glGetShaderi(vsID, GL_COMPILE_STATUS) != GL_TRUE) {
            System.out.println(glGetShaderInfoLog(vsID, glGetShaderi(vsID, GL_INFO_LOG_LENGTH)));
        }

        int fsID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsID, fragShader);
        glCompileShader(fsID);
        if (glGetShaderi(fsID, GL_COMPILE_STATUS) != GL_TRUE) {
            // Error compiling fragment shader
            System.out.println(glGetShaderInfoLog(fsID, glGetShaderi(fsID, GL_INFO_LOG_LENGTH)));
        }

        glAttachShader(progID, vsID);
        glAttachShader(progID, fsID);
        glLinkProgram(progID);
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
     */
    public void initGL() throws IOException {
        glfwInit();
        long window = createWindow();
        glfwSetMouseButtonCallback(window, GLGUI::mouseCallback);
        glfwSetCursorPosCallback(window, GLGUI::cursor_pos_callback);
        glfwSetKeyCallback(window, GLGUI::keyboardCallback);

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
            float[] newO = {prevMouseX + changeInMouseX, prevMouseY + changeInMouseY};
            guiHelper.setGraphPos(newO);
            guiHelper.setGraphScale(graphScale);
            pixels = guiHelper.drawGraph();
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

    private static void cursor_pos_callback(long l, double x, double y) {
        mousex = (float)(x-400)/200.f;
        mousey = (float)(y-400)/200.f;
        glUniform1f(0, mousex + zx);
        glUniform1f(1, mousey + zy);

        if (dragMove) {
            if (!prevDragMove) {
                initialMouseX = mousex;
                initialMouseY = mousey;
                prevDragMove = true;
            }
            else {
                changeInMouseX = -(mousex - initialMouseX) * graphScale / 5;  // TODO: Make '5' a variable? It's like a normalizing constant.
                changeInMouseY = (mousey - initialMouseY) * graphScale / 5;  // TODO: Make '5' a variable? It's like a normalizing constant. Or make it another option move fast/slow mode?!!!
            }
        }
        else {
            prevMouseX += changeInMouseX;
            prevMouseY += changeInMouseY;
            changeInMouseX = 0;
            changeInMouseY = 0;
        }
    }

    private static void mouseCallback(long win, int button, int action, int mods) { // TODO: To Louis, why is the first parameter not GLFWWindow* instead? As specified in the java docs.
        // Below Ted: Mouse drag:
        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_LEFT) {
            dragMove = true;
        }
        if (action == GLFW_RELEASE && button == GLFW_MOUSE_BUTTON_LEFT) {
            dragMove = false;
            prevDragMove = false; // Well, technically doesn't match its name, but enough for our purpose.
        }
    }

    private static void keyboardCallback(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            if (key == GLFW_KEY_UP) {
                graphScale *= scaleInterval;
            }
            else if (key == GLFW_KEY_DOWN) {
                graphScale /= scaleInterval;
            }
        }
    }
}
