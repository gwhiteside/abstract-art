package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	
	private static final int GRAPHICS_CHUNK_OFFSET = 0xD0200;
	private static final int GRAPHICS = 0xD0200;
	private static final int POINTER_TABLE = 0xE64EE;
	private static final int PALETTES = 0xE6714;
	//private static final int NUM_GRAPHIC_ENTRIES = 110;
	
	private static final int ATTRIBUTES_CHUNK_OFFSET = 0x159789;
	private static final int ATTRIBUTES = 0x159789;
	private static final int NUM_ATTRIBUTE_ENTRIES = 231;
	private static final int ATTRIBUTE_ENTRY_LEN = 94;
	
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
	
	ByteBuffer spriteData;
	ByteBuffer attributeData;
	
	public Enemy(Context context)
	{
		this.context = context;
		spriteData = loadData(R.raw.enemy_battle_sprite_data, 92436).order(ByteOrder.LITTLE_ENDIAN);;
		attributeData = loadData(R.raw.enemy_attribute_data, 21714).order(ByteOrder.LITTLE_ENDIAN);;
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
		
		attributeData.position(enemyIndex * 94);
		ByteBuffer attributes = attributeData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
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
		
		spriteData.position(GRAPHICS - GRAPHICS_CHUNK_OFFSET);
		ByteBuffer graphics = spriteData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		spriteData.position(POINTER_TABLE - GRAPHICS_CHUNK_OFFSET);
		ByteBuffer pointerTable = spriteData.slice().order(ByteOrder.LITTLE_ENDIAN);

		pointerTable.position(spriteIndex * 5);
		int pSpriteData = RomUtil.toHex(pointerTable.getInt()) - GRAPHICS_CHUNK_OFFSET;
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
		attributeData.position(enemyIndex * 94);
		ByteBuffer attributes = attributeData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		// load name
		
		mName = getName(enemyIndex);
		
		// load row
		row = RomUtil.unsigned(attributes.get(0x5B));
		
		// load palette
		
		int paletteNum = attributes.get(0x35);
		Log.i(TAG, mName + " palette number: " + paletteNum);
		spriteData.position(PALETTES - GRAPHICS_CHUNK_OFFSET);
		ShortBuffer paletteData = spriteData.asShortBuffer().slice();
		
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
		
		// TODO: loadBattleSprite has been crashing for several people (line 174, pointerTable.position(spriteIndex * 5);) but I can't reproduce
		// the problem on either of my phones. It only seems to be this part with the enemy loading, the backgrounds themselves have worked fine 
		// for a while. SO, until I can find a device that has this problem, I'm just going to have to check to make sure the index is <= 110
		// so we don't get crazy out of bounds exceptions. I have no idea why that's happening.
		if(spriteIndex > 0 && spriteIndex <= 110) loadBattleSprite(spriteIndex - 1); // in the game ROM a value of 0 is reserved for "invisible," no actual sprite data is loaded
		else loadInvisibleSprite();
		
		if(spriteIndex > 110) {
		    Log.e(TAG, "Error: spriteIndex > 110 attempted (?!). Loading invisible sprite instead.");
		}
	}
	
	public ByteBuffer loadData(int rawResource, int size)
	{
		// TODO: rewrite data loader
		ByteBuffer romData;
		
		InputStream input = context.getResources().openRawResource(rawResource);
		ByteArrayOutputStream output = new ByteArrayOutputStream(size); // currently, largest file is a bit over 120kb... trying to avoid over allocating heap
		
		int bytesRead;
		byte[] buffer = new byte[16384];
		
		try {
			while((bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		romData = ByteBuffer.wrap(output.toByteArray());
		
		return romData;
	}

}
