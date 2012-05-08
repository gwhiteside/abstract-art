package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import net.georgewhiteside.utility.Dimension;
import net.starmen.pkhack.HackModule;

import android.content.Context;
import android.graphics.Bitmap;
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
	AbstractArt abstractArt;
	
	private static final int GRAPHICS_CHUNK_OFFSET = 0xD0200;
	private static final int GRAPHICS = 0xD0200;
	private static final int POINTER_TABLE = 0xE64EE;
	private static final int PALETTES = 0xE6714;
	private static final int NUM_GRAPHIC_ENTRIES = 110;
	
	private static final int ATTRIBUTES_CHUNK_OFFSET = 0x159789;
	private static final int ATTRIBUTES = 0x159789;
	private static final int NUM_ATTRIBUTE_ENTRIES = 231;
	private static final int ATTRIBUTE_ENTRY_LEN = 94;
	
	private int[] palette = new int[16];
	private String name;
	
	ByteBuffer battleSprite;
	Dimension dimensions;
	
	byte[] buffer = new byte[128 * 128 / 2]; // max X dimension * max Y dimension * 4bpp
	
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
		abstractArt = (AbstractArt)context.getApplicationContext();
		spriteData = abstractArt.loadData(R.raw.enemy_battle_sprite_data);
		attributeData = abstractArt.loadData(R.raw.enemy_attribute_data);
	}
	
	public void load(int index)
	{
		if(index < 0 || index >= NUM_GRAPHIC_ENTRIES)
		{
			Log.e(TAG, "getEnemy(): invalid index");
			return;
		}
		
		loadAttributes(index);
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
	
	public String getName()
	{
		return name;
	}
	
	private void loadBattleSprite(int spriteIndex)
	{
		spriteData.position(GRAPHICS - GRAPHICS_CHUNK_OFFSET);
		ByteBuffer graphics = spriteData.slice();
		
		spriteData.position(POINTER_TABLE - GRAPHICS_CHUNK_OFFSET);
		ByteBuffer pointerTable = spriteData.slice();

		pointerTable.position(spriteIndex * 5);
		int pSpriteData = RomUtil.toHex(pointerTable.getInt()) - GRAPHICS_CHUNK_OFFSET;
		dimensions = DIMENSIONS[pointerTable.get()];
		
		// build sprite image from ROM data
		
		int decompLen = RomUtil.decompress(pSpriteData, buffer, buffer.length, graphics);
		
		byte[] spriteIndexed = new byte[dimensions.width * dimensions.height];
		
		int offset = 0;
		for(int q = 0; q < (dimensions.height / 32); q++)
        {
            for(int r = 0; r < (dimensions.width / 32); r++)
            {
                for(int a = 0; a < 4; a++)
                {
                    for(int j = 0; j < 4; j++)
                    {
                        HackModule.read4BPPArea(spriteIndexed, buffer, offset, (j + r * 4) * 8, (a + q * 4) * 8, dimensions.width, 0);
                        offset += 32;
                    }
                }
            }
        }
		
		// colorize the indexed image giving us an array of RGBA values
		
		byte[] spriteRgba = new byte[spriteIndexed.length * 4];
		battleSprite = ByteBuffer.wrap(spriteRgba);
		IntBuffer rgba = battleSprite.asIntBuffer();
		
		for(int i = 0; i < spriteIndexed.length; i++)
		{
			rgba.put(palette[spriteIndexed[i]]);
		}
		
		//File outFile = new File(context.getExternalCacheDir(), "image.bin");
		//Cache.write(outFile, spriteRgba, spriteRgba.length);
	}
	
	private void loadAttributes(int enemyIndex)
	{
		attributeData.position(enemyIndex * 94);
		ByteBuffer attributes = attributeData.slice();
		
		// load name
		
		attributes.position(1);
		int maxStringLen = 25;
		
		StringBuilder sb = new StringBuilder(maxStringLen);
		
		for(int i = 0; i < maxStringLen; i++)
		{
			short character = RomUtil.unsigned(attributes.get());
			
			if(character == 0x00) break;
			
			if(character < 0x30)
			{
				character = '?';
			}
			else
			{
				character -= 0x30;
			}
			
			sb.append((char)(character));
		}
		
		name = sb.toString();
		
		
		// load palette
		
		int paletteNum = attributes.get(0x35);
		Log.i(TAG, name + " palette number: " + paletteNum);
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
		
		loadBattleSprite(spriteIndex - 1); // in the game ROM a value of 0 is reserved for "invisible," no actual sprite data is loaded
	}
	

}
