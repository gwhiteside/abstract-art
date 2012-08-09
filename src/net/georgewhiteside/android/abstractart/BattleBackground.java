package net.georgewhiteside.android.abstractart;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

/*

SNES BG3 (main) and BG4 (sub)

"In all modes and for all BGs, color 0 in any palette is considered transparent."
 
*/

// spiteful crow entry rom location: 0xAE930

// TODO: is layer 21 (mobile sprout, bg45 in selector) correct? must be some sort of skew parameter possibly...? also, should it have the very slight jump? see http://youtu.be/9XGrP7zrVUE?t=3m44s update: looks correct according to zsnes; check game hardware (or bsnes) just in case

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
	SharedPreferences sharedPreferences;
	Context context;
	
	private ByteBuffer romData;
	private int currentIndex;
	private int currentRomBackgroundIndex;
	
	//private List<short[]> layerTable;
	private short[][] layerTable;

	public Layer bg3;
	public Layer bg4;
	
	// backgrounds we don't want showing up in the background viewer for one reason or another
	private static final List<Integer> blacklist = Arrays.asList(
		// things I just don't want to include for the moment
		9,		// skate punk; already exists (in our currently incomplete enemy loader)
		11,		// skate punk; already exists (in our currently incomplete enemy loader)
		371,	// fobby; already exists (in our currently incomplete enemy loader)
		
		// things I probably never want to include in the list
		0,		// (unused) spiteful crow (3); blank background
		425,	// magic butterfly; blank
		473,	// starman jr.; blank
		481,	// magic butterfly??
		483		// giygas; blank
	);
	
	/**
	 * @param input an <code>InputStream</code> from which to read ROM battle background data
	 */
	public BattleBackground(Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		romData = loadData(R.raw.bgbank, 122922).order(ByteOrder.LITTLE_ENDIAN);
		
		processLayerTable();

		bg3 = new Layer(romData, context);
		bg4 = new Layer(romData, context);
		
		currentIndex = -1;
		currentRomBackgroundIndex = -1;
		
		//loadBackgroundSet();
	}
	
	public void doTick()
	{
		bg3.doTick();
		bg4.doTick();
	}
	
	public Layer getBg3()
	{
		return bg3;
	}
	
	public Layer getBg4()
	{
		return bg4;
	}
	
	/**
	 * @return number of <b>unique</b> background layer combinations
	 */
	public int getNumberOfBackgrounds()
	{
		return layerTable.length;
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
		
		short[][] buffer = new short[MAX_ENTRIES][3];
		
		for(int i = 0; i < MAX_ENTRIES; i++)
		{
			short value0 = romData.getShort();
			short value1 = romData.getShort();
			
			if(blacklist.contains(Integer.valueOf(i))) continue;
			
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
				buffer[uniqueCount][2] = (short) i; // track natural Earthbound ROM index
				uniqueCount++;
			}
		}
		
		layerTable = new short[uniqueCount][3];
		System.arraycopy(buffer, 0, layerTable, 0, uniqueCount);
	}
	
	public void setIndex(int index)
	{
		if(currentIndex != index)
		{
			Log.d(TAG, "background group index: " + index);
			
			int layerA = layerTable[index][0];
			int layerB = layerTable[index][1];
			
			setLayers(layerA, layerB);
			currentIndex = index;
			currentRomBackgroundIndex = layerTable[index][2];
		}
	}
	
	public void setLayers(int A, int B)
	{
		bg3.loadLayer(A);
		bg4.loadLayer(B);
		Log.d(TAG, "loaded layers: " + A + ", " + B);
	}
	
	public int getCacheableImagesTotal()
	{
		int images = 103; // TODO: don't hardcode this
		
		return images;
	}
	
	public int getRomBackgroundIndex(int address)
	{
		//return currentRomBackgroundIndex;
		return layerTable[address][2];
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
