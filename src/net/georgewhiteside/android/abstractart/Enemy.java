package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import net.georgewhiteside.utility.Dimension;
import net.starmen.pkhack.HackModule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

/*
0D0200-0E64ED (0162ee) = Battle Sprites Graphics
0E64EE-0E6713 (000226) = Battle Sprites Pointer Table
0E6714-0E6B13 (000400) = Battle Sprites Palettes
*/

public class Enemy
{
	private static final String TAG = "Enemy";
	Context context;
	
	private static final int GRAPHICS = 0xD0000;               // Battle Sprites Graphics
	private static final int POINTER_TABLE = 0xE62EE;          // Battle Sprites Pointer Table
	private static final int PALETTES = 0xE6514;               // Battle Sprites Palettes
	private static final int ENEMY_ATTRIBUTE_DATA = 0x159589;  // Enemy Configuration Table
	//private static final int NUM_GRAPHIC_ENTRIES = 110;
	
	private int currentIndex;
	
	private int[] palette = new int[16];
	private String mName;
	private int row; // 0 = front, 1 = back
	
	ByteBuffer battleSprite;
	Dimension dimensions;
	
	byte[] buffer = new byte[128 * 128 / 2]; // max X dimension * max Y dimension * 4bpp
	byte[] spriteIndexedBuffer = new byte[128 * 128];
	byte[] spriteRgbaBuffer = new byte[128 * 128 * 4];
	
	public final static Dimension[] DIMENSIONS = new Dimension[]
	{
		null,
        new Dimension(32, 32),
        new Dimension(64, 32),
        new Dimension(32, 64),
        new Dimension(64, 64),
        new Dimension(128, 64),
        new Dimension(128, 128)
	};
	
	ByteBuffer romData;
	
	public Enemy(Context context, ByteBuffer romData)
	{
		this.context = context;
		this.romData = romData;
		currentIndex = -1;
	}
	
	public void load(int index)
	{
		if(currentIndex != index)
		{
			if(index < 0 || index >= 231)
			{
				Log.e(TAG, "getEnemy(): invalid index");
				return;
			}
	
			loadAttributes(index);
			
			currentIndex = index; // note the currently loaded index so the data can be accessed efficiently
		}
	}
	
	public byte[] getBattleSprite()
	{
		return battleSprite.array();
	}
	
	public int getBattleSpriteWidth()
	{
		return dimensions.width;
	}
	
	public int getBattleSpriteHeight()
	{
		return dimensions.height;
	}
	
	public String getCurrentName()
	{
		return mName;
	}
	
	public String getName(int enemyIndex)
	{
		// save some processing time if the name was already decoded
		if(currentIndex == enemyIndex) {
			return mName;
		}
		
		romData.position(ENEMY_ATTRIBUTE_DATA + enemyIndex * 94);
		ByteBuffer attributes = romData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		// load name
		
		attributes.position(1);
		int maxStringLen = 25;
		
		StringBuilder sb = new StringBuilder(maxStringLen);
		
		for(int i = 0; i < maxStringLen; i++)
		{
			short character = RomUtil.unsigned(attributes.get());
			
			if(character == 0x00) break;
			
			if(character < 0x30) {
				character = '?';
			} else {
				character -= 0x30;
			}
			
			if(character == 0x7C)  {
				// pipe character is interpreted as the main character's name
				sb.append("Ness");
			} else {
				sb.append((char)(character));
			}
		}
		
		return sb.toString();
	}
	
	public int getRow()
	{
		return row;
	}
	
	private void loadBattleSprite(int spriteIndex)
	{
		String cacheFileName = mName + ".png";
		File cacheDir = new File(context.getCacheDir(), "sprites");
		
		File cacheFile = new File(cacheDir, cacheFileName);
		
		romData.position(GRAPHICS);
		ByteBuffer graphics = romData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		romData.position(POINTER_TABLE);
		ByteBuffer pointerTable = romData.slice().order(ByteOrder.LITTLE_ENDIAN);

		pointerTable.position(spriteIndex * 5);
		int pSpriteData = RomUtil.toHex(pointerTable.getInt());
		dimensions = DIMENSIONS[pointerTable.get()];
		
		if(cacheFile.exists())
		{
			Log.i(TAG, "Loading existing sprite: " + cacheFileName);
			// TODO: integrate the nitty-gritty into the Cache class

			ByteBuffer bitmapBuffer = ByteBuffer.allocate(dimensions.width * dimensions.height * 4);

			// can cause a crash on rare occasions ... mainly when adding new features ;)
			// SO, just trapping any potential problems here so I don't get slowed down
			try {
				BitmapFactory.decodeFile(cacheFile.getPath()).copyPixelsToBuffer(bitmapBuffer);
			}
			catch(Exception e) {
				Log.e("AbstractArt", "Couldn't open " + cacheFile.getPath() + " ... " + e.getMessage());
			}
			
			battleSprite = bitmapBuffer;
		}
		else
		{
			// build sprite image from ROM data
			
			int decompLen = RomUtil.decompress(pSpriteData, buffer, buffer.length, graphics);
			
			Arrays.fill(spriteIndexedBuffer, (byte) 0);
			
			int offset = 0;
			for(int q = 0; q < (dimensions.height / 32); q++)
	        {
	            for(int r = 0; r < (dimensions.width / 32); r++)
	            {
	                for(int a = 0; a < 4; a++)
	                {
	                    for(int j = 0; j < 4; j++)
	                    {
	                        HackModule.read4BPPArea(spriteIndexedBuffer, buffer, offset, (j + r * 4) * 8, (a + q * 4) * 8, dimensions.width, 0);
	                        offset += 32;
	                    }
	                }
	            }
	        }
			
			// colorize the indexed image giving us an array of RGBA values
			
			battleSprite = ByteBuffer.wrap(spriteRgbaBuffer, 0, dimensions.width * dimensions.height * 4);
			IntBuffer rgba = battleSprite.asIntBuffer();
			
			for(int i = 0; i < dimensions.width * dimensions.height; i++)
			{
				rgba.put(palette[spriteIndexedBuffer[i]]);
			}
			
			// save the image to cache
			Log.i(TAG, "Saving sprite " + mName + " to cache");
			
			cacheFile.getParentFile().mkdirs(); // safely does nothing if path exists
 			
 			try {
 				Bitmap img = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888);
 				img.copyPixelsFromBuffer(battleSprite);
 				FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
 				img.compress(CompressFormat.PNG, 80, fileOutputStream); // quality is irrelevant for PNGs
 				fileOutputStream.close();
 			} catch (FileNotFoundException e) {
 				e.printStackTrace();
 			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void loadInvisibleSprite()
	{
		dimensions = DIMENSIONS[1];
		
		// create fully transparent black sprite
		
		byte[] spriteRgba = new byte[dimensions.width * dimensions.height * 4];
		battleSprite = ByteBuffer.wrap(spriteRgba);
		IntBuffer rgba = battleSprite.asIntBuffer();
		
		for(int i = 0; i < spriteRgba.length; i += 4)
		{
			rgba.put(0x00000000);
		}
		
		//File outFile = new File(context.getExternalCacheDir(), "image.bin");
		//Cache.write(outFile, spriteRgba, spriteRgba.length);
	}
	
	private void loadAttributes(int enemyIndex)
	{
	    romData.position(ENEMY_ATTRIBUTE_DATA + enemyIndex * 94);
		ByteBuffer attributes = romData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		// load name
		
		mName = getName(enemyIndex);
		
		// load row
		row = RomUtil.unsigned(attributes.get(0x5B));
		
		// load palette
		
		int paletteNum = attributes.get(0x35);
		Log.i(TAG, mName + " palette number: " + paletteNum);
		romData.position(PALETTES);
		ShortBuffer paletteData = romData.asShortBuffer().slice();
		
		paletteData.position(paletteNum * 16);
		
		for(int c = 0; c < 16; c++)
		{
			// values are packed RGB555:
			// 0BBBBBGG GGGRRRRR
			
			short color = paletteData.get();
			int r = 0, g = 0, b = 0;
			b = (color >> 10) & 0x1F;
			g = (color >> 5) & 0x1F;
			r = color & 0x1F;
			
			// scale to 8-bit values
			
			r = (r << 3 | r >> 2);
			g = (g << 3 | g >> 2);
			b = (b << 3 | b >> 2);
			
			palette[c] = r << 24 | g << 16 | b << 8 | 0xFF;
		}
		palette[0] &= 0xffffff00; // make palette color 0 transparent
		
		// load battle sprite
		
		int spriteIndex = attributes.getShort(0x1c);
		
		if(spriteIndex > 0 && spriteIndex <= 110) loadBattleSprite(spriteIndex - 1); // in the game ROM a value of 0 is reserved for "invisible," no actual sprite data is loaded
		else loadInvisibleSprite();
		
		if(spriteIndex > 110) {
		    Log.e(TAG, "Error: spriteIndex > 110 attempted (?!). Loading invisible sprite instead.");
		}
	}
	

}
