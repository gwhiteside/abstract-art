package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;

import android.content.Context;

/*
0D0200-0E64ED (0162ee) = Battle Sprites Graphics
0E64EE-0E6713 (000226) = Battle Sprites Pointer Table
0E6714-0E6B13 (000400) = Battle Sprites Palettes
*/

public class BattleEnemyFactory
{
	Context context;
	AbstractArt abstractArt;
	private static final int OFFSET = 0xD0200;
	
	private static final int BATTLE_SPRITE_GRAPHICS = 0xD0200;
	private static final int BATTLE_SPRITE_POINTER_TABLE = 0xE64EE;
	private static final int BATTLE_SPRITE_PALETTES = 0xE6714;
	
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
	
	public void getImage(int enemyIndex)
	{
		
	}
}
