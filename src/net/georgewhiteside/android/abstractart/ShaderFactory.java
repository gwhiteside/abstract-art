package net.georgewhiteside.android.abstractart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.preference.PreferenceManager;
import android.util.Log;

//import com.badlogic.gdx.backends.android.AndroidGL20;

// TODO: cache programs in a hashmap or something

/**
 * Constructs the smallest shader possible for the given <code>BattleBackground</code>. This is a terrible mess, but it's
 * a necessary sacrifice to get the best performance out of the mobile platform. (There are quiet whisperings about the
 * mobile shader compilers sucking.)
 * @author George
 *
 */
public class ShaderFactory
{
	private Context context;
	private static final String TAG = "shader";
	
	private SharedPreferences sharedPreferences;
	
	private boolean knobMonolithic = false;	// this option is kept for development reasons
	
	private static final String[] bgPrefix = {"bg3_", "bg4_"};
	
	private String spriteVertexShader =
		"uniform mat4 uMVPMatrix;\n" +
		"attribute vec4 a_position;\n" +
		"attribute vec2 a_texCoord;\n" +
		"varying vec2 v_texCoord;\n" +
		"void main() {\n" +
		"    gl_Position = uMVPMatrix * a_position;\n" +
		"    v_texCoord = a_texCoord;\n" +
		"}\n";
	
	private String spriteFragmentShader =
		"precision mediump float;\n" +
		"varying vec2 v_texCoord;\n" +
		"uniform sampler2D s_texture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
		"    vec4 color = texture2D(s_texture, v_texCoord);\n" +
		"    gl_FragColor = color;\n" +
		"}\n";

	private String vertexShader =
			"uniform mat4 uMVPMatrix;\n" +
			"attribute vec4 a_position;\n" +
			"attribute vec2 a_texCoord;\n" +
			"varying vec2 v_texCoord;\n" +
			"void main() {\n" +
			"    gl_Position = uMVPMatrix * a_position;\n" +
			"    v_texCoord = a_texCoord;\n" +
			"}\n";
	
	private String fragmentHeader =
		"precision highp float;\n" +

		"varying vec2 v_texCoord;\n" +
		"uniform sampler2D bg3_texture;\n" +
		"uniform sampler2D bg4_texture;\n" +
		"uniform sampler2D s_palette;\n" +
	
		"uniform vec2 resolution;\n" +
	
		"uniform int bg3_dist_type;\n" +
		"uniform vec3 bg3_dist;\n" +
		"uniform vec4 bg3_palette;\n" +
		"uniform vec2 bg3_scroll;\n" +
		"uniform float bg3_compression;\n" +
		"uniform float bg3_rotation;\n" +
	
		"uniform int bg4_dist_type;\n" +
		"uniform vec3 bg4_dist;\n" +
		"uniform vec4 bg4_palette;\n" +
		"uniform vec2 bg4_scroll;\n" +
		"uniform float bg4_compression;\n" +
		"uniform float bg4_rotation;\n" +
	
		"#define AMPL   0\n" +
		"#define FREQ   1\n" +
		"#define SPEED  2\n" +
	
		"#define BEGIN1 0\n" +
		"#define END1   1\n" +
		"#define BEGIN2 2\n" +
		"#define END2   3\n";
	
	public ShaderFactory(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	/**
	 * WARNING: THIS IS MOSTLY HERE AS A REMINDER.
	 * 
	 * <p>Inconsistencies between mobile chipsets make this difficult-to-impossible
	 * to realize, depending on how exactly you try to do it. Saving the compiled
	 * shader binaries isn't universally supported, so that's out. Simply caching the
	 * program reference handles *might* work, but I don't know enough about how long
	 * programs are kept in RAM, or how many are, or if more than one is kept, and knowing
	 * exactly when a program IS cleared (from basic testing on my phone it seems to be
	 * when the GL context is destroyed, but that's only a tiny piece of the larger
	 * puzzle), and whether these behaviors vary across chipsets, and so on. Way too
	 * many unknowns here.
	 * 
	 * <p>Returns a value guaranteed to uniquely identify the shader's functions. This
	 * should be used to cache linked shader program IDs, preventing unnecessary
	 * compiling and linking on every background change for a given GL instance.
	 * 
	 * @return a collision-free hash value identifying the shader functions
	 */
	public int getShaderSignature(BattleBackground bbg)
	{
		/*
		 * The value is determined by checking which effects are used and setting
		 * corresponding bit fields or subfields in an integer. The exact method doesn't
		 * matter, so long as every possible combination of shader settings generates
		 * a unique value. It doesn't even need to generate the same value for a given
		 * configuration between application updates.
		 */
		return 0;
	}
	
	public int getShader(BattleBackground bbg, float letterBoxSize)
	{
		String fragmentShader = "";
		boolean enablePaletteEffects = sharedPreferences.getBoolean("enablePaletteEffects", false); // SharedPreference
		
		if(knobMonolithic)
		{
			fragmentShader = readTextFile(R.raw.distortion_frag);
		}
		else
		{
			// TODO: build this shader with a StringBuilder instead
		
			fragmentShader += fragmentHeader;
			
			fragmentShader +=
				"void main()\n" +
				"{\n" +
				"    float y = v_texCoord.y * 256.0;\n" + 
				"    if(y < " + letterBoxSize + " || y > 224.0 - " + letterBoxSize + ") { gl_FragColor.rgba = vec4(0.0, 0.0, 0.0, 1.0); } else {";
			
			// iterate over both layers and construct the smallest shader possible
			
			for(int i = 0; i < 2; i++)
			{
				Layer layer = i == 0 ? bbg.bg3 : bbg.bg4;
				
				// we always want the bottom layer, but skip the top layer if it's null
				if(layer == bbg.bg3 || layer == bbg.bg4 && layer.getIndex() != 0)
				{
					String id = bgPrefix[i];
					fragmentShader += "vec2 " + id + "offset = vec2(0.0);\n";
					
					// distortion
					
					// TODO: probe battle background layer to determine if multiple distortions are used, and which ones; if more than one, support them
					
					int numberOfEffects = layer.distortion.getNumberOfEffects();
					//numberOfEffects = 0;
					
					if(numberOfEffects != 0)
					{
						fragmentShader += "float " + id + "distortion_offset = (" + id + "dist[AMPL] * sin(" + id + "dist[FREQ] * y + " + id + "dist[SPEED]));\n";
						
						if(numberOfEffects == 1)
						{
							switch(layer.distortion.getType())
							{
								default:
									break;
									
								case 1:
									fragmentShader += id + "offset.x = " + id + "distortion_offset;\n";
									break;
									
								case 2:
									fragmentShader += id + "offset.x = floor(mod(y, 2.0)) == 0.0 ? " + id + "distortion_offset : -" + id + "distortion_offset;\n";
									break;
									
								case 3:
									fragmentShader += id + "offset.y = mod(" + id + "distortion_offset, resolution.y);\n";
									break;
									
								case 4:
									fragmentShader +=	id + "offset.x = floor(mod(y, 2.0)) == 0.0 ? " + id + "distortion_offset : -" + id + "distortion_offset;\n" +
														id + "offset.x += (y * (" + id + "compression / resolution.y));\n";
									break;
							}
						}
						else // 2 or more effects are used
						{
							fragmentShader +=	"if(" + id + "dist_type == 1) {\n" +
													id + "offset.x = " + id + "distortion_offset;\n" +
									
												"} else if(" + id + "dist_type == 2) {\n" +
													id + "offset.x = floor(mod(y, 2.0)) == 0.0 ? " + id + "distortion_offset : -" + id + "distortion_offset;\n" +
												
												"} else if(" + id + "dist_type == 3) {\n" +
													id + "offset.y = mod(" + id + "distortion_offset, resolution.y);\n" +
												"}\n" +
												
												"if(" + id + "dist_type == 4) {\n" +
													id + "offset.x = floor(mod(y, 2.0)) == 0.0 ? " + id + "distortion_offset : -" + id + "distortion_offset;\n" +
													id + "offset.x += (y * (" + id + "compression / resolution.y));\n" +
												"}\n";
						}
					}

					
					// vertical compression
					
					if(layer.distortion.getType() != 4 && layer.distortion.getCompression() != 0 && layer.distortion.getCompressionDelta() != 0)
					{
						fragmentShader += id + "offset.y += (y * (" + id + "compression / resolution.y));\n";
					}
					
					// layer scrolling
					
					if(	layer.translation.getHorizontalAcceleration() != 0 || layer.translation.getHorizontalVelocity() != 0 ||
						layer.translation.getVerticalAcceleration() != 0 || layer.translation.getVerticalVelocity() != 0 )
					{
						fragmentShader += id + "offset += bg3_scroll;\n";
					}
					
					// divide offset down to correct range
					
					fragmentShader += id + "offset /= resolution;\n";
					
					if(enablePaletteEffects == true) {
						
						// get palette index
						
						fragmentShader += "float " + id + "index = texture2D(" + id + "texture, " + id + "offset + v_texCoord).r;\n";
						fragmentShader += id + "index *= 256.0;\n";
						
						// make sure index is proper (probably not necesary, but I'm paranoid around all this float math)
						
						//fragmentShader += id + "index = floor(" + id + "index + 0.5);\n";
						
						// add palette cycling code if required
					
						switch(layer.getPaletteCycleType())
						{
							default:
								// no palette cycling
								break;
								
							case 1:
								// rotate palette subrange left
								fragmentShader +=
									"if(" + id + "index >= " + id + "palette[BEGIN1] - 0.5 && " + id + "index <= " + id + "palette[END1] + 0.5)\n" +
									"{\n" +
									"    float range = " + id + "palette[END1] - " + id + "palette[BEGIN1];\n" +
									"    " + id + "index = " + id + "index - " + id + "rotation;\n" +
									"    if(" + id + "index < " + id + "palette[BEGIN1]) {\n" +
									"        " + id + "index = " + id + "palette[END1] + 1.0 - abs(" + id + "palette[BEGIN1] - " + id + "index);\n" +
									"    }\n" +
									"}\n";
								break;
							
							case 2:
								// rotate two palette subranges left
								fragmentShader +=
									"if(" + id + "index >= " + id + "palette[BEGIN1] - 0.5 && " + id + "index <= " + id + "palette[END1] + 0.5)\n" +
									"{\n" +
									"    float range = " + id + "palette[END1] - " + id + "palette[BEGIN1];\n" +
									"    " + id + "index = " + id + "index - " + id + "rotation;\n" +
									"    if(" + id + "index < " + id + "palette[BEGIN1]) {\n" +
									"        " + id + "index = " + id + "palette[END1] + 1.0 - abs(" + id + "palette[BEGIN1] - " + id + "index);\n" +
									"    }\n" +
									"}\n" +
									"else if(" + id + "index >= " + id + "palette[BEGIN2] - 0.5 && " + id + "index <= " + id + "palette[END2] + 0.5)\n" +
									"{\n" +
									"    float range = " + id + "palette[END2] - " + id + "palette[BEGIN2];\n" +
									"    " + id + "index = " + id + "index - " + id + "rotation;\n" +
									"    if(" + id + "index < " + id + "palette[BEGIN2]) {\n" +
									"        " + id + "index = " + id + "palette[END2] + 1.0 - abs(" + id + "palette[BEGIN2] - " + id + "index);\n" +
									"    }\n" +
									"}\n";
								break;
							
							case 3:
								// "mirror rotate" palette subrange left (indices cycle like a triangle waveform)
								fragmentShader +=
									"if(" + id + "index >= " + id + "palette[BEGIN1] - 0.5 && " + id + "index <= " + id + "palette[END1] + 0.5)\n" +
									"{\n" +
									"    float range = " + id + "palette[END1] - " + id + "palette[BEGIN1];\n" +
									"    " + id + "index = " + id + "index + " + id + "rotation - " + id + "palette[BEGIN1];\n" +
									"    range = floor(range + 0.5);\n" +
									"    " + id + "index = floor(" + id + "index + 0.5);\n" +
									"    if(" + id + "index > range * 2.0 + 1.0) {\n" +
									"        " + id + "index = " + id + "palette[BEGIN1] + (" + id + "index - ((range * 2.0) + 2.0));\n" +
									"    }\n" +
									"    else if(" + id + "index > range) {\n" +
									"        " + id + "index = " + id + "palette[END1] - (" + id + "index - (range + 1.0));\n" +
									"    }\n" +
									"    else {\n" +
									"        " + id + "index += " + id + "palette[BEGIN1];\n" +
									"    }\n" +
									"}\n";
								break;
						}
					
						// divide color index down into texture lookup range
						
						fragmentShader += id + "index /= 16.0;\n";
						
						// actual palette color lookup
						
						float paletteRow = layer == bbg.bg3 ? 0.0f : 1.0f;
						fragmentShader += "vec4 " + id + "color = texture2D(s_palette, vec2(" + id + "index, " + paletteRow + " / 16.0));\n";
					} else {
						fragmentShader += "vec4 " + id + "color = texture2D(" + id + "texture, " + id + "offset + v_texCoord);\n";
					}
					
				}
			}
			
			// output blending
			
			if(bbg.bg4.getIndex() != 0)
			{
				// both layers are active; perform an alpha blend with BG4 at 50% opacity
				fragmentShader += 
					"bg4_color.a *= 0.5;\n" +
					"gl_FragColor.rgb = bg4_color.rgb * bg4_color.a + bg3_color.rgb * (1.0 - bg4_color.a);\n" +
					"gl_FragColor.a = 1.0;\n";
			}
			else
			{
				fragmentShader +=
					"gl_FragColor.rgb = bg3_color.rgb;\n" +
					"gl_FragColor.a = 1.0;\n";
			}
		
			// ...aaand the final curly brace:
			
			fragmentShader += "}}\n";
		}
		
		//Log.d("shader", vertexShader);
		//Log.d("shader", fragmentShader);
		
		int result = createProgram(vertexShader, fragmentShader);
		if(result == 0) { throw new RuntimeException("[...] shader compilation failed"); }
		return result;
	}
	
	public int getSpriteShader()
	{
		int result = createProgram(spriteVertexShader, spriteFragmentShader);
		if(result == 0) { throw new RuntimeException("[...] shader compilation failed"); }
		return result;
	}
	
	private int createProgram(String vertexSource, String fragmentSource)
	{
		// courtesy of android.developer.com
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if(vertexShader == 0)
		{
			return 0;
		}

		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if(pixelShader == 0)
		{
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if(program != 0)
		{
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if(linkStatus[0] != GLES20.GL_TRUE)
			{
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		//Log.i(TAG, "Program link status: " + GLES20.glGetProgramInfoLog(program));
		//Log.i(TAG, "shader program handle: " + program);
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
		if(shader != 0)
		{
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0 && shaderType != GLES20.GL_VERTEX_SHADER)
			{
				Log.e(TAG, "Could not compile shader " + shaderType + ":");

				/*
				 * Apparently, glGetShaderInfoLog is actually broken, which is... great. The next line does nothing.
				 * see http://stackoverflow.com/questions/4588800/glgetshaderinfolog-returns-empty-string-android
				 */

				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		} else {
			Log.e(TAG, "glCreateShader() failed; no opengl context");
		}
		
		/*String type = "";
		switch(shaderType)
		{
		case GLES20.GL_FRAGMENT_SHADER:
			type = "Fragment";
			break;
		case GLES20.GL_VERTEX_SHADER:
			type = "Vertex";
			break;
		}
		Log.i(TAG, type + " shader compile status: " + GLES20.glGetShaderInfoLog(shader));*/
		return shader;
	}
	
	private void checkGlError(String op)
	{
		/* from developer.android.com */
		int error;
		while((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
		{
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
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
			e.printStackTrace();
			return null;
		}
		
		return body.toString();
	}
	
}

