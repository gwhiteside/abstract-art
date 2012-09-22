package net.georgewhiteside.android.abstractart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.georgewhiteside.android.aapreset.DistortionEffect;
import net.georgewhiteside.android.aapreset.PaletteEffect;
import net.georgewhiteside.android.aapreset.Preset;
import net.georgewhiteside.android.aapreset.TranslationEffect;

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
	
	private static final Map<String, String> map;
	
	static {
		Map<String, String> initMap = new HashMap<String, String>();
		
		initMap.put("$BG_TEXTURE", "texture");
		initMap.put("$BG_OFFSET", "offset");
		initMap.put("$BG_OFFSET_X", "offset.x");
		initMap.put("$BG_OFFSET_Y", "offset.y");
		initMap.put("$BG_DIST_TYPE", "dist_type");
		initMap.put("$BG_DISTORTION_OFFSET", "dist_offset");
		initMap.put("$BG_DIST_AMPL", "dist[AMPL]");
		initMap.put("$BG_DIST_FREQ", "dist[FREQ]");
		initMap.put("$BG_DIST_SPEED", "dist[SPEED]");
		initMap.put("$BG_COMPRESSION", "compression");
		initMap.put("$BG_INDEX", "index");
		initMap.put("$BG_SCROLL", "scroll");
		initMap.put("$BG_COLOR", "color");
		initMap.put("$BG_PALETTE_BEGIN1", "palette[BEGIN1]");
		initMap.put("$BG_PALETTE_END1", "palette[END1]");
		initMap.put("$BG_PALETTE_BEGIN2", "palette[BEGIN2]");
		initMap.put("$BG_PALETTE_END2", "palette[END2]");
		initMap.put("$BG_ROTATION", "rotation");
		
		map = Collections.unmodifiableMap(initMap);
	}
	
	public ShaderFactory(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public int getShader(Preset preset, float letterBoxSize)
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
				"    if(y < " + letterBoxSize + " || y > 224.0 - " + letterBoxSize + ") {\n" +
				"        gl_FragColor.rgba = vec4(0.0, 0.0, 0.0, 1.0);\n" +
				"    } else {\n";
			
			// iterate over layer(s) and tailor the most efficient shader possible
			List<net.georgewhiteside.android.aapreset.Layer> layers = preset.getLayers();
			int numLayers = layers.size();
			for(int i = 0; i < numLayers; i++)
			{
				net.georgewhiteside.android.aapreset.Layer layer = layers.get(i);
				String prefix = null;
				
				if(i == 0) {
					prefix = "bg3_";
				}
				if(i == 1) {
					prefix = "bg4_";
				}

				fragmentShader += "    vec2 $BG_OFFSET = vec2(0.0);\n";
				
				DistortionEffect dist = layer.getDistortionEffect();
				TranslationEffect trans = layer.getTranslationEffect();
				
				// distortion
				
				if(dist != null) {
					int numberOfEffects = dist.getNumberOfEffects();
					//numberOfEffects = 0;
					
					if(numberOfEffects != 0)
					{
						fragmentShader += "    float $BG_DISTORTION_OFFSET = ($BG_DIST_AMPL * sin($BG_DIST_FREQ * y + $BG_DIST_SPEED));\n";
						
						if(numberOfEffects == 1)
						{
							switch(dist.getType())
							{
								default:
									break;
									
								case 1:
									fragmentShader += "    $BG_OFFSET_X = $BG_DISTORTION_OFFSET;\n";
									break;
									
								case 2:
									fragmentShader += "    $BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n";
									break;
									
								case 3:
									fragmentShader += "    $BG_OFFSET_Y = mod($BG_DISTORTION_OFFSET, resolution.y);\n";
									break;
									
								case 4:
									fragmentShader += "    $BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
													"    $BG_OFFSET_X += (y * ($BG_COMPRESSION / resolution.y));\n";
									break;
							}
						}
						else // 2 or more effects are used
						{
							fragmentShader +=	"    if($BG_DIST_TYPE == 1) {\n" +
												"        $BG_OFFSET_X = $BG_DISTORTION_OFFSET;\n" +
									
												"    } else if($BG_DIST_TYPE == 2) {\n" +
												"        $BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
												
												"    } else if($BG_DIST_TYPE == 3) {\n" +
												"        $BG_OFFSET_Y = mod($BG_DISTORTION_OFFSET, resolution.y);\n" +
												"    }\n" +
												
												"    if($BG_DIST_TYPE == 4) {\n" +
												"        $BG_OFFSET_X = floor(mod(y, 2.0)) == 0.0 ? $BG_DISTORTION_OFFSET : -$BG_DISTORTION_OFFSET;\n" +
												"        $BG_OFFSET_X += (y * ($BG_COMPRESSION / resolution.y));\n" +
												"    }\n";
						}
					}
					
					// vertical compression
					
					if(dist.getType() != 4 && dist.getCompression() != 0 && dist.getCompressionDelta() != 0)
					{
						fragmentShader += "    $BG_OFFSET_Y += (y * ($BG_COMPRESSION / resolution.y));\n";
					}
				}
				
				// layer scrolling
				if(trans != null) {
					if(	trans.getHorizontalAcceleration() != 0 || trans.getHorizontalVelocity() != 0 ||
						trans.getVerticalAcceleration() != 0 || trans.getVerticalVelocity() != 0 )
					{
						fragmentShader += "    $BG_OFFSET += $BG_SCROLL;\n";
					}
				}
				
				// divide offset down to correct range
				
				fragmentShader += "    $BG_OFFSET /= resolution;\n";
				
				if(enablePaletteEffects == true) {
					
					// get palette index
					
					fragmentShader += "    float $BG_INDEX = texture2D($BG_TEXTURE, $BG_OFFSET + v_texCoord).r;\n";
					fragmentShader += "    $BG_INDEX *= 256.0;\n";
					
					// add palette cycling code if required
					PaletteEffect pal = layer.getPaletteEffect();
					if(pal != null) {
						switch(pal.getType())
						{
							default:
								// no palette cycling
								break;
								
							case 1:
								// rotate palette subrange left
								fragmentShader +=
									"    if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"    {\n" +
									"        float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"        $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"        if($BG_INDEX < $BG_PALETTE_BEGIN1) {\n" +
									"            $BG_INDEX = $BG_PALETTE_END1 + 1.0 - abs($BG_PALETTE_BEGIN1 - $BG_INDEX);\n" +
									"        }\n" +
									"    }\n";
								break;
							
							case 2:
								// rotate two palette subranges left
								fragmentShader +=
									"    if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"    {\n" +
									"        float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"        $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"        if($BG_INDEX < $BG_PALETTE_BEGIN1) {\n" +
									"            $BG_INDEX = $BG_PALETTE_END1 + 1.0 - abs($BG_PALETTE_BEGIN1 - $BG_INDEX);\n" +
									"        }\n" +
									"    }\n" +
									"    else if($BG_INDEX >= $BG_PALETTE_BEGIN2 - 0.5 && $BG_INDEX <= $BG_PALETTE_END2 + 0.5)\n" +
									"    {\n" +
									"        float range = $BG_PALETTE_END2 - $BG_PALETTE_BEGIN2;\n" +
									"        $BG_INDEX = $BG_INDEX - $BG_ROTATION;\n" +
									"        if($BG_INDEX < $BG_PALETTE_BEGIN2) {\n" +
									"            $BG_INDEX = $BG_PALETTE_END2 + 1.0 - abs($BG_PALETTE_BEGIN2 - $BG_INDEX);\n" +
									"        }\n" +
									"    }\n";
								break;
							
							case 3:
								// "mirror rotate" palette subrange left (indices cycle like a triangle waveform)
								fragmentShader +=
									"    if($BG_INDEX >= $BG_PALETTE_BEGIN1 - 0.5 && $BG_INDEX <= $BG_PALETTE_END1 + 0.5)\n" +
									"    {\n" +
									"        float range = $BG_PALETTE_END1 - $BG_PALETTE_BEGIN1;\n" +
									"        $BG_INDEX = $BG_INDEX + $BG_ROTATION - $BG_PALETTE_BEGIN1;\n" +
									"        range = floor(range + 0.5);\n" +
									"        $BG_INDEX = floor($BG_INDEX + 0.5);\n" +
									"        if($BG_INDEX > range * 2.0 + 1.0) {\n" +
									"            $BG_INDEX = $BG_PALETTE_BEGIN1 + ($BG_INDEX - ((range * 2.0) + 2.0));\n" +
									"        }\n" +
									"        else if($BG_INDEX > range) {\n" +
									"            $BG_INDEX = $BG_PALETTE_END1 - ($BG_INDEX - (range + 1.0));\n" +
									"        }\n" +
									"        else {\n" +
									"            $BG_INDEX += $BG_PALETTE_BEGIN1;\n" +
									"        }\n" +
									"    }\n";
								break;
						}
					}
					// divide color index down into texture lookup range
					
					fragmentShader += "    $BG_INDEX /= 16.0;\n";
					
					// actual palette color lookup
					
					float paletteRow = i == 0 ? 0.0f : 1.0f; // i == 0 in the outer loop is the bottom layer; 1 is the top
					fragmentShader += "    vec4 $BG_COLOR = texture2D(s_palette, vec2($BG_INDEX, " + paletteRow + " / 16.0));\n";
				} else {
					fragmentShader += "    vec4 $BG_COLOR = texture2D($BG_TEXTURE, $BG_OFFSET + v_texCoord);\n";
				}
				
				// replace placeholder tags with values
				
				fragmentShader = interpolate(fragmentShader, map, prefix);
			}
			
			// output blending
			
			if(numLayers == 2)
			{
				// both layers are active; perform an alpha blend with BG4 at 50% opacity
				fragmentShader += 
					"    bg4_color.a *= 0.5;\n" +
					"    gl_FragColor.rgb = bg4_color.rgb * bg4_color.a + bg3_color.rgb * (1.0 - bg4_color.a);\n" +
					"    gl_FragColor.a = 1.0;\n";
			}
			else
			{
				fragmentShader +=
					"    gl_FragColor.rgb = bg3_color.rgb;\n" +
					"    gl_FragColor.a = 1.0;\n";
			}
		
			// ...aaand the final curly brace:
			
			fragmentShader += "    }\n";
			fragmentShader += "}\n";
		}
		
		//Log.d("shader", vertexShader);
		//Log.d("shader", fragmentShader);
		
		int result = createProgram(vertexShader, fragmentShader);
		
		if(result == 0) {
			String error = "Shader compilation failed!";
			error += "\n \n-- VERTEX SHADER\n \n";
			error += vertexShader;
			error += "\n \n-- FRAGMENT SHADER\n \n";
			error += fragmentShader;
			throw new RuntimeException(error);
		}
		
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
	        String field = matcher.group(1);
	        matcher.appendReplacement(sb, "");
	        sb.append(prefix + mapping.get(field));
	    }
	    
	    matcher.appendTail(sb);
	    return sb.toString();
	}
}

