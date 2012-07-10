package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

// 0xAF908 - OFFSET + bgData.get(13 + i) * 17

/*
  
AABB - duration
  CC - type					(0 - none?, 1 - horizontal, 2 - horizontal interlaced, 3 - vertical, 4 - unknown)
DDEE - frequency
FFGG - amplitude			(unit is 1/512ths of a pixel
  HH - unknown				see below
IIJJ - compression 			(vertically squish the whole thing by this many pixels)
KKLL - frequency delta		(frequency changes by this amount each tick)
MMNN - amplitude delta		(amplitude changes by this amount each tick)
  OO - speed
PPQQ - compression delta	(compression changes by this amount each tick)
  
7 backgrounds use the HH byte; the backgrounds and values are as follows (base 10):

bg		HH
---		--
21		2
221		2
223		2
224		10
227		2
255		2
295		10

*/


// TODO: is the cycle checking really necessary, or do the loops neatly cycle?
// TODO: what's up with 223? it starts out with a compression effect, then just slides diagonally nw then ne in a loop ... 222 is fine though... 227 seems to be messed up too

public class Distortion
{
	public static final byte NONE = 0;
	public static final byte HORIZONTAL = 1;
	public static final byte HORIZONTAL_INTERLACED = 2;
	public static final byte VERTICAL = 3;
	public static final byte UNKNOWN = 4;
	
	private ByteBuffer[] data = new ByteBuffer[4];
	private int mIndex;
	private int mNumberOfEffects;
	
	// variables to track distortion sequence cycling
	private int mEffectDuration;
	private int mCurrentEffect;
	
	private int mTick;
	
	public Distortion(ByteBuffer distortionData, ByteBuffer distortionIndices)
	{
		load(distortionData, distortionIndices);
		
	}
	
	public float computeShaderAmplitude()
	{
		// returns C1 * amplitude
		// where C1 = 1.0 / 512.0
		// where amplitude = u_ampl + u_ampl_delta * time
		double amplitude = getAmplitude() + getAmplitudeDelta() * mTick;
		amplitude /= 512.0;
		return (float)amplitude;
	}
	
	public float computeShaderCompression()
	{
		// returns compression
		// where compression = u_comp + u_comp_delta * time
		double compression = getCompression() + getCompressionDelta() * mTick;
		return (float)compression;
	}
	
	public float computeShaderFrequency()
	{
		// returns C2 * frequency
		// where C2 = 0.000095873799 = 8.0 * PI / (1024.0 * 256.0);
		// where frequency = u_freq + u_freq_delta * time
		
		double frequency = getFrequency() + getFrequencyDelta() * mTick;
		frequency *= 8.0 * Math.PI / (1024.0 * 256.0);
		return (float)frequency;
	}
	
	public float computeShaderSpeed()
	{
		// returns C3 * u_speed * u_tick
		// where C3 = PI / 120.0
		
		return (float)(Math.PI * getSpeed() * mTick) / 120.0f;
	}
	
	
	
	public void doTick()
	{
		if(mEffectDuration != 0)
		{
			mEffectDuration--;
			
			if(mEffectDuration == 0)
			{
				mCurrentEffect++;
				if(mCurrentEffect >= mNumberOfEffects)
				{
					mCurrentEffect = 0;
				}
				setIndex(mCurrentEffect); // also loads next mEffectDuration and resets mTick to 0
				
				//Log.d("Distortion", "effect change: " + mCurrentEffect);
				return;
			}
		}
		
		mTick++;
	}
	
	public void dump(int index)
	{
		//int original = index;
		//setIndex(index);
		Log.d("Distortion", "duration: " + getDuration());
		Log.d("Distortion", "type: " + getType());
		Log.d("Distortion", "frequency: " + getFrequency());
		Log.d("Distortion", "amplitude: " + getAmplitude());
		Log.d("Distortion", "unknown hh: " + getUnknownHH());
		Log.d("Distortion", "compression: " + getCompression());
		Log.d("Distortion", "frequency delta: " + getFrequencyDelta());
		Log.d("Distortion", "amplitude delta: " + getAmplitudeDelta());
		Log.d("Distortion", "speed: " + getSpeed());
		Log.d("Distortion", "compression delta: " + getCompressionDelta());
		Log.d("Distortion", "number of effects: " + getNumberOfEffects());
		//setIndex(original);
	}
	
	public void load(ByteBuffer distortionData, ByteBuffer distortionIndices)
	{
		mNumberOfEffects = 0;
		for(int i = 0; i < 4; i++) {
			int index = RomUtil.unsigned(distortionIndices.get(i));
			if(index > 0)
				mNumberOfEffects++;
			distortionData.position(index * 17);
			data[i] = distortionData.slice().order(ByteOrder.LITTLE_ENDIAN);
		}
		
		setIndex(0);
	}
	
	public int getAmplitude()
	{
		return RomUtil.unsigned(data[mIndex].getShort(5));
	}
	
	public int getAmplitudeDelta()
	{
		return data[mIndex].getShort(12);
	}
	
	public int getCompression()
	{
		return data[mIndex].getShort(8);
	}
	
	public int getCompressionDelta()
	{
		return data[mIndex].getShort(15);
	}
	
	public int getDuration()
	{
		return RomUtil.unsigned(data[mIndex].getShort(0));
	}
	
	public int getFrequency()
	{
		return RomUtil.unsigned(data[mIndex].getShort(3));
	}
	
	public int getFrequencyDelta()
	{
		return data[mIndex].getShort(10);
	}

	public int getIndex()
	{
		return mIndex;
	}
	
	public int getNumberOfEffects()
	{
		return mNumberOfEffects;
	}
	
	public int getSpeed()
	{
		return data[mIndex].get(14);
	}
	
	public int getType()
	{
		return data[mIndex].get(2);
	}
	
	public int getUnknownHH()
	{
		return data[mIndex].get(7);
	}
	
	public void setIndex(int index)
	{
		if(index < 0 || index > 3)
			index = -1; // TODO exception
		
		mIndex = index;
		
		mEffectDuration = getDuration();
		mCurrentEffect = getIndex();
		mTick = 0;
	}
	
	
}
