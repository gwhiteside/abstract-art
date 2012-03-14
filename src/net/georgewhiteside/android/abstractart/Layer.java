package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.util.Log;

// TODO: scrolling bug on background 227?

// layers with second palette cycle: 60 61 (probably others, haven't checked them all)

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
	private byte[][][] paletteData = new byte[8][PALETTE_MAX][3];
	
	private int tileDataLength;
	private int arrangeDataLength;	// in case I want to dynamically allocate space
	
	private int loadedIndex = -1;
	
	private List<byte[][]> tiles;
	private byte[] image;
	private byte[] palette;
	
	private int paletteRotation, triangle;
	private int mTick;
 	
 	public static final int DIST_NONE = 0x00;
 	public static final int DIST_HORIZONTAL = 0x01;
 	public static final int DIST_INTERLACED = 0x02;
 	public static final int DIST_VERTICAL = 0x03;
 	public static final int DIST_UNKNOWN = 0x04;
 	
 	public static final int CYCLE_NONE = 0x00;
	public static final int CYCLE_ROTATE1 = 0x01;
	public static final int CYCLE_ROTATE2 = 0x02;
	public static final int CYCLE_TRIANGLE = 0x03;
 	
 	public int getImageIndex() { return bgData.get(0); }
 	public int getPaletteIndex() { return bgData.get(1); }
 	public int getBPP() { return bgData.get(2); }

 	// TODO: wrap these Distortion and Translation objects?
	public Distortion distortion;
	public Translation translation;
	
	public Layer(ByteBuffer data)
	{
		image = new byte[256 * 256 * 1];
		palette = new byte[16 * 1 * 4];
		romData = data;
	}
	
	public void doTick()
	{
		distortion.doTick();
		translation.doTick();
		
		// handle palette cycling animation
		if(getPaletteCycleType() != Layer.CYCLE_NONE)
		{
			mTick++;
			if(mTick == getPaletteCycleSpeed())
			{
				mTick = 0;
				
				switch(getPaletteCycleType())
				{
					case Layer.CYCLE_ROTATE1:
					case Layer.CYCLE_ROTATE2:
						// TODO: for cycle type 0x02, if the lengths of both cycle ranges is not equal, this will give incorrect output...
						// I may need to figure something else out, but for now it's tricky because there's no integer modulus in the GLSL and I can't get it
						// to work consistently with floats. Sending the modified palette indices over isn't an attractive option either because that could take
						// up to 32 varyings, or one 16x2 texture upload every frame... maybe look into the palette texture re-ups, it might be feasible
						// OTHERWISE, I may want to consider adding an extra varying for each layer to track cycle rotation independently for type 0x02
						if(paletteRotation >= (getPaletteCycle1End() - getPaletteCycle1Begin() + 1))
						{
							paletteRotation = 1;
						}
						else
						{
							paletteRotation++;
						}
						
						break;
						
					case Layer.CYCLE_TRIANGLE:
						if(paletteRotation >= (getPaletteCycle1End() - getPaletteCycle1Begin() + 1) * 2)
						{
							paletteRotation = 1;
						}
						else
						{
							paletteRotation++;
						}
						break;
				}
			}
		}
	}
	
	public int getPaletteCycle1Begin()
	{
		return bgData.get(4);
	}
	
	public int getPaletteCycle1End()
	{
		return bgData.get(5);
	}
	
	public int getPaletteCycle2Begin()
	{
		return bgData.get(6);
	}
	
	public int getPaletteCycle2End()
	{
		return bgData.get(7);
	}
	
	public int getPaletteCycleSpeed()
	{
		return ROMUtil.unsigned(bgData.get(8));
	}
	
	/**
	 * Gets the type of palette cycling animation for this layer:
	 * 
	 * <ul><li><code>0x00</code> - no cycling</li>
	 * <li><code>0x01</code> - rotate right</li>
	 * <li><code>0x02</code> - double rotate right</li>
	 * <li><code>0x03</code> - triangle rotation</ul>
	 * 
	 * <p>There is very likely a difference between values <code>0x01</code> and <code>0x02</code>, but I haven't investigated it yet.</p>
	 * 
	 * @return the type of palette cycling animation for this layer
	 */
	public int getPaletteCycleType()
	{
		// seem to have: none, rotate right?, rotate right?, and triangle
		return bgData.get(3);
	}
	
	public int getPaletteRotation()
	{
		return paletteRotation;
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
		
		Log.d(TAG, String.format("layer %d bytes 3-8: %02X %02X %02X %02X %02X %02X", index, bgData.get(3), bgData.get(4), bgData.get(5), bgData.get(6), bgData.get(7), bgData.get(8)));
		
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
				paletteData[p][c][0] = (byte)(r << 3 | r >> 2);
				paletteData[p][c][1] = (byte)(g << 3 | g >> 2);
				paletteData[p][c][2] = (byte)(b << 3 | b >> 2);
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
		paletteRotation = 0;
		mTick = 0;
		
		romData.rewind();
	}
	
	private void drawImage()
	{
		int b1, b2;
		int block, tile, subpal;
		int n;
		boolean vflip, hflip;
		
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
				for (int i = 0; i < 8; i++)
				{
					for (int j = 0; j < 8; j++)
					{
						int px = 0, py = 0;
						
						if (hflip)
							px = (x * 8) + 7 - i;
						else
							px = (x * 8) + i;

						if (vflip)
							py = (y * 8) + 7 - j;
						else
							py = (y * 8) + j;

						int pos = px + (py * 256);
						
						image[pos] = tiles.get(tile)[i][j];
					}
				}
			}
		}
		
		// put together the palette image too
		
		subpal = 0; // just a temp thing
		for(int i = 0; i < (1 << getBPP()); i++)
		{
			palette[i * 4 + 0] = paletteData[subpal][i][0];
			palette[i * 4 + 1] = paletteData[subpal][i][1];
			palette[i * 4 + 2] = paletteData[subpal][i][2];
			palette[i * 4 + 3] = (byte)0xFF;
		}
		palette[3] = (byte)0x00; // opacity 0 for color 0
	}
	
	protected void BuildTiles()
	{
		int n = tileDataLength / (8 * getBPP());

		tiles = new ArrayList<byte[][]>();
		
		for (int i = 0; i < n; i++)
		{
			tiles.add(new byte[8][]);

			int o = i * 8 * getBPP();

			for (int y = 0; y < 8; y++)
			{
				tiles.get(i)[y] = new byte[8];
				for (int x = 0; x < 8; x++)
				{
					int c = 0;
					for (int bp = 0; bp < getBPP(); bp++)
					{
						c += (( (tileData[o + x * 2 + ((bp / 2) * 16 + (bp & 1))]) & (1 << 7 - y)) >> 7 - y) << bp;
					}
					tiles.get(i)[y][x] = (byte)c;
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
	
	public byte[] getPalette()
	{
		if(loadedIndex == -1)
		{
			load(0);
		}
		return palette;
	}
}
