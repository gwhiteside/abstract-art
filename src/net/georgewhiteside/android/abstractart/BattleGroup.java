package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class BattleGroup
{
	private Context context;
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
		battleBackground = new BattleBackground(context);
		enemy = new Enemy(context);
		enemyBattleGroupPointers = loadData(R.raw.enemy_battle_group_pointers, 3872).order(ByteOrder.LITTLE_ENDIAN);
		enemyBattleGroupData = loadData(R.raw.enemy_battle_group_data, 2695).order(ByteOrder.LITTLE_ENDIAN);
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
		ByteBuffer myEnemyBattleGroupPointers = enemyBattleGroupPointers.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		myEnemyBattleGroupPointers.position(trueIndex * 8);
		int pBattleGroup = RomUtil.toHex(myEnemyBattleGroupPointers.getInt()) - ENEMY_BATTLE_GROUP_DATA_OFFSET;
		ByteBuffer myEnemyBattleGroupData = enemyBattleGroupData.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		myEnemyBattleGroupData.position(pBattleGroup + 1); // skip first byte of entry (number of "this enemy"s that appear)
		int myEnemyIndex = RomUtil.unsigned(myEnemyBattleGroupData.getShort()); // enemy table index of enemy to appear
		
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
