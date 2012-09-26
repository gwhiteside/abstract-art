package net.georgewhiteside.android.abstractart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.preference.PreferenceManager;
import android.util.Log;

import net.georgewhiteside.android.aapreset.DistortionEffect;
import net.georgewhiteside.android.aapreset.Image;
import net.georgewhiteside.android.aapreset.Layer;
import net.georgewhiteside.android.aapreset.PaletteEffect;
import net.georgewhiteside.android.aapreset.Preset;
import net.georgewhiteside.android.aapreset.Sprite;
import net.georgewhiteside.android.aapreset.TranslationEffect;
import net.georgewhiteside.android.abstractart.RectanglePacker.Rectangle;
import net.georgewhiteside.android.abstractart.Wallpaper;

import org.jf.GLWallpaper.GLWallpaperService;

// float refreshrate = getWindowManager().getDefaultDisplay().getRefreshRate();

// "The PowerVR 530/535 is very slow. Andreno 200 and PowerVR 530/535 are first GPU generation
// (OpenGL ES 2.x) for hdpi resolution. You can't redraw a full screen at 60FPS with a simple texture."

public class Renderer implements GLWallpaperService.Renderer
{
	private static final String TAG = "Renderer";
	private Context context;
	
	private SharedPreferences sharedPreferences;
	
	private ShaderFactory shader;
	
	private FloatBuffer quadVertexBuffer;
	private FloatBuffer textureVertexBuffer;
	private FloatBuffer textureOutputBuffer;
	
	private int mProgram, hFXProgram;
	private int mPositionHandle, hPosition;
	private int mTextureHandle, hTexture;
	private int mBg3TextureLoc;
	private int mBg4TextureLoc;
	private int hBaseMap;
	private int mPaletteLoc;
	
	private int mResolutionLoc;
	private int mBg3DistTypeLoc, mBg4DistTypeLoc;
	private int mBg3DistLoc, mBg4DistLoc;
	private int mBg3Scroll, mBg4Scroll;
	private int mBg3PaletteLoc, mBg4PaletteLoc;
	private int mBg3CompressionLoc, mBg4CompressionLoc;
	private int mBg3RotationLoc, mBg4RotationLoc;
	
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
	
	private int[] mBattleSpriteId;
	private int mBattleSpriteProgramId;
	
	private int currentBackground;
	private boolean persistBackgroundSelection;
	
	Random rand = new Random();
	
	private boolean mirrorVertical = false;
	
	private Object lock = new Object();
	
	boolean enableSmoothScaling = true;
	
	private boolean enablePanning = false;
	private boolean refreshOutput = false;
	private boolean forceReload = false;
	private boolean requestNewBackground = false;
	
	private long currentTime;
	private long previousTime;
	private float deltaTime;
	
	private boolean isPreview = false;
	private boolean isChooserPreviewRenderer = false;
	
	int numSprites = 0;
	
	int fps;
	float renderUpdatePeriod;
	
	public boolean ready = false;
	
	private boolean renderWhenDirty = false;
	
	private static final float MAX_TIMESKIP = 1.0f; // maximum allowed delta time between two frame logic updates
	
	Preset preset;
	Preset nextPreset;
	
	TextureAtlas spriteAtlas = new TextureAtlas();
	
	/*
	public int getRomBackgroundIndex(int address)
	{
		return battleGroup.battleBackground.getRomBackgroundIndex(address);
	}
	*/
	
	public int getCacheableImagesTotal()
	{
		int images = 103; // TODO: don't hardcode this
		
		return images;
	}
	
	/*
	public int getBackgroundsTotal()
	{
		return battleGroup.battleBackground.getNumberOfBackgrounds();
	}
	*/
	
	public void cacheImage(int index)
	{
		// this is, like, deprecated and junk
		// battleGroup.load(index);
		Log.e(TAG, "cacheImage() is deprecated! Pay attention to me!");
	}
	
	/*
	public void setRandomBackground()
	{
		int number = Wallpaper.random.nextInt(battleGroup.battleBackground.getNumberOfBackgrounds() - 1) + 1;
		loadBattleBackground(number);
	}
	*/
	
	public void setPersistBackgroundSelection(boolean value)
	{
		persistBackgroundSelection = value;
	}
	
	public void setIsChooserPreviewRenderer(boolean value) {
		isChooserPreviewRenderer = value;
	}
	
	public void setRenderWhenDirty(boolean value) {
		renderWhenDirty = value;
	}
	
	public Renderer(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		shader = new ShaderFactory(context);
		mTextureA = ByteBuffer.allocateDirect(256 * 256 * 1);
		mTextureB = ByteBuffer.allocateDirect(256 * 256 * 1);
		mPalette = ByteBuffer.allocateDirect(16 * 16 * 4);
		
		currentBackground = -1;
		persistBackgroundSelection = false;
		
		refreshOutput();
		
		//Log.i(TAG, "Renderer created");
	}
	
	public Renderer(Context context, boolean mirrorVertical)
	{
		this(context);
		this.mirrorVertical = mirrorVertical;
	}
	
	public void setBackground(int index) {
		currentBackground = index;
	}
	
	public void requestNewBackground(boolean value) {
		requestNewBackground = value;
	}
	
	public void refreshOutput() {
		refreshOutput = true;
	}
	
	public void setOffsets(float xOffset, float yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}
	
	float xOffset;
	float yOffset;
	
	public void setEnablePanning(boolean value) {
		enablePanning = value;
	}
	
	public void setIsPreview(boolean value) {
		isPreview = value;
	}
	
	public void queueImmediate(Preset preset) {
		nextPreset = preset;
		requestNewBackground = true;
	}
	
	public void onDrawFrame(GL10 unused)
	{
		if(refreshOutput == true && ready) {
			int fps = Integer.valueOf(sharedPreferences.getString("framerateCap", null));
			renderUpdatePeriod = 1.0f / fps;
			
			forceReload = true;
			onSurfaceCreated(null, null);
			refreshOutput = false;
		}
		
		if(requestNewBackground == true && ready) {
			requestNewBackground = false;
			preset = nextPreset;
			nextPreset = null;
			//loadBattleBackground(preset);
			Wallpaper.setNewBackground(this);
		}
		
		currentTime = System.nanoTime();
		deltaTime = (currentTime - previousTime) / 1000000000.0f;
		previousTime = currentTime;
		
		if(deltaTime > MAX_TIMESKIP) {
			// if the time between two frames drops too low (99.999% of the time because of the screen
			// being off or the wallpaper being hidden) set it to some defined maximum value so the output
			// doesn't appear all wonky for certain backgrounds with changing effect patterns
			deltaTime = MAX_TIMESKIP;
		}
		
		/*
		// as long as it's within 2ms, just let it go
		if(deltaTime + 0.002 < renderUpdatePeriod) {
			long beginAdjust = System.nanoTime();
			try {
				Thread.sleep((long)((renderUpdatePeriod - deltaTime) * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			float totalAdjust = (System.nanoTime() - beginAdjust) / 1000000000.0f;
			//Log.i(TAG, "deltaTime less than update period - " + deltaTime * 1000 + "ms; post-adjustment: " + (deltaTime + totalAdjust) * 1000 + "ms");
			
			deltaTime += totalAdjust;
		} 
		*/
		
		//Log.i(TAG, "deltaTime: " + deltaTime * 1000 + "ms");
		
		if(!renderWhenDirty) {
			preset.update(deltaTime);
		}
		
		renderScene();
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
		
	    float outputScaler = 1;
	    
	    if(surfaceRatio == textureRatio) { // thumbnail hack (won't scale if someone actually uses an 8:7 screen ratio)
	    	outputScaler = 1; 
	    } else if(surfaceRatio > textureRatio) { // landscape
	    	if(enableSmoothScaling) {
	    		outputScaler = mSurfaceWidth / 256.0f;
	    	} else {
	    		outputScaler = (int)(mSurfaceWidth / 256) + 1;
	    	}
	    } else if(surfaceRatio < textureRatio) { // portrait
	    	if(enableSmoothScaling) {
	    		outputScaler = mSurfaceHeight / 224.0f;
	    	} else {
	    		outputScaler = (int)(mSurfaceHeight / 224) + 1;
	    	}
	    }
	    
	    int bestWidthFit = (int) (outputScaler * 256);
        int bestHeightFit = (int) (outputScaler * 224);
        
        mSurfaceWidth = bestWidthFit;
        mSurfaceHeight = bestHeightFit;
        mSurfaceVerticalOffset = (height - bestHeightFit) / 2;
        mSurfaceHorizontalOffset = (width - bestWidthFit) / 2;
		
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
		String outputScaling = sharedPreferences.getString("outputScaling", null);
		
		if(outputScaling.equals("smooth")) {
			enableSmoothScaling = true;
		} else if(outputScaling.equals("sharp")) {
			enableSmoothScaling = false;
		}
		
		if(enableSmoothScaling) {
			mFilterOutput = true;
		} else {
			mFilterOutput = false;
		}
		
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
		
		/*if(persistBackgroundSelection && currentBackground >= 0 && currentBackground < getBackgroundsTotal())
		{
			if(!Wallpaper.backgroundListIsDirty || isChooserPreviewRenderer) {
				loadBattleBackground(currentBackground);
			} else {
				Wallpaper.setNewBackground(this);
			}
		}
		else*/
		{
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
	}

	private void updateShaderVariables()
	{
		// glUniform* calls always act on the current program that is bound with glUseProgram
		
		int numLayers = preset.getLayers().size();
		
		Layer bg3 = null;
		Layer bg4 = null;
		
		switch(numLayers) {
		case 2: bg4 = preset.getLayers().get(1); // FALLTHROUGH
		case 1: bg3 = preset.getLayers().get(0);
			break;
		}
		
		// update shader resolution
		
		GLES20.glUniform2f(mResolutionLoc, mRenderWidth, mRenderHeight);
		
		// update shader variables for distortion, translation, and palette effects
		
		if(bg3 != null) {
			DistortionEffect bg3Dist = bg3.getDistortionEffect();
			if(bg3Dist != null) {
				GLES20.glUniform1i(mBg3DistTypeLoc, bg3Dist.getType());
				GLES20.glUniform3f(mBg3DistLoc, bg3Dist.runningAmplitude(), bg3Dist.runningFrequency(), bg3Dist.runningSpeed());
				GLES20.glUniform1f(mBg3CompressionLoc, bg3Dist.runningCompression());
			}
			
			TranslationEffect bg3Trans = bg3.getTranslationEffect();
			if(bg3Trans != null) {
				GLES20.glUniform2f(mBg3Scroll, bg3Trans.currentHorizontalOffset(), bg3Trans.currentVerticalOffset());
			}
			
			PaletteEffect bg3Pal = bg3.getPaletteEffect();
			if(bg3Pal != null) {
				GLES20.glUniform4f(mBg3PaletteLoc, (float)bg3Pal.getCycle1Begin(), (float)bg3Pal.getCycle1End(), (float)bg3Pal.getCycle2Begin(), (float)bg3Pal.getCycle2End());
				GLES20.glUniform1f(mBg3RotationLoc, (float)bg3Pal.getRotation());
			}
		}
		
		if(bg4 != null) {
			DistortionEffect bg4Dist = bg4.getDistortionEffect();
			if(bg4Dist != null) {
				GLES20.glUniform1i(mBg4DistTypeLoc, bg4Dist.getType());
				GLES20.glUniform3f(mBg4DistLoc, bg4Dist.runningAmplitude(), bg4Dist.runningFrequency(), bg4Dist.runningSpeed());
				GLES20.glUniform1f(mBg4CompressionLoc, bg4Dist.runningCompression());
			}
			
			TranslationEffect bg4Trans = bg4.getTranslationEffect();
			if(bg4Trans != null) {
				GLES20.glUniform2f(mBg4Scroll, bg4Trans.currentHorizontalOffset(), bg4Trans.currentVerticalOffset());
			}
			
			PaletteEffect bg4Pal = bg4.getPaletteEffect();
			if(bg4Pal != null) {
				GLES20.glUniform4f(mBg4PaletteLoc, (float)bg4Pal.getCycle1Begin(), (float)bg4Pal.getCycle1End(), (float)bg4Pal.getCycle2Begin(), (float)bg4Pal.getCycle2End());
				GLES20.glUniform1f(mBg4RotationLoc, (float)bg4Pal.getRotation());
			}
		}
	}
	
	public void loadBattleBackground(Preset preset)
	{	
		synchronized(lock) {
			this.preset = preset;
			//currentBackground = index;
			forceReload = false;
			
			Log.d("debug", "Preset name: " + preset.getTitle());
			
			List<Layer> layers = preset.getLayers();
			
			byte[] dataA;
			byte[] dataB;
			byte[] paletteBg3;
			byte[] paletteBg4;
			
			if(layers.size() == 2) {
				dataA = layers.get(0).getImage().getIndexedBytes();
				dataB = layers.get(1).getImage().getIndexedBytes();
				paletteBg3 = layers.get(0).getImage().getPalette().getBytes();
				paletteBg4 = layers.get(1).getImage().getPalette().getBytes();
			} else if(layers.size() == 1) {
				dataA = layers.get(0).getImage().getIndexedBytes();
				dataB = new byte[256 * 256];
				paletteBg3 = layers.get(0).getImage().getPalette().getBytes();
				paletteBg4 = new byte[16 * 1 * 4];
			} else {
				dataA = new byte[256 * 256];
				dataB = new byte[256 * 256];
				paletteBg3 = new byte[16 * 1 * 4];
				paletteBg4 = new byte[16 * 1 * 4];
			}
			
			int filter = mFilterBackgrounds ? GLES20.GL_LINEAR : GLES20.GL_NEAREST;
			
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
	        		mLetterBoxSize = preset.getLetterbox();
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
			
			mProgram = shader.getShader(preset, mLetterBoxSize);
			
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
			
			// enemy loading stuff 
			
			if(mRenderEnemies)
			{
				List<Sprite> uniqueSprites = preset.getSprites();
				
				// add all the sprites, including duplicates, to a new list
				List<Sprite> sprites = new ArrayList<Sprite>();
				for(Sprite sprite : uniqueSprites) {
					for(int i = 0; i < sprite.getAmount(); i++) {
						sprites.add(sprite);
					}
				}
				
				numSprites = sprites.size();
				
				// create a texture atlas for the (unique) sprite image(s)
				List<Image> images = new ArrayList<Image>();
				for(Sprite sprite : uniqueSprites) {
					images.add(sprite.getImage());
				}
				spriteAtlas.generateAtlas(images);
				
				enemyVertexBuffer = ByteBuffer
						.allocateDirect(numSprites * 3 * 6 * 4) // numSprites * coordinates (3) * vertices (6) * float (4 bytes)
						.order(ByteOrder.nativeOrder())
						.asFloatBuffer(); 
				
				enemyTextureVertexBuffer = ByteBuffer
						.allocateDirect(numSprites * 2 * 6 * 4) // numSprites * coordinates (2) * vertices (6) * float (4 bytes)
						.order(ByteOrder.nativeOrder())
						.asFloatBuffer();
				
				byte[] spriteData = spriteAtlas.getAtlas();
				ByteBuffer texture = ByteBuffer.allocateDirect(spriteData.length);
				texture.put(spriteData).position(0);
				
				mBattleSpriteId = new int[1];
				
				GLES20.glGenTextures(1, mBattleSpriteId, 0);
				
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBattleSpriteId[0]);
				
		        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, spriteAtlas.getWidth(), spriteAtlas.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texture);
		        
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		        
		        mBattleSpriteProgramId = shader.getSpriteShader();
		        
		        mEnemyPositionHandle = GLES20.glGetAttribLocation(mBattleSpriteProgramId, "a_position"); // a_position
				mEnemyTextureHandle = GLES20.glGetAttribLocation(mBattleSpriteProgramId, "a_texCoord"); // a_texCoord
				mEnemyTextureLoc = GLES20.glGetUniformLocation(mBattleSpriteProgramId, "s_texture");
				
				
				
				// calculate total width
				int rowWidth = 0;
				for(Sprite sprite : sprites) {
					rowWidth += sprite.getWidth();
				}
				rowWidth += 8 * (sprites.size() - 1); // 8px space between sprites
				// GL surface width runs -1 .. +1
				float xOffset = 0 - (1.0f / 256.0f) * rowWidth;
				
				Log.d("debug", "sprites: ");
				
				for(int i = 0; i < sprites.size(); i++) {
					Sprite sprite = sprites.get(i);
					
					float yOffset = -1.0f + (1.0f / 224.0f) * 80 * 2 + (1.0f / 224.0f) * sprite.getRow() * 16 * 2; // row 0 == front; row 1 == back (back is 16px above front)
					
					/*if(battleGroup.enemy.getCurrentName().equals("Giygas")) {
						// quick hack to get the one Giygas background aligned properly in lieu of supporting multiple enemies
						// and their rows (for now)
						rowOffset = battleGroup.enemy.getRow() * (1.0f / 224.0f) * 32.0f;
					}*/
					
					Rectangle rect = spriteAtlas.packer.findRectangle(sprite.getTextureId());
					
					int width = rect.width;
					int height = rect.height;
					
					float x = (1.0f / 256.0f) * (width) * 2;
					float y = (1.0f / 224.0f) * (height) * 2;
					
					float quadVertices[] =
					{
						// triangle 0
						xOffset,		yOffset,	 0.0f,	// 0
						xOffset + x,	yOffset,	 0.0f,	// 1
						xOffset,		yOffset + y,	 0.0f,	// 2
						
						// triangle 1
						xOffset,		yOffset + y,	 0.0f,	// 2
						xOffset + x,	yOffset,	 0.0f,	// 1
						xOffset + x,	yOffset + y,	 0.0f,	// 3
					};
					
					xOffset += (1.0f / 256.0f) * 8 * 2 + x;
					
					float px = 1.0f / spriteAtlas.getWidth();
					float py = 1.0f / spriteAtlas.getHeight();
					
					
					
					float spriteX = px * rect.x;
					float spriteY = py * rect.y;
					float spriteWidth = px * rect.width;
					float spriteHeight = py * rect.height;
					
					float textureMap[] =
					{
							
						// triangle 0
						spriteX,	spriteY + spriteHeight,
						spriteX + spriteWidth,	spriteY + spriteHeight,
						spriteX,	spriteY,
						
						// triangle 1
						spriteX,	spriteY,
						spriteX + spriteWidth,	spriteY + spriteHeight,
						spriteX + spriteWidth,	spriteY
							
						
					/*
						// triangle 0
						0.0f,	1.0f,
						1.0f,	1.0f,
						0.0f,	0.0f,
						
						// triangle 1
						0.0f,	0.0f,
						1.0f,	1.0f,
						1.0f,	0.0f
					*/
					};
					
					
					enemyVertexBuffer.put(quadVertices);
					enemyTextureVertexBuffer.put(textureMap);
				}
				
				enemyVertexBuffer.position(0);
				enemyTextureVertexBuffer.position(0);
				
				GLES20.glEnable(GLES20.GL_BLEND);
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				GLES20.glDisable(GLES20.GL_BLEND);
			}
			
			/*
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
			*/
			
			ready = true;
		}
	}
	
	private void renderScene()
	{
		mRenderWidth = 256.0f;
		mRenderHeight = 256.0f;
		
		GLES20.glViewport(0, 0, 256, 224);	// render to native texture size, scale up later
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]);
		
		renderBattleBackground();
		
		if(mRenderEnemies && numSprites > 0) {
			renderEnemy();
		}
		
		/* now, try to render the texture? */
		
		GLES20.glUseProgram(hFXProgram);
		
		int horizontalOffset = enablePanning && !isPreview ? (int) (xOffset * mSurfaceHorizontalOffset * 2) : mSurfaceHorizontalOffset;
		
		GLES20.glViewport(horizontalOffset, mSurfaceVerticalOffset, mSurfaceWidth, mSurfaceHeight);		// now we're scaling the framebuffer up to size
		
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
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6 * numSprites);
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
	
	/*
	private void queryGl(GL10 gl10)
	{
		//String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		//Log.i("GLInfo", extensions);
		int[] params = new int[64];
		
		GLES20.glGetIntegerv(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, params, 0);
		Log.i("GLInfo", String.format("GLES20.GL_ALIASED_LINE_WIDTH_RANGE: %d - %d", params[0], params[1]));
	}
	*/
}
