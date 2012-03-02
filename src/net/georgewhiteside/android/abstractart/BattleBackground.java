package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

/*
Animation Bank

0A0200-0AD9A0 (00d7a1) = Battle BGs: Primary Data Group
0AD9A1-0ADB3C (00019c) = Battle BGs: Graphics Pointer Table
0ADB3D-0ADCD8 (00019c) = Battle BGs: Arrangement Pointer Table
0ADCD9-0ADEA0 (0001c8) = Battle BGs: Palette Pointer Table
0ADEA1-0AF457 (0015b7) = Battle BGs: Rendering Data
0AF458-0AF907 (0004b0) = Battle BGs: Scroll Table
0AF908-0B01FE (0008f7) = Battle BGs: Distortion Table
0B01FF-0B01FF (000001) = Nullspace
0B0200-0BDA99 (00d89a) = Battle BGs: Secondary Data Group
0BDA9A-0BE229 (000790) = Battle Group BG Association Data
*/


/*
2012-02-22: added scrolling background effects
			added scrolling background effect cycling
			added distortion effect cycling
*/

// TODO: background 34 doesn't render correctly, related to cycling code?
// fixed xTODO: background index 59 (layer index 43) incorrect, keeps increasing each iteration
// TODO: is 31(21) correct? must be some sort of skew parameter possibly...? also, should it have the very slight jump? see http://youtu.be/9XGrP7zrVUE?t=3m44s
// TODO: is 220 (223) 100% correct? I see a *very* minor jump at the transition from effect 3 -> 0, like a single frame is skipped; verify against SNES 
// TODO: 129 & 130 are pretty glitchy

// 0BDA9A-0BE229 (000790) = Battle Group BG Association Data

/**
 * Loads the images and effect information necessary to render a single battle background.
 * 
 * @author George Whiteside
 */
public class BattleBackground
{
	private static final String TAG = "BattleBackground";
	private static final int OFFSET = 0xA0200;
	
	private ByteBuffer romData;

	//private List<short[]> layerTable;
	private short[][] layerTable;

	public Layer layerA;
	public Layer layerB;
 	
	/**
	 * @param input an <code>InputStream</code> from which to read ROM battle background data
	 */
	public BattleBackground(InputStream input)
	{
		loadData(input);
		processLayerTable();
		//LayerAssociationTable = new ArrayList<short[]>();
		
		//romData.position(0xBDA9A - OFFSET);
		//layerAssociationTable = romData.slice();

		layerA = new Layer(romData);
		layerB = new Layer(romData);
	}
	
	public Layer getLayerA()
	{
		return layerA;
	}
	
	public Layer getLayerB()
	{
		return layerB;
	}
	
	/**
	 * @return number of <b>unique</b> background layer combinations
	 */
	public int getNumberOfBackgrounds()
	{
		return layerTable.length;
	}
	
	private void loadData(InputStream input)
	{
		// TODO: rewrite data loader
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
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
		romData.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Loads the background layer combination table and cleans it up.
	 * 
	 * Each battle background is composed of two independently animated layers, and these
	 * layer combinations are stored in a 484 x 2 table. There are different enemy battle
	 * groups that have the same background data, but because of the way the game data
	 * is structured, each battle group has its own (not necessarily unique) entry. Several
	 * of the entries end up being duplicates, so this method reads the table in while
	 * pruning the duplicates as a matter of convenience. Order is preserved for each
	 * unique entry.
	 */
	private void processLayerTable()
	{
		romData.position(0xBDA9A - OFFSET);
		
		final int MAX_ENTRIES = 484;
		int uniqueCount = 0;
		
		short[][] buffer = new short[MAX_ENTRIES][2];
		
		for(int i = 0; i < MAX_ENTRIES; i++)
		{
			short value0 = romData.getShort();
			short value1 = romData.getShort();
			
			boolean isUnique = true;
			for(int j = 0; j < uniqueCount; j++)
			{
				if(buffer[j][0] == value0 && buffer[j][1] == value1)
				{
					isUnique = false;
					break;
				}
			}
			
			if(isUnique == true)
			{
				buffer[uniqueCount][0] = value0;
				buffer[uniqueCount][1] = value1;
				uniqueCount++;
			}
		}
		
		layerTable = new short[uniqueCount][2];
		System.arraycopy(buffer, 0, layerTable, 0, uniqueCount);
	}
	
	public void setIndex(int index)
	{
		Log.d(TAG, "background group index: " + index);
		setLayers(layerTable[index][0], layerTable[index][1]);
	}
	
	public void setLayers(int A, int B)
	{
		layerA.load(A);
		layerB.load(B);
	}
}
