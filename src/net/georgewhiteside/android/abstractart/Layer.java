package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.preference.PreferenceManager;
import android.util.Log;

// TODO: scrolling bug on background 227?

// layers with second palette cycle: 60 61 (probably others, haven't checked them all)

/*
 * Note to self: don't pull the image-related stuff from Layer out into its own class without thinking it through very
 * carefully... the images themselves rely on data that belongs to Layer. Most notably, the bit depth information which
 * is necessary to even *decompress* an image (not just display it properly), is part of the Layer data.
 */

public class Layer
{
	private final String TAG = "Layer";
	private ByteBuffer romData;
	private static final int OFFSET = 0xA0200;
	
	private Context context;
	private SharedPreferences sharedPreferences;
	
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
	
	private int paletteId; // hack for disabled color effects
	
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
	
	public Layer(ByteBuffer data, Context context)
	{
		this.context = context;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		image = new byte[256 * 256 * 1];
		palette = new byte[16 * 1 * 4];
		romData = data;
		romData.order(ByteOrder.LITTLE_ENDIAN);
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
		return RomUtil.unsigned(bgData.get(8));
	}
	
	/**
	 * Gets the type of palette cycling animation for this layer:
	 * 
	 * <ul><li><code>0x00</code> - no cycling</li>
	 * <li><code>0x01</code> - rotate right</li>
	 * <li><code>0x02</code> - double rotate right</li>
	 * <li><code>0x03</code> - triangle rotation</ul>
	 * 
	 * @return the type of palette cycling animation for this layer
	 */
	public int getPaletteCycleType()
	{
		return bgData.get(3);
	}
	
	public int getPaletteRotation()
	{
		return paletteRotation;
	}
	
	public void loadLayer(int index)
	{
		if( index < 0 || index > 326 ) {
			return;
		}

		//Log.d(TAG, "layer index: " + index);

		// load background attribute data
		
		romData.position(0xADEA1 - OFFSET + index * 17);
		bgData = romData.slice().order(ByteOrder.LITTLE_ENDIAN);
		
		//Log.d(TAG, String.format("layer %d (image %d) bytes 3-8: %02X %02X %02X %02X %02X %02X", index, getImageIndex(), bgData.get(3), bgData.get(4), bgData.get(5), bgData.get(6), bgData.get(7), bgData.get(8)));
		
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
		
		
		
		// load color palette
		
		romData.position(0xADCD9 - OFFSET + getPaletteIndex() * 4);
		int pPaletteData = RomUtil.toHex(romData.getInt()) - OFFSET;
		paletteId = pPaletteData;  // hack for disabled color effects
		
		
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
		
		loadSubPalette(0);
		
		loadImage(getImageIndex());
		
		loadedIndex = index;
		paletteRotation = 0;
		mTick = 0;
		
		romData.rewind();
	}
	
	
	
	private void loadImage(int index)
	{
		// TODO: the image data ultimately needs to be a Buffer; change the byte[] image to a ByteBuffer
		
		boolean enablePaletteEffects = sharedPreferences.getBoolean("enablePaletteEffects", false); // SharedPreference
		String cacheFileName;
		File cacheDir;
		int bufferSize;
		
		if(enablePaletteEffects == true) {
			cacheFileName = String.valueOf(index);
			cacheDir = new File(context.getCacheDir(), "layers-indexed");
			bufferSize = 256 * 256 * 1;
			
		} else {
			cacheFileName = String.format("%03d-%d", index, paletteId); // hack for disabled color effects
			cacheDir = new File(context.getCacheDir(), "layers-rgba");
			bufferSize = 256 * 256 * 4;
		}
		
		if(image.length != bufferSize) {
			image = new byte[bufferSize];
		}
		
		File cacheFile = new File(cacheDir, cacheFileName);
		
		if(cacheFile.exists())
		{
			//Log.i(TAG, String.format("Reading previously cached image from %s", cacheFile.getPath()));
			
			if(enablePaletteEffects) {
				Cache.readCompressed(cacheFile, image, 256 * 256);
				//Cache.read(cacheFile, image, 256 * 256);
			} else {
				// TODO: integrate the nitty-gritty into the Cache class
				ByteBuffer buffer = ByteBuffer.allocate(256 * 256 * 4);
				
				// can cause a crash on rare occasions ... mainly when adding new features ;)
				// SO, just trapping any potential problems here so I don't get slowed down
				try {
					BitmapFactory.decodeFile(cacheFile.getPath()).copyPixelsToBuffer(buffer);
				}
				catch(Exception e) {
					Log.e("AbstractArt", "Couldn't open " + cacheFile.getPath() + " ... resuming with blank texture");
				}
				
				image = buffer.array();
			}
		}
		else
		{
			//Log.i(TAG, "Building image from compressed data");
			
			// decompress tiles, decompress tile arrangements, and assemble the former according to the latter
			// the 65,536-byte result is assigned to the byte[] image member of this instance
			
			prepareImageData(index);
			buildTiles();
			buildImage(enablePaletteEffects);
			
			//Log.i(TAG, String.format("Caching image to %s", cacheFile.getPath()));
			
			if(enablePaletteEffects) {
				Cache.writeCompressed(cacheFile, image);
				//Cache.write(cacheFile, image);
			} else {
				cacheFile.getParentFile().mkdirs(); // safely does nothing if path exists
	 			
	 			try {
	 				Bitmap img = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888); //Bitmap.createBitmap(buffer.asIntBuffer().array(), 256, 256, Bitmap.Config.ARGB_8888); // createBitmap(int[] colors, int width, int height, Bitmap.Config config)
	 				img.copyPixelsFromBuffer(ByteBuffer.wrap(image));
	 				FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
	 				img.compress(CompressFormat.PNG, 80, fileOutputStream); // quality is irrelevant for PNGs
	 				fileOutputStream.close();
	 			} catch (FileNotFoundException e) {
	 				e.printStackTrace();
	 			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void prepareImageData(int index)
	{
		// load tile graphics
		
		romData.position(0xAD9A1 - OFFSET + index * 4);
		int pTileData = RomUtil.toHex(romData.getInt()) - OFFSET;
		tileDataLength = RomUtil.decompress(pTileData, tileData, TILE_MAX, romData);
		
		// load tile arrangement data
		
		romData.position(0xADB3D - OFFSET + index * 4);
		int pArrangeData = RomUtil.toHex(romData.getInt()) - OFFSET;
		arrangeDataLength = RomUtil.decompress(pArrangeData, arrangeData, ARRANGE_MAX, romData);
	}
	
	public boolean checkIfCached(int imageNumber)
	{
		return false;
	}
	
	private void buildImage(boolean indexedColor)
	{
		int b1, b2;
		int block, tile, subpal = 0;
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

						int stride = 256;
						byte color = tiles.get(tile)[i][j];
						
						if(indexedColor) {
							int pos = px + py * stride;
							image[pos] = color;
						} else {
							int pos = px * 4 + py * 4 * stride;
							
							image[pos + 0] = paletteData[subpal][color][0];
							image[pos + 1] = paletteData[subpal][color][1];
							image[pos + 2] = paletteData[subpal][color][2];
							image[pos + 3] = color == 0 ? (byte)0x00 : (byte)0xFF;
							
						}
						
					}
				}
			}
		}
		//loadSubPalette(subpal);
	}
	
	private void loadSubPalette(int sub)
	{
		int subpal = sub; // just a temp thing
		for(int i = 0; i < (1 << getBPP()); i++)
		{
			palette[i * 4 + 0] = paletteData[subpal][i][0];
			palette[i * 4 + 1] = paletteData[subpal][i][1];
			palette[i * 4 + 2] = paletteData[subpal][i][2];
			palette[i * 4 + 3] = (byte)0xFF;
		}
		palette[3] = (byte)0x00; // opacity 0 for color 0
	}
	
	protected void buildTiles()
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
			loadLayer(0);
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
			loadLayer(0);
		}
		return palette;
	}
}
