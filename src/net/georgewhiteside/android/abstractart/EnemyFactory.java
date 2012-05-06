package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.nio.ByteBuffer;

import net.georgewhiteside.utility.Dimension;
import net.starmen.pkhack.HackModule;

import android.content.Context;
import android.util.Log;

/*
0D0200-0E64ED (0162ee) = Battle Sprites Graphics
0E64EE-0E6713 (000226) = Battle Sprites Pointer Table
0E6714-0E6B13 (000400) = Battle Sprites Palettes
*/

public class EnemyFactory
{
	private static final String TAG = "BattleEnemyFactory";
	Context context;
	AbstractArt abstractArt;
	private static final int OFFSET = 0xD0200;
	
	private static final int GRAPHICS = 0xD0200;
	private static final int POINTER_TABLE = 0xE64EE;
	private static final int PALETTES = 0xE6714;
	
	private static final int NUM_ENTRIES = 110;
	
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
	
	ByteBuffer romData;
	
	
	
	public class Enemy
	{
		private int index;
		
	}
	
	public EnemyFactory(Context context)
	{
		this.context = context;
		abstractArt = (AbstractArt)context.getApplicationContext();
		romData = abstractArt.loadData(R.raw.enemy_battle_sprite_data);
	}
	
	public Enemy getEnemy(int index)
	{
		if(index < 0 || index >= NUM_ENTRIES)
		{
			Log.e(TAG, "getEnemy(): invalid index");
			return null;
		}
		
		romData.position(GRAPHICS - OFFSET);
		ByteBuffer graphics = romData.slice();
		
		romData.position(POINTER_TABLE - OFFSET);
		ByteBuffer pointerTable = romData.slice();
		
		romData.position(PALETTES - OFFSET);
		ByteBuffer palettes = romData.slice();
		
		pointerTable.position(index * 5);
		int pSpriteData = RomUtil.toHex(pointerTable.getInt()) - OFFSET;
		Dimension dimensions = DIMENSIONS[pointerTable.get()];
		
		int decompLen = RomUtil.decompress(pSpriteData, buffer, buffer.length, graphics);
		
		byte[] sprite = new byte[dimensions.width * dimensions.height];
		
		int offset = 0;
		for(int q = 0; q < (dimensions.height / 32); q++)
        {
            for(int r = 0; r < (dimensions.width / 32); r++)
            {
                for(int a = 0; a < 4; a++)
                {
                    for(int j = 0; j < 4; j++)
                    {
                        HackModule.read4BPPArea(sprite, buffer, offset, (j + r * 4) * 8, (a + q * 4) * 8, dimensions.width, 0);
                        offset += 32;
                    }
                }
            }
        }
		
		File outFile = new File(context.getCacheDir(), "test-indexed-image.bin");
		Cache.write(outFile, sprite, sprite.length);
		
		return null;
	}
}
