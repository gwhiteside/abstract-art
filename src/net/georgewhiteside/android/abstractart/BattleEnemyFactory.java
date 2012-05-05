package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;

import android.content.Context;
import android.util.Log;

/*
0D0200-0E64ED (0162ee) = Battle Sprites Graphics
0E64EE-0E6713 (000226) = Battle Sprites Pointer Table
0E6714-0E6B13 (000400) = Battle Sprites Palettes
*/

public class BattleEnemyFactory
{
	private static final String TAG = "BattleEnemyFactory";
	Context context;
	AbstractArt abstractArt;
	private static final int OFFSET = 0xD0200;
	
	private static final int GRAPHICS = 0xD0200;
	private static final int POINTER_TABLE = 0xE64EE;
	private static final int PALETTES = 0xE6714;
	
	private static final int NUM_ENTRIES = 110;
	
	ByteBuffer romData;
	
	public class BattleEnemy
	{
		private int index;
		
	}
	
	public BattleEnemyFactory(Context context)
	{
		this.context = context;
		abstractArt = (AbstractArt)context.getApplicationContext();
		romData = abstractArt.loadData(R.raw.enemy_battle_sprite_data);
	}
	
	public BattleEnemy getEnemy(int index)
	{
		if(index < 0 || index >= NUM_ENTRIES)
		{
			Log.e(TAG, "getEnemy(): invalid index");
			return null;
		}
		
		romData.position(GRAPHICS - OFFSET);
		ByteBuffer spriteGraphics = romData.slice();
		
		romData.position(POINTER_TABLE - OFFSET);
		ByteBuffer spritePointers = romData.slice();
		
		romData.position(PALETTES - OFFSET);
		ByteBuffer spritePalettes = romData.slice();
		
		byte[] uncompressed = new byte[16384];
		
		
		
		return null;
	}
}
