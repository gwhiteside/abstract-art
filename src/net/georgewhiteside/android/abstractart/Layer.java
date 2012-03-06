package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

// TODO: scrolling bug on background 227?

public class Layer
{
	private final String TAG = "Layer";
	private ByteBuffer romData;
	private static final int OFFSET = 0xA0200;
	
	//private static final int ATTRIBUTES = 0xADEA1 - OFFSET;
	
	//private byte[] bgData = new byte[17];
	//private short[][] scrollingData = new short[4][5];
	//private byte[][] distortionData = new byte[4][17];
	
	private ByteBuffer bgData;
	
	// max sizes were computed in advance; no need to waste time making
	// multiple decompression passes to determine sizes, and no need
	// to constantly reallocate graphic buffers
	
	private static final int TILE_MAX = 0x3740;
	private static final int ARRANGE_MAX = 0x800; // every bg arrangement is this size actually
	private static final int PALETTE_MAX = 0x10; // palettes are either 2bpp or 4bpp
	
	private byte[] tileData = new byte[TILE_MAX];
	private byte[] arrangeData = new byte[ARRANGE_MAX];
	private byte[][][] palette = new byte[8][PALETTE_MAX][3];
	
	private int tileDataLength;
	private int arrangeDataLength;	// in case I want to dynamically allocate space
	
	private int loadedIndex = -1;
	
	private byte[] image;
 	
 	public static final int DIST_NONE = 0;
 	public static final int DIST_HORIZONTAL = 1;
 	public static final int DIST_INTERLACED = 2;
 	public static final int DIST_VERTICAL = 3;
 	public static final int DIST_UNKNOWN = 4;
 	
 	public int getImageIndex() { return bgData.get(0); }
 	public int getPaletteIndex() { return bgData.get(1); }
 	public int getBPP() { return bgData.get(2); }

 	// TODO: wrap these Distortion and Translation objects?
	public Distortion distortion;
	public Translation translation;
 	
	public Layer(ByteBuffer data)
	{
		image = new byte[256 * 256 * 4];
		romData = data;
	}
	
	public void load(int index)
	{
		if( index < 0 || index > 326 ) {
			return;
		}

		Log.d(TAG, "layer index: " + index);

		// load background attribute data
		
		romData.position(0xADEA1 - OFFSET + index * 17);
		bgData = romData.slice();
		
		for (int i = 0; i < 4; i++) {
			// bytes 9 - 12 are scrolling background effect indices in the 0xAF458 table; 10 bytes (5 shorts) each
			//romData.position(0xAF458 - OFFSET + bgData.get(9 + i) * 10);
			//scrollingData[i] = romData.asShortBuffer().slice();
			
			// bytes 13 - 16 are distortion type indices in the 0xAF908 table; 17 bytes each
			//romData.position(0xAF908 - OFFSET + bgData.get(13 + i) * 17);
			//distortionData[i] = romData.slice();
		}
		
		//Log.d(TAG, String.format("index: %d indices: %d %d %d %d", index, bgData.get(13), bgData.get(14), bgData.get(15), bgData.get(16)));
		
		romData.position(0xAF458 - OFFSET);
		bgData.position(9);
		if(translation == null)
			translation = new Translation(romData.slice(), bgData.slice());
		else
			translation.load(romData.slice(), bgData.slice());
		
		romData.position(0xAF908 - OFFSET);
		bgData.position(13);
		if(distortion == null)
			distortion = new Distortion(romData.slice(), bgData.slice());
		else
			distortion.load(romData.slice(), bgData.slice());
		
		//Log.d(TAG, String.format("bbg: %d: image %d: %02X %02X %02X %02X", index, getImageIndex(), distortionData[0].get(2), distortionData[1].get(2), distortionData[2].get(2), distortionData[3].get(2)));
		
		// load graphic tile data
		
		romData.position(0xAD9A1 - OFFSET + getImageIndex() * 4);
		int pTileData = ROMUtil.toHex(romData.getInt()) - OFFSET;
		tileDataLength = ROMUtil.decompress(pTileData, tileData, TILE_MAX, romData);
		
		// load tile arrangement data
		
		romData.position(0xADB3D - OFFSET + getImageIndex() * 4);
		int pArrangeData = ROMUtil.toHex(romData.getInt()) - OFFSET;
		arrangeDataLength = ROMUtil.decompress(pArrangeData, arrangeData, ARRANGE_MAX, romData);
		
		// load color palette
		
		romData.position(0xADCD9 - OFFSET + getPaletteIndex() * 4);
		int pPaletteData = ROMUtil.toHex(romData.getInt()) - OFFSET;
		
		// TODO: read palettes correctly?
				
		// values are packed RGB555:
		// 0BBBBBGG GGGRRRRR
		
		romData.position(pPaletteData);
		
		// c = color, p = palette
		for(int p = 0; p < 8; p++) {
			for(int c = 0; c < (1 << getBPP()); c++)
			{
				short color = romData.getShort();
				int r = 0, g = 0, b = 0;
				b = (color >> 10) & 0x1F;
				g = (color >> 5) & 0x1F;
				r = color & 0x1F;
				
				// scale to rgb888 values
				palette[p][c][0] = (byte)(r << 3 | r >> 2);
				palette[p][c][1] = (byte)(g << 3 | g >> 2);
				palette[p][c][2] = (byte)(b << 3 | b >> 2);
			}
		}
		
		BuildTiles();
		
		try {
			drawImage();
		} catch(Exception e) {
			//Log.e(TAG, String.format("bbg: %d: image: %d palette: %d subpalette: %d", index, getImageIndex(), getPaletteIndex(), _subpal));
			Log.e(TAG, e.getMessage());
			
		}
		
		loadedIndex = index;
		
		romData.rewind();
	}
	
	
	
	
	private void drawImage()
	{
		int b1, b2;
		int block, tile, subpal;
		int n;
		boolean vflip, hflip;
		
		//String tile_string = "";
		//String pal_string = "";
		
		// for every tile location
		for (int y = 0; y < 32; y++)
		{
			
			
			for (int x = 0; x < 32; x++)
			{
				// prepare the attributes
				n = y * 32 + x;

				b1 = arrangeData[n * 2];
				b2 = arrangeData[n * 2 + 1] << 8;
				block = b1 + b2;

				tile = block & 0x3FF;
				vflip = (block & 0x8000) != 0;
				hflip = (block & 0x4000) != 0;
				subpal = (block >> 10) & 7;
				
				
				
				//tile_string += String.format("%3X ", tile);
				//pal_string += String.format("%3X ", subpal);
				
				// TODO what am I doing wrong/different that I had to make this hack???
				// BEGIN HACK
				if(tile >= 0x80 && tile <= 0xFF) {
					tile += 0x100;
				}
				if(subpal == 7) {
					tile = block & 0xFF;
					subpal = 0;
				}
				// END HACK
				
				// and draw its pixels
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						int px = 0, py = 0;
						
						if (hflip)
						px = (x * 8) + 7 - i;
						else
						px = (x * 8) + i;

						if (vflip)
						py = (y * 8) + 7 - j;
						else
						py = (y * 8) + j;

						int rowstride = 4*256;
						int pos = (px * 4) + (py * rowstride);
						
						int index = tiles.get(tile)[i][j];
						
						image[pos + 0] = palette[subpal][index][0];
						image[pos + 1] = palette[subpal][index][1];
						image[pos + 2] = palette[subpal][index][2];
						image[pos + 3] = (byte)(index == 0 ? 0x00 : 0xFF);
					}
				}
			}
			//tile_string += "\n";
			//pal_string += "\n";
		}
		
		//Log.d(TAG, tile_string + "\n\n");
		//Log.d(TAG, pal_string);
	}
	
	private List<byte[][]> tiles;
	
	protected void BuildTiles()
	{
		int n = tileDataLength / (8 * getBPP());

		tiles = new ArrayList<byte[][]>();
		
		for (int i = 0; i < n; i++)
		{
			tiles.add(new byte[8][]);

			int o = i * 8 * getBPP();

			for (int x = 0; x < 8; x++)
			{
				tiles.get(i)[x] = new byte[8];
				for (int y = 0; y < 8; y++)
				{
					int c = 0;
					for (int bp = 0; bp < getBPP(); bp++)
						c += (((tileData[o + y * 2 + ((bp / 2) * 16 + (bp & 1))]) & (1 << 7 - x)) >> 7 - x) << bp;
					tiles.get(i)[x][y] = (byte)c;
				}
			}
		}
	}
	
	public byte[] getImage()
	{
		if(loadedIndex == -1)
		{
			load(0);
		}
		return image;
	}
	
	public int getIndex()
	{
		return loadedIndex;
	}
}
