package net.georgewhiteside.android.abstractart;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class GLOffscreenSurface {
    final static String TAG = "GLOffscreenSurface";

    GLSurfaceView.Renderer mRenderer;
    int mWidth, mHeight;
        
    EGL10 mEGL; 
    EGLDisplay mEGLDisplay;
    EGLConfig[] mEGLConfigs;
    EGLConfig mEGLConfig;
    EGLContext mEGLContext;
    EGLSurface mEGLSurface;
    int mEGLContextClientVersion;
    EGLConfigChooser mEGLConfigChooser;
    EGLContextFactory mEGLContextFactory;
    GL10 mGL;
    
    String mThreadOwner;
    
    public GLOffscreenSurface(int width, int height) {
        mWidth = width;
        mHeight = height;
    }
    
    public boolean checkCurrentThread()
    {
    	if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            return false;
        } else {
        	return true;
        }
    }
    
    public void setRenderer(GLSurfaceView.Renderer renderer) {
    	// add the ConfigChoosers and whatnot later? I'll probably never use it
    	if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
    	if (mEGLContextFactory == null) {
            mEGLContextFactory = new DefaultContextFactory();
        }
    	
    	configure();
    	
        mRenderer = renderer;
        
        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.");
            return;
        }
        
        // Call the renderer initialization routines
        mRenderer.onSurfaceCreated(mGL, mEGLConfig);
        mRenderer.onSurfaceChanged(mGL, mWidth, mHeight);
    }
    
    public void configure()
    {
    	int[] version = new int[2];
        int[] attribList = new int[] {
        	EGL10.EGL_WIDTH, mWidth,
        	EGL10.EGL_HEIGHT, mHeight,
        	EGL10.EGL_NONE
        };
                
        mEGL = (EGL10) EGLContext.getEGL();	// get an EGL instance
        
        mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY); // get to the default display
        
        if (mEGLDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
        
        // initialize EGL display
        
        if(!mEGL.eglInitialize(mEGLDisplay, version)) {
        	throw new RuntimeException("eglInitialize failed");
        }
        
        mEGLConfigChooser = new SimpleEGLConfigChooser(true); // 
        mEGLConfig = mEGLConfigChooser.chooseConfig(mEGL, mEGLDisplay);
        
        mEGLContextFactory = new DefaultContextFactory();
        mEGLContext = mEGLContextFactory.createContext(mEGL, mEGLDisplay, mEGLConfig);
        if (mEGLContext == null || mEGLContext == EGL10.EGL_NO_CONTEXT) {
        	mEGLContext = null;
            throw new RuntimeException("createContext failed");
        }
        
        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig,  attribList); // create an EGL surface we can render into
        
        if (mEGLSurface == null || mEGLSurface == EGL10.EGL_NO_SURFACE) {
            Log.e("eglCreatePbufferSurface", "something went terribly wrong while attempting to create pbuffer surface");
            return;
        }
        
        if(!mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
        	throw new RuntimeException("eglMakeCurrent failed");
        }
        
        mGL = (GL10)mEGLContext.getGL();
        mThreadOwner = Thread.currentThread().getName(); // Record thread owner of OpenGL context
    }
        
    public Bitmap getBitmap() {
        // Do we have a renderer?
        if (mRenderer == null) {
            Log.e(TAG, "getBitmap: Renderer was not set.");
            return null;
        }
        
        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }
                
        // Call the renderer draw routine
        mRenderer.onDrawFrame(mGL);
       
        return workingConvertToBitmap();
    }
    
    
    
    
    
    public void setEGLContextClientVersion(int version) {
        mEGLContextClientVersion = version;
    }
    
    public interface EGLContextFactory {
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);
        void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
    }
    
    private class DefaultContextFactory implements EGLContextFactory {
        private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion, EGL10.EGL_NONE}; 
            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, mEGLContextClientVersion != 0 ? attrib_list : null);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                throw new RuntimeException("eglDestroyContext failed");
            }
        }
    }
    
    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link GLSurfaceView#setEGLConfigChooser(EGLConfigChooser)}
     */
    public interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL10#eglChooseConfig} and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         * @param egl the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs);

        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (mEGLContextClientVersion != 2) {
                return configSpec;
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
            newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
            newConfigSpec[len] = 4; /* EGL_OPENGL_ES2_BIT */
            newConfigSpec[len+1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
       }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                             EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                              EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    /**
     * This class will choose a RGB_565 surface with
     * or without a depth buffer.
     *
     */
    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(5, 6, 5, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }
    
    
    
    
    
    
    private Bitmap workingConvertToBitmap() {
    	Bitmap bitmap;
    	IntBuffer ib = IntBuffer.allocate(mWidth*mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth*mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal image.
        for (int i = 0; i < mHeight; i++) {     
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight-i-1)*mWidth + j, ib.get(i*mWidth + j));
            }
        }                  

        bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ibt);
        
        return bitmap;
    }
    
        
    private Bitmap convertToBitmap() {
    	Bitmap bitmap;
        IntBuffer ib = IntBuffer.allocate(mWidth*mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth*mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        /*
        for(int y = 0; y < mHeight; y++) {     
            for(int x = 0; x < mWidth; x++) {
                ibt.put((mHeight-y-1)*mWidth + x, ib.get(y*mWidth + x));
            }
        }         
        */         

        bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ib);
        return Bitmap.createBitmap(ib.array(), mWidth, mHeight, Bitmap.Config.ARGB_8888);
    } 
}
