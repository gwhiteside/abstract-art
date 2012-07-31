package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class BattleGroup
{
	private Context context;
	public BattleBackground battleBackground;
	public Enemy enemy;
	
	private ByteBuffer romData;
	
	public static final int LETTER_BOX_NONE = 0, LETTER_BOX_SMALL = 1, LETTER_BOX_MEDIUM = 2, LETTER_BOX_LARGE = 3;
	
	private static final int ENEMY_BATTLE_GROUP_POINTERS = 0x10C60D;   // Enemy Battle Groups Pointer Table
	private static final int ENEMY_BATTLE_GROUP_DATA = 0x10D52D;       // Enemy Battle Groups Table
	
	private int letterBoxSize = LETTER_BOX_NONE;
	private int enemyIndex = 0;
	
	public BattleGroup(Context context, ByteBuffer romData)
	{
		this.context = context;
		this.romData = romData;
		battleBackground = new BattleBackground(context, romData);
		enemy = new Enemy(context, romData);
	}
	
	public int getCurrentEnemyIndex()
	{
		return enemyIndex;
	}
	
	/*
	 * TODO I'm just letting stuff get really hacky now... fix this
	 */
	public int getEnemyIndex(int battleGroupIndex)
	{
		int trueIndex = battleBackground.getRomBackgroundIndex(battleGroupIndex); // necessary so long as we prune the background list of "duplicates"
		romData.position(ENEMY_BATTLE_GROUP_POINTERS + trueIndex * 8);
		int pBattleGroup = RomUtil.toHex(romData.getInt());
		romData.position(pBattleGroup + 1); // skip first byte of entry (number of "this enemy"s that appear)
		int myEnemyIndex = RomUtil.unsigned(romData.getShort()); // enemy table index of enemy to appear
		
		return myEnemyIndex;
	}
	
	public int getLetterBoxSize()
	{
		return letterBoxSize;
	}
	
	public int getLetterBoxPixelSize()
	{
		switch(letterBoxSize)
		{
			default: return 0;
			case LETTER_BOX_NONE: return 0;
			case LETTER_BOX_SMALL: return 48;	
			case LETTER_BOX_MEDIUM: return 58;
			case LETTER_BOX_LARGE: return 68;
		}
	}
	
	public void load(int index)
	{
		battleBackground.setIndex(index);
		
		int trueIndex = battleBackground.getRomBackgroundIndex(index); // necessary so long as we prune the background list of "duplicates"
		
		// for now we're just loading up an enemy group, rendering the first enemy, and leaving it at that.
		// for future reference, there are "duplicate" backgrounds that actually have different enemy groups,
		// and multiple enemies could be rendered for some of the battle groups
		
		romData.position(ENEMY_BATTLE_GROUP_POINTERS + trueIndex * 8);
		int pBattleGroup = RomUtil.toHex(romData.getInt());
		romData.getShort(); // flag identifier (short value)
		romData.get(); // run away when flag is... (boolean value)
		letterBoxSize = RomUtil.unsigned(romData.get()); // letterbox size; corresponds to LETTER_BOX_* defined in this class
		
		// using the pointer we just retrieved, read the data from the enemy battle group data table
		
		romData.position(pBattleGroup);
		//List<Integer> enemyBattleGroup = new ArrayList<Integer>();
		int amount = 0;
		
		amount = RomUtil.unsigned(romData.get());
		enemyIndex = RomUtil.unsigned(romData.getShort());
		
		enemy.load(enemyIndex);
	}
}
