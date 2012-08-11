package net.georgewhiteside.android.abstractart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.preference.PreferenceManager;
import android.util.Log;

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
	
	private static String spriteVertexShader =
		"uniform mat4 uMVPMatrix;\n" +
		"attribute vec4 a_position;\n" +
		"attribute vec2 a_texCoord;\n" +
		"varying vec2 v_texCoord;\n" +
		"void main() {\n" +
		"    gl_Position = uMVPMatrix * a_position;\n" +
		"    v_texCoord = a_texCoord;\n" +
		"}\n";
	
	private static String spriteFragmentShader =
		"precision mediump float;\n" +
		"varying vec2 v_texCoord;\n" +
		"uniform sampler2D s_texture;\n" +
		"\n" +
		"void main()\n" +
		"{\n" +
		"    vec4 color = texture2D(s_texture, v_texCoord);\n" +
		"    gl_FragColor = color;\n" +
		"}\n";

	private static String vertexShader =
			"uniform mat4 uMVPMatrix;\n" +
			"attribute vec4 a_position;\n" +
			"attribute vec2 a_texCoord;\n" +
			"varying vec2 v_texCoord;\n" +
			"void main() {\n" +
			"    gl_Position = uMVPMatrix * a_position;\n" +
			"    v_texCoord = a_texCoord;\n" +
			"}\n";
	
	private static String fragmentHeader =
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
	
	private static Map<String, String> map = new HashMap<String, String>()
	{{
		put("$BG_TEXTURE", "texture");
		put("$BG_OFFSET", "offset");
		put("$BG_OFFSET_X", "offset.x");
		put("$BG_OFFSET_Y", "offset.y");
		put("$BG_DIST_TYPE", "dist_type");
		put("$BG_DISTORTION_OFFSET", "distortion_offset");
		put("$BG_DIST_AMPL", "dist[AMPL]");
		put("$BG_DIST_FREQ", "dist[FREQ]");
		put("$BG_DIST_SPEED", "dist[SPEED]");
		put("$BG_COMPRESSION", "compression");
		put("$BG_INDEX", "index");
		put("$BG_SCROLL", "scroll");
		put("$BG_COLOR", "color");
		put("$BG_PALETTE_BEGIN1", "palette[BEGIN1]");
		put("$BG_PALETTE_END1", "palette[END1]");
		put("$BG_PALETTE_BEGIN2", "palette[BEGIN2]");
		put("$BG_PALETTE_END2", "palette[END2]");
		put("$BG_ROTATION", "rotation");
	}};
	
	public ShaderFactory(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
				Layer layer = null;
				String prefix = null;
				
				if(i == 0) {
					layer = bbg.bg3;
					prefix = "bg3_";
				}
				if (i == 1) {
					layer = bbg.bg4;
					prefix = "bg4_";
				}
				
				// we always want the bottom layer, but skip the top layer if it's null
				if(layer == bbg.bg3 || layer == bbg.bg4 && layer.getIndex() != 0)
				{
					fragmentShader += "vec2 $BG_OFFSET = vec2(0.0);\n";
					
					// distortion
					
					// TODO: probe battle background layer to determine if multiple distortions are used, and which ones; if more than one, support them
					
					int numberOfEffects = layer.distortion.getNumberOfEffects();
					//numberOfEffects = 0;
					
					if(numberOfEffects != 0)
					{
						fragmentShader += "float $BG_DISTORTION_OFFSET = ($BG_DIST_AMPL * sin($BG_DIST_FREQ * y + $BG_DIST_SPEED));\n";
						
						if(numberOfEffects == 1)
						{
							switch(layer.distortion.getType())
							{
								default:
									break;
									
								case 1:
									fragmentShader += "$BG_OFFSET_X = $BG_DISTORTION_OFFSET;\n";
									break;
									
								case 2:
									fragmentShader += "$BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n";
									break;
									
								case 3:
									fragmentShader += "$BG_OFFSET_Y = mod($BG_DISTORTION_OFFSET, resolution.y);\n";
									break;
									
								case 4:
									fragmentShader += "$BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
													"$BG_OFFSET_X += (y * ($BG_COMPRESSION / resolution.y));\n";
									break;
							}
						}
						else // 2 or more effects are used
						{
							fragmentShader +=	"if($BG_DIST_TYPE == 1) {\n" +
													"$BG_OFFSET_X = $BG_DISTORTION_OFFSET;\n" +
									
												"} else if($BG_DIST_TYPE == 2) {\n" +
													"$BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
												
												"} else if($BG_DIST_TYPE == 3) {\n" +
													"$BG_OFFSET_Y = mod($BG_DISTORTION_OFFSET, resolution.y);\n" +
												"}\n" +
												
												"if($BG_DIST_TYPE == 4) {\n" +
													"$BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
													"$BG_OFFSET_X += (y * ($BG_COMPRESSION / resolution.y));\n" +
												"}\n";
						}
					}

					
					// vertical compression
					
					if(layer.distortion.getType() != 4 && layer.distortion.getCompression() != 0 && layer.distortion.getCompressionDelta() != 0)
					{
						fragmentShader += "$BG_OFFSET_Y += (y * ($BG_COMPRESSION / resolution.y));\n";
					}
					
					// layer scrolling
					
					if(	layer.translation.getHorizontalAcceleration() != 0 || layer.translation.getHorizontalVelocity() != 0 ||
						layer.translation.getVerticalAcceleration() != 0 || layer.translation.getVerticalVelocity() != 0 )
					{
						fragmentShader += "$BG_OFFSET += $BG_SCROLL;\n";
					}
					
					// divide offset down to correct range
					
					fragmentShader += "$BG_OFFSET /= resolution;\n";
					
					if(enablePaletteEffects == true) {
						
						// get palette index
						
						fragmentShader += "float $BG_INDEX = texture2D($BG_TEXTURE, $BG_OFFSET + v_texCoord).r;\n";
						fragmentShader += "$BG_INDEX *= 256.0;\n";
						
						// add palette cycling code if required
					
						switch(layer.getPaletteCycleType())
						{
							default:
								// no palette cycling
								break;
								
							case 1:
								// rotate palette subrange left
								fragmentShader +=
									"if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"{\n" +
									"    float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"    $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"    if($BG_INDEX < $BG_PALETTE_BEGIN1) {\n" +
									"        $BG_INDEX = $BG_PALETTE_END1 + 1.0 - abs($BG_PALETTE_BEGIN1 - $BG_INDEX);\n" +
									"    }\n" +
									"}\n";
								break;
							
							case 2:
								// rotate two palette subranges left
								fragmentShader +=
									"if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"{\n" +
									"    float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"    $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"    if($BG_INDEX < $BG_PALETTE_BEGIN1) {\n" +
									"        $BG_INDEX = $BG_PALETTE_END1 + 1.0 - abs($BG_PALETTE_BEGIN1 - $BG_INDEX);\n" +
									"    }\n" +
									"}\n" +
									"else if($BG_INDEX >= $BG_PALETTE_BEGIN2 - 0.5 && $BG_INDEX <= $BG_PALETTE_END2 + 0.5)\n" +
									"{\n" +
									"    float range = $BG_PALETTE_END2 - $BG_PALETTE_BEGIN2;\n" +
									"    $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"    if($BG_INDEX < $BG_PALETTE_BEGIN2) {\n" +
									"        $BG_INDEX = $BG_PALETTE_END2 + 1.0 - abs($BG_PALETTE_BEGIN2 - $BG_INDEX);\n" +
									"    }\n" +
									"}\n";
								break;
							
							case 3:
								// "mirror rotate" palette subrange left (indices cycle like a triangle waveform)
								fragmentShader +=
									"if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"{\n" +
									"    float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"    $BG_INDEX = $BG_INDEX + $BG_ROTATION - $BG_PALETTE_BEGIN1;\n" +
									"    range = floor(range + 0.5);\n" +
									"    $BG_INDEX = floor($BG_INDEX + 0.5);\n" +
									"    if($BG_INDEX > range * 2.0 + 1.0) {\n" +
									"        $BG_INDEX = $BG_PALETTE_BEGIN1 + ($BG_INDEX - ((range * 2.0) + 2.0));\n" +
									"    }\n" +
									"    else if($BG_INDEX > range) {\n" +
									"        $BG_INDEX = $BG_PALETTE_END1 - ($BG_INDEX - (range + 1.0));\n" +
									"    }\n" +
									"    else {\n" +
									"        $BG_INDEX += $BG_PALETTE_BEGIN1;\n" +
									"    }\n" +
									"}\n";
								break;
						}
					
						// divide color index down into texture lookup range
						
						fragmentShader += "$BG_INDEX /= 16.0;\n";
						
						// actual palette color lookup
						
						float paletteRow = layer == bbg.bg3 ? 0.0f : 1.0f;
						fragmentShader += "vec4 $BG_COLOR = texture2D(s_palette, vec2($BG_INDEX, " + paletteRow + " / 16.0));\n";
					} else {
						fragmentShader += "vec4 $BG_COLOR = texture2D($BG_TEXTURE, $BG_OFFSET + v_texCoord);\n";
					}
					
					// replace placeholder tags with values
					
					fragmentShader = interpolate(fragmentShader, map, prefix);
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
		Log.d("shader", fragmentShader);
		
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
	
	private static Pattern tokenPattern = Pattern.compile("(\\$[\\w]*)");

	public String interpolate(String tokenizedString, Map<String, String> mapping, String prefix) {
	    StringBuffer sb = new StringBuffer();
	    Matcher matcher = tokenPattern.matcher(tokenizedString);
	    
	    while(matcher.find())
	    {
	    	Log.i(TAG, "matcher find");
	        String field = matcher.group(1);
	        matcher.appendReplacement(sb, "");
	        Log.i(TAG, "match field: " + field);
	        sb.append(prefix + mapping.get(field));
	    }
	    
	    matcher.appendTail(sb);
	    return sb.toString();
	}
}

