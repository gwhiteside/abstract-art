package net.georgewhiteside.android.abstractart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import net.georgewhiteside.android.abstractart.Wallpaper;
import net.georgewhiteside.android.abstractart.Wallpaper.AbstractArtEngine;

import org.jf.GLWallpaper.GLWallpaperService;

// float refreshrate = getWindowManager().getDefaultDisplay().getRefreshRate();

// "The PowerVR 530/535 is very slow. Andreno 200 and PowerVR 530/535 are first GPU generation
// (OpenGL ES 2.x) for hdpi resolution. You can't redraw a full screen at 60FPS with a simple texture."

public class Renderer implements GLWallpaperService.Renderer, GLSurfaceView.Renderer
{
	private static final String TAG = "Renderer";
	private Context context;
	
	private SharedPreferences sharedPreferences;
	
	public BattleGroup battleGroup;
	private ShaderFactory shader;
	
	private FPSCounter mFPSCounter = new FPSCounter();
	
	private FloatBuffer quadVertexBuffer;
	private FloatBuffer textureVertexBuffer;
	private FloatBuffer textureOutputBuffer;
	
	private int mProgram, hFXProgram;
	private int mPositionHandle, hPosition;
	private int mTextureHandle, hTexture;
	private int mBaseMapTexId;
	private int mBg3TextureLoc, hTextureA;
	private int mBg4TextureLoc;
	private int hBaseMap;
	private int mPaletteLoc;
	
	private int mPaletteRotation;
	
	private int mResolutionLoc;
	private int mCompressionLoc;
	private int mBg3DistTypeLoc, mBg4DistTypeLoc;
	private int mBg3DistLoc, mBg4DistLoc;
	private int mDistTypeLoc;
	private int mBg3Scroll, mBg4Scroll;
	private int mBg3PaletteLoc, mBg4PaletteLoc;
	private int mBg3CompressionLoc, mBg4CompressionLoc;
	private int mBg3RotationLoc, mBg4RotationLoc;
	private int mCycleTypeLoc;
	
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private float mRenderWidth;
	private float mRenderHeight;
	
	private int mSurfaceVerticalOffset = 0;
	private int mSurfaceHorizontalOffset = 0;
	
	private int hMVPMatrix;
	private float[] mProjMatrix = new float[16];
	
	private int[] mFramebuffer = new int[1];
	private int[] mRenderTexture = new int[1];
	
	private Boolean mFilterBackgrounds = false;
	private Boolean mFilterOutput = false;
	
	private Boolean mRenderEnemies = true;
	private int mEnemyPositionHandle;
	private int mEnemyTextureHandle;
	private int mEnemyTextureLoc;
	private FloatBuffer enemyVertexBuffer;
	private FloatBuffer enemyTextureVertexBuffer;
	
	private float mLetterBoxSize;
	
	private int[] mTextureId = new int[3];
	private ByteBuffer mTextureA, mTextureB;
	private ByteBuffer mPalette;
	
	private int[] mBattleSpriteId = new int[1];
	private int mBattleSpriteProgramId;
	
	private int currentBackground;
	private boolean persistBackgroundSelection;
	
	public boolean isPreview;
	
	private long startTime, endTime;
	
	Random rand = new Random();
	
	private boolean mHighRes = false;
	private long frameTime = 60;
	
	private boolean mirrorVertical = false;
	
	private Object lock = new Object();
	
	public int getRomBackgroundIndex(int address)
	{
		return battleGroup.battleBackground.getRomBackgroundIndex(address);
	}
	
	public int getCacheableImagesTotal()
	{
		int images = 103; // TODO: don't hardcode this
		
		return images;
	}
	
	public int getBackgroundsTotal()
	{
		return battleGroup.battleBackground.getNumberOfBackgrounds();
	}
	
	public void cacheImage(int index)
	{
		battleGroup.load(index);
	}
	
	public void setRandomBackground()
	{
		int number = Wallpaper.random.nextInt(battleGroup.battleBackground.getNumberOfBackgrounds() - 1) + 1;
		loadBattleBackground(number);
	}
	
	public void setPersistBackgroundSelection(boolean value)
	{
		persistBackgroundSelection = value;
	}
	
	public void setIsPreview(boolean value)
	{
		isPreview = value;
	}
	
	public Renderer(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		battleGroup = new BattleGroup(context);
		shader = new ShaderFactory(context);
		mTextureA = ByteBuffer.allocateDirect(256 * 256 * 1);
		mTextureB = ByteBuffer.allocateDirect(256 * 256 * 1);
		mPalette = ByteBuffer.allocateDirect(16 * 16 * 4);
		
		startTime = endTime = 0;
		
		isPreview = false;
		
		currentBackground = -1;
		persistBackgroundSelection = false;
		
		Log.i(TAG, "Renderer created");
	}
	
	public Renderer(Context context, boolean mirrorVertical)
	{
		this(context);
		this.mirrorVertical = mirrorVertical;
	}
	
	public Renderer(Context context, int initialBackground)
	{
		this(context);
		this.currentBackground = initialBackground;
	}
	
	public void onDrawFrame(GL10 unused)
	{
		endTime = System.currentTimeMillis();
		long delta = endTime - startTime;
		if(delta < frameTime)
		{
			try {
				Thread.sleep(frameTime - delta);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		startTime = System.currentTimeMillis();
		
		mFPSCounter.logStartFrame();
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); // target screen
		//GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
		//mRenderWidth = mSurfaceWidth;
		//mRenderHeight = mSurfaceHeight;
		
		renderScene();
			
		battleGroup.battleBackground.doTick();
		
		mFPSCounter.logEndFrame();
	}

	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		
		float surfaceRatio = (float) mSurfaceWidth / mSurfaceHeight;
		float textureRatio = 256.0f / 224.0f;
		
		mSurfaceHorizontalOffset = 0;
		mSurfaceVerticalOffset = 0;
		mLetterBoxSize = 0.0f;
		
		if(surfaceRatio == textureRatio) // thumbnail hack (won't scale if someone actually uses an 8:7 screen ratio)
		{
			
		}
		else
		{
		    int outputScaler = 1;
		    
		    if(surfaceRatio > textureRatio)
		        outputScaler = (int)(mSurfaceWidth / 256) + 1; // landscape
		    
		    if(surfaceRatio < textureRatio)
		        outputScaler = (int)(mSurfaceHeight / 224) + 1; //portrait
		    
		    int bestWidthFit = outputScaler * 256;
            int bestHeightFit = outputScaler * 224;
            
            mSurfaceWidth = bestWidthFit;
            mSurfaceHeight = bestHeightFit;
            mSurfaceVerticalOffset = (height - bestHeightFit) / 2;
            mSurfaceHorizontalOffset = (width - bestWidthFit) / 2;
		}
		
		Matrix.orthoM(mProjMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
	}
	
	private void setupScreenQuad()
	{
		float quadVertices[] =
		{
			-1.0f,	-1.0f,	 0.0f,
			 1.0f,	-1.0f,	 0.0f,
			-1.0f,	 1.0f,	 0.0f,
			 1.0f,	 1.0f,	 0.0f			 
		};
		
		float textureMap[] =
		{
				0.0f,	 0.875f,
				 1.0f,	 0.875f,
				 0.0f,	 0.0f,
				 1.0f,	 0.0f 
		};
		
		float textureMapFlip[] =
		{
				0.0f,	 0.0f,
				 1.0f,	 0.0f,
				 0.0f,	 0.875f,
				 1.0f,	 0.875f 
		};

		quadVertexBuffer = ByteBuffer
				.allocateDirect(quadVertices.length * 4) // float is 4 bytes
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer(); 
		quadVertexBuffer.put(quadVertices);
		quadVertexBuffer.position(0);
		
		textureVertexBuffer = ByteBuffer
				.allocateDirect(textureMap.length * 4) // float is 4 bytes
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer(); 
		textureVertexBuffer.put(textureMap);
		textureVertexBuffer.position(0);
		
		textureOutputBuffer = ByteBuffer
				.allocateDirect(textureMap.length * 4) // float is 4 bytes
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer(); 
		textureOutputBuffer.put(textureMapFlip);
		textureOutputBuffer.position(0);
	}

	public void onSurfaceCreated( GL10 unused, EGLConfig config )
	{
		//queryGl(unused);
		
		setupScreenQuad();
		
		GLES20.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );	// set surface background color
		GLES20.glDisable(GLES20.GL_DITHER); // dithering causes really crappy/distracting visual artifacts when distorting the textures
		
		GLES20.glGenFramebuffers(1, mFramebuffer, 0);
		GLES20.glGenTextures(1, mRenderTexture, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTexture[0]);
		GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 256, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null );//GLES20.GL_UNSIGNED_SHORT_5_6_5, null ); //GLES20.GL_UNSIGNED_BYTE, null );
		int filter = mFilterOutput ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]); // do I need to do this here?
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mRenderTexture[0], 0); // specify texture as color attachment
		
		if(isPreview)
		{
			setRandomBackground();
		}
		else if(persistBackgroundSelection && currentBackground >= 0 && currentBackground < getBackgroundsTotal())
		{
			loadBattleBackground(currentBackground);
		}
		else
		{
			//setRandomBackground();
			Wallpaper.setNewBackground(this);
		}
		
		/* shader for final output texture (the "low res") output */
		
		hFXProgram = createProgram(readTextFile(R.raw.passthrough_vert), readTextFile(R.raw.passthrough_frag));
		if(hFXProgram == 0) { throw new RuntimeException("[...] shader compilation failed"); }
		
		hPosition = GLES20.glGetAttribLocation(hFXProgram, "a_position"); // a_position
		hTexture = GLES20.glGetAttribLocation(hFXProgram, "a_texCoord"); // a_texCoord
		hBaseMap = GLES20.glGetUniformLocation(hFXProgram, "s_texture"); // get sampler locations
		
		// used when rendering to the GLOffscreenSurface to mirror screenshots about the horizontal axis
		
		if(mirrorVertical) {
			Matrix.scaleM(mProjMatrix, 0, 1, -1, 1);
		}
		
		// handle the rendering knobs
		
		frameTime = 1000 / 60; //frameTime = 1000 / Integer.valueOf(sharedPreferences.getString("intFramerate", null));
		
		
		Log.i(TAG, "Surface created");
	}

	private void updateShaderVariables()
	{
		// glUniform* calls always act on the current program that is bound with glUseProgram
		// have this method take an argument to determine which program to apply to
		
		Layer bg3 = battleGroup.battleBackground.getBg3();
		Layer bg4 = battleGroup.battleBackground.getBg4();
		
		// update shader resolution
		
		GLES20.glUniform2f(mResolutionLoc, mRenderWidth, mRenderHeight);
		
		// update distortion effect variables for the shader program
		
		GLES20.glUniform1i(mBg3DistTypeLoc, bg3.distortion.getType());
		GLES20.glUniform1i(mBg4DistTypeLoc, bg4.distortion.getType());
		GLES20.glUniform3f(mBg3DistLoc, bg3.distortion.computeShaderAmplitude(), bg3.distortion.computeShaderFrequency(), bg3.distortion.computeShaderSpeed());
		GLES20.glUniform3f(mBg4DistLoc, bg4.distortion.computeShaderAmplitude(), bg4.distortion.computeShaderFrequency(), bg4.distortion.computeShaderSpeed());
		GLES20.glUniform1f(mBg3CompressionLoc, bg3.distortion.computeShaderCompression());
		GLES20.glUniform1f(mBg4CompressionLoc, bg4.distortion.computeShaderCompression());
		
		// update translation effect variables for the shader program
		
		GLES20.glUniform2f(mBg3Scroll, bg3.translation.getHorizontalOffset(), bg3.translation.getVerticalOffset());
		GLES20.glUniform2f(mBg4Scroll, bg4.translation.getHorizontalOffset(), bg4.translation.getVerticalOffset());
		
		// update palette
		
		GLES20.glUniform4f(mBg3PaletteLoc, (float)bg3.getPaletteCycle1Begin(), (float)bg3.getPaletteCycle1End(), (float)bg3.getPaletteCycle2Begin(), (float)bg3.getPaletteCycle2End());
		GLES20.glUniform4f(mBg4PaletteLoc, (float)bg4.getPaletteCycle1Begin(), (float)bg4.getPaletteCycle1End(), (float)bg4.getPaletteCycle2Begin(), (float)bg4.getPaletteCycle2End());
		GLES20.glUniform1f(mBg3RotationLoc, (float)bg3.getPaletteRotation());
		GLES20.glUniform1f(mBg4RotationLoc, (float)bg4.getPaletteRotation());
		
		// old stuff
		GLES20.glUniform2i(mDistTypeLoc, bg3.distortion.getType(), bg4.distortion.getType());
		GLES20.glUniform2i(mCycleTypeLoc, bg3.getPaletteCycleType(), bg4.getPaletteCycleType());
	}
	
	public void loadBattleBackground(int index)
	{	
		synchronized(lock) {
			currentBackground = index;
			Log.i(Wallpaper.TAG, "Loading battle group " + index);
			battleGroup.load(index);
			
			byte[] dataA = battleGroup.battleBackground.getBg3().getImage();
			byte[] dataB = battleGroup.battleBackground.getBg4().getImage();
			byte[] paletteBg3 = battleGroup.battleBackground.getBg3().getPalette();
			byte[] paletteBg4 = battleGroup.battleBackground.getBg4().getPalette();
			int filter = mFilterBackgrounds ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
			
			//bbg.layerA.distortion.dump(0);
			//bbg.layerA.translation.dump(0);
			
			// http://code.google.com/p/android/issues/detail?id=6641
			mRenderEnemies = sharedPreferences.getBoolean("enableEnemies", false); // set instance variable here once so we're not loading the preference every frame
			boolean enablePaletteEffects = sharedPreferences.getBoolean("enablePaletteEffects", false);
			boolean enableLetterboxing = sharedPreferences.getBoolean("enableLetterboxing", false);
			
			int bufferSize;
			int format;
			if(enablePaletteEffects == true) {
				bufferSize = 256 * 256 * 1;
				format = GLES20.GL_LUMINANCE;
			} else {
				bufferSize = 256 * 256 * 4;
				format = GLES20.GL_RGBA;
			}
			
			if(mTextureA.capacity() != bufferSize) {
				mTextureA = ByteBuffer.allocateDirect(bufferSize);
				mTextureB = ByteBuffer.allocateDirect(bufferSize);
			}
			
			try {
		        mTextureA.put(dataA).position(0);
		        mTextureB.put(dataB).position(0);
			} catch(BufferOverflowException e) {
				
			}
	        
	        mPalette.position(0);
	        mPalette.put(paletteBg3).position(0);
	        
	        mPalette.position(16 * 1 * 4);
	        mPalette.put(paletteBg4).position(0);
	        
	        GLES20.glGenTextures(3, mTextureId, 0);
	        
	        // BG3 background layer
	        
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
	
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, 256, 256, 0, format, GLES20.GL_UNSIGNED_BYTE, mTextureA);
	        
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
	        
	        // BG4 background layer
	
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[1]);
	
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, 256, 256, 0, format, GLES20.GL_UNSIGNED_BYTE, mTextureB);
	
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
	        
	        // palettes
	        
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[2]);
	
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 16, 16, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPalette);
	
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	      
	        // set up output display letterbox values for the shader
	        
	        if(enableLetterboxing == true)
	        {
	        	String letterboxSize = sharedPreferences.getString("letterboxSize", null);
	        	
	        	if(letterboxSize.equals("variable"))
	        	{
	        		// attempt to include the existing extra space as a portion of the background's specified letter box
	        		// mLetterBoxSize = (battleGroup.getLetterBoxPixelSize() - outputPadding / outputScaler);
	        		mLetterBoxSize = battleGroup.getLetterBoxPixelSize();
	        	}
	        	else if(letterboxSize.equals("none")) {
	        		mLetterBoxSize = 0;
	        	} else if(letterboxSize.equals("small")) {
	        		mLetterBoxSize = 48;
	        	} else if(letterboxSize.equals("medium")) {
	        		mLetterBoxSize = 58;
	        	} else if(letterboxSize.equals("large")) {
	        		mLetterBoxSize = 68;
	        	}
	        }
	        else
	        {
	        	mLetterBoxSize = 0;
			}
			
	        /* shader for effects, update program uniforms */
			
			mProgram = shader.getShader(battleGroup.battleBackground, mLetterBoxSize);
			
			mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_position"); // a_position
			mTextureHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord"); // a_texCoord
			mBg3TextureLoc = GLES20.glGetUniformLocation(mProgram, "bg3_texture"); // get sampler locations
			mBg4TextureLoc = GLES20.glGetUniformLocation(mProgram, "bg4_texture"); // get sampler locations
			mPaletteLoc = GLES20.glGetUniformLocation(mProgram, "s_palette");
			
			mResolutionLoc = GLES20.glGetUniformLocation(mProgram, "resolution");
			mBg3DistTypeLoc = GLES20.glGetUniformLocation(mProgram, "bg3_dist_type");
			mBg4DistTypeLoc = GLES20.glGetUniformLocation(mProgram, "bg4_dist_type");
			mBg3DistLoc = GLES20.glGetUniformLocation(mProgram, "bg3_dist");
			mBg4DistLoc = GLES20.glGetUniformLocation(mProgram, "bg4_dist");
			mBg3Scroll = GLES20.glGetUniformLocation(mProgram, "bg3_scroll");
			mBg4Scroll = GLES20.glGetUniformLocation(mProgram, "bg4_scroll");
			mBg3PaletteLoc = GLES20.glGetUniformLocation(mProgram, "bg3_palette");
			mBg4PaletteLoc = GLES20.glGetUniformLocation(mProgram, "bg4_palette");
			mBg3CompressionLoc = GLES20.glGetUniformLocation(mProgram, "bg3_compression");
			mBg3RotationLoc = GLES20.glGetUniformLocation(mProgram, "bg3_rotation");
			mBg4CompressionLoc = GLES20.glGetUniformLocation(mProgram, "bg4_compression");
			mBg4RotationLoc = GLES20.glGetUniformLocation(mProgram, "bg4_rotation");
			
			// old stuff
			mCycleTypeLoc = GLES20.glGetUniformLocation(mProgram, "u_cycle_type");
			mDistTypeLoc = GLES20.glGetUniformLocation(mProgram, "u_dist_type");
			
			
			
			/* enemy loading stuff */
			if(mRenderEnemies)
			{
				byte[] spriteData = battleGroup.enemy.getBattleSprite();
				ByteBuffer sprite = ByteBuffer.allocateDirect(spriteData.length);
				
		        sprite.put(spriteData).position(0);
				
				GLES20.glGenTextures(1, mBattleSpriteId, 0);
				
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBattleSpriteId[0]);
				
		        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, battleGroup.enemy.getBattleSpriteWidth(), battleGroup.enemy.getBattleSpriteHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, sprite);
		        
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
				
		        mBattleSpriteProgramId = shader.getSpriteShader();
		        
		        mEnemyPositionHandle = GLES20.glGetAttribLocation(mBattleSpriteProgramId, "a_position"); // a_position
				mEnemyTextureHandle = GLES20.glGetAttribLocation(mBattleSpriteProgramId, "a_texCoord"); // a_texCoord
				mEnemyTextureLoc = GLES20.glGetUniformLocation(mBattleSpriteProgramId, "s_texture");
				
				float rowOffset = 0.0f;
				if(battleGroup.enemy.getCurrentName().equals("Giygas")) {
					// quick hack to get the one Giygas background aligned properly in lieu of supporting multiple enemies
					// and their rows (for now)
					rowOffset = battleGroup.enemy.getRow() * (1.0f / 224.0f) * 32.0f;
				}
				
				float x = (1.0f / 256.0f) * (battleGroup.enemy.getBattleSpriteWidth());
				float y = (1.0f / 224.0f) * (battleGroup.enemy.getBattleSpriteHeight());

				float quadVertices[] =
				{
					-x,	-y + rowOffset,	 0.0f,
					 x,	-y + rowOffset,	 0.0f,
					-x,	 y + rowOffset,	 0.0f,
					 x,	 y + rowOffset,	 0.0f			 
				};
				
				float textureMap[] =
				{
					0.0f,	 1.0f,
					1.0f,	 1.0f,
					0.0f,	 0.0f,
					1.0f,	 0.0f 
				};
				
				enemyVertexBuffer = ByteBuffer
						.allocateDirect(quadVertices.length * 4) // float is 4 bytes
						.order(ByteOrder.nativeOrder())
						.asFloatBuffer(); 
				enemyVertexBuffer.put(quadVertices);
				enemyVertexBuffer.position(0);
				
				enemyTextureVertexBuffer = ByteBuffer
						.allocateDirect(textureMap.length * 4) // float is 4 bytes
						.order(ByteOrder.nativeOrder())
						.asFloatBuffer(); 
				enemyTextureVertexBuffer.put(textureMap);
				enemyTextureVertexBuffer.position(0);
			}
			
			if(mRenderEnemies) {
				GLES20.glEnable(GLES20.GL_BLEND);
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				GLES20.glDisable(GLES20.GL_BLEND);
			}
			
		}
	}
	
	private void renderScene()
	{
		mRenderWidth = 256.0f;
		mRenderHeight = 256.0f;
		
		GLES20.glViewport(0, 0, 256, 224);	// render to native texture size, scale up later
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]);
		
		/* it may be prudent to check the framebuffer status here before continuing... */
		
		renderBattleBackground();
		if(mRenderEnemies) renderEnemy();
		
		/* now, try to render the texture? */
		
		GLES20.glUseProgram(hFXProgram);
		
		GLES20.glViewport(mSurfaceHorizontalOffset, mSurfaceVerticalOffset, mSurfaceWidth, mSurfaceHeight);		// now we're scaling the framebuffer up to size
		
		hMVPMatrix = GLES20.glGetUniformLocation(hFXProgram, "uMVPMatrix");/* projection and camera */
		
		/* load vertex positions */
		
		GLES20.glVertexAttribPointer(hPosition, 3, GLES20.GL_FLOAT, false, 12, quadVertexBuffer);
		GLES20.glEnableVertexAttribArray(hPosition);
		
		/* load texture mapping */

		GLES20.glVertexAttribPointer(hTexture, 2, GLES20.GL_FLOAT, false, 8, textureOutputBuffer);
		GLES20.glEnableVertexAttribArray(hTexture);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTexture[0]);
		
		GLES20.glUniform1i(hBaseMap, 0);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		GLES20.glUniformMatrix4fv(hMVPMatrix, 1, false, mProjMatrix, 0);	/* projection and camera */
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	private void renderBattleBackground() // "high res" render
	{
		hMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");/* projection and camera */
		
		GLES20.glUseProgram(mProgram);
		
		/* load vertex positions */
		
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, quadVertexBuffer);
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		
		/* load texture mapping */

		GLES20.glVertexAttribPointer(mTextureHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);
		GLES20.glEnableVertexAttribArray(mTextureHandle);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
		GLES20.glUniform1i(mBg3TextureLoc, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[1]);
		GLES20.glUniform1i(mBg4TextureLoc, 1);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[2]);
		GLES20.glUniform1i(mPaletteLoc, 2);
		
		updateShaderVariables(); // be mindful of which active program this applies to!!
		
		/* apply model view projection transformation */
		
		GLES20.glUniformMatrix4fv(hMVPMatrix, 1, false, mProjMatrix, 0);	/* projection and camera */
		
		/* draw the triangles */
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	private void renderEnemy()
	{
		hMVPMatrix = GLES20.glGetUniformLocation(mBattleSpriteProgramId, "uMVPMatrix");/* projection and camera */
		
		GLES20.glUseProgram(mBattleSpriteProgramId);
		
		/* load vertex positions */
		
		GLES20.glVertexAttribPointer(mEnemyPositionHandle, 3, GLES20.GL_FLOAT, false, 12, enemyVertexBuffer);
		GLES20.glEnableVertexAttribArray(mEnemyPositionHandle);
		
		/* load texture mapping */

		GLES20.glVertexAttribPointer(mEnemyTextureHandle, 2, GLES20.GL_FLOAT, false, 8, enemyTextureVertexBuffer);
		GLES20.glEnableVertexAttribArray(mEnemyTextureHandle);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBattleSpriteId[0]);
		GLES20.glUniform1i(mEnemyTextureLoc, 3);
		
		/* apply model view projection transformation */
		
		GLES20.glUniformMatrix4fv(hMVPMatrix, 1, false, mProjMatrix, 0);	/* projection and camera */
		
		/* draw the triangles */
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	private void checkGlError(String op)
	{
		/* from developer.android.com */
		int error;
		while((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}
	
	private int createProgram(String vertexSource, String fragmentSource)
	{
		// courtesy of android.developer.com
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if(vertexShader == 0) {
			return 0;
		}

		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if(pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if(program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if(linkStatus[0] != GLES20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}
	
	private int loadShader(int shaderType, String source)
	{
		
//	int loadShader(int type, String code) {
//		int shader = GLES20.glCreateShader(type);
//		GLES20.glShaderSource(shader, code);
//		GLES20.glCompileShader(shader);
//		return shader;
//	}
		
		int shader = GLES20.glCreateShader(shaderType);
		if(shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}
	
	private String readTextFile(final int resourceId)
	{
		/* method lifted from learnopengles.com */
		final InputStream inputStream = context.getResources().openRawResource(resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
 
		String nextLine;
		final StringBuilder body = new StringBuilder();
		 
		try
		{
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				body.append(nextLine);
				body.append('\n');
			}
		}
		catch (IOException e)
		{
			return null;
		}
 
		return body.toString();
	}
	
	private void queryGl(GL10 gl10)
	{
		//String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		//Log.i("GLInfo", extensions);
		int[] params = new int[64];
		
		GLES20.glGetIntegerv(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, params, 0);
		Log.i("GLInfo", String.format("GLES20.GL_ALIASED_LINE_WIDTH_RANGE: %d - %d", params[0], params[1]));
	}
}
