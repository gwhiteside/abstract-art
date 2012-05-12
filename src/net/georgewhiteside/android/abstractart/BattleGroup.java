package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class BattleGroup
{
	private Context context;
	private AbstractArt abstractArt;
	public BattleBackground battleBackground;
	public Enemy enemy;
	
	private ByteBuffer enemyBattleGroupPointers;
	private ByteBuffer enemyBattleGroupData;
	
	public static final int LETTER_BOX_NONE = 0, LETTER_BOX_SMALL = 1, LETTER_BOX_MEDIUM = 2, LETTER_BOX_LARGE = 3;
	
	private static final int ENEMY_BATTLE_GROUP_DATA_OFFSET = 0x10D72D;
	
	private int letterBoxSize = LETTER_BOX_NONE;
	private int enemyIndex = 0;
	
	public BattleGroup(Context context)
	{
		this.context = context;
		abstractArt = (AbstractArt)context.getApplicationContext();
		battleBackground = new BattleBackground(context);
		enemy = new Enemy(context);
		enemyBattleGroupPointers = abstractArt.loadData(R.raw.enemy_battle_group_pointers);
		enemyBattleGroupData = abstractArt.loadData(R.raw.enemy_battle_group_data);
	}
	
	public int getEnemyIndex()
	{
		return enemyIndex;
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
		
		enemyBattleGroupPointers.position(trueIndex * 8);
		int pBattleGroup = RomUtil.toHex(enemyBattleGroupPointers.getInt()) - ENEMY_BATTLE_GROUP_DATA_OFFSET;
		enemyBattleGroupPointers.getShort(); // flag identifier (short value)
		enemyBattleGroupPointers.get(); // run away when flag is... (boolean value)
		letterBoxSize = RomUtil.unsigned(enemyBattleGroupPointers.get()); // letterbox size; corresponds to LETTER_BOX_* defined in this class
		
		// using the pointer we just retrieved, read the data from the enemy battle group data table
		
		enemyBattleGroupData.position(pBattleGroup);
		//List<Integer> enemyBattleGroup = new ArrayList<Integer>();
		int amount = 0;
		
		amount = RomUtil.unsigned(enemyBattleGroupData.get());
		enemyIndex = RomUtil.unsigned(enemyBattleGroupData.getShort());
		
		enemy.load(enemyIndex);
	}
}
