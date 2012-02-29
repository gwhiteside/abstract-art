package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;

import android.util.Log;

/*

AF458

[AAAA BBBB CCCC DDDD EEEE] 10-byte entries (5 shorts)

AA AA - duration, given in 1/60ths of a second
BB BB - horizontal offset
CC CC - vertical offset
DD DD - horizontal acceleration
EE EE - vertical acceleration

Positive and negative directions:

      +
     
      |
+  ---+---  -
      |
     
      -
      
 */

// TODO: scrolling bug on some background changes; new layer scrolls for about a second (when it shouldn't) if the previous background had scrolling

public class Translation
{
	private ByteBuffer[] data = new ByteBuffer[4];
	private int mIndex;
	private int mNumberOfTranslations;
	
	private int mTranslationDuration;
	private float mHorizontalVelocity;
	private float mVerticalVelocity;
	private float mHorizontalOffset;
	private float mVerticalOffset;
	
	
	public Translation(ByteBuffer translationData, ByteBuffer translationIndices)
	{
		load(translationData, translationIndices);
	}
	
	public float getCurrentHorizontalOffset()
	{
		return mHorizontalOffset;
	}
	
	/*
	 * This is based on observation and rough guesswork; be warned when referencing this implementation
	 */
	public void doTick()
	{
		// x(t) = x0 + v0*t + 1/2*a*t^2

		if(mTranslationDuration != 0)
		{
			mTranslationDuration--;
			
			mHorizontalVelocity += (float)getHorizontalAcceleration() / 256.0f;
			mHorizontalOffset += mHorizontalVelocity;
			
			mVerticalVelocity += (float)getVerticalAcceleration() / 256.0f;
			mVerticalOffset += mVerticalVelocity;
			
			if(mTranslationDuration == 0)
			{
				float hcarry = mHorizontalOffset;
				float vcarry = mVerticalOffset;
				
				mIndex++;
				
				if(mIndex >= mNumberOfTranslations)
				{
					mIndex = 0;
					mHorizontalVelocity = 0;
					mVerticalVelocity = 0;
				}
				
				setIndex(mIndex);
				
				mHorizontalOffset = hcarry;
				mVerticalOffset = vcarry;
				
				
			}
		}
	}
	
	public void dump(int index)
	{
		//int original = index;
		//setIndex(index);
		Log.d("Translation", "duration: " + getDuration());
		Log.d("Translation", "horizontal offset: " + getHorizontalOffset());
		Log.d("Translation", "horizontal accel: " + getHorizontalAcceleration());
		Log.d("Translation", "vertical offset: " + getVerticalOffset());
		Log.d("Translation", "vertical accel: " + getVerticalAcceleration());
		//setIndex(original);
	}
	
	public int getDuration()
	{
		return ROMUtil.unsigned(data[mIndex].getShort(0));
	}
	
	public int getHorizontalOffset()
	{
		return data[mIndex].getShort(2);
	}
	
	public int getHorizontalAcceleration()
	{
		return data[mIndex].getShort(6);
	}
	
	public int getNumberOfTranslations()
	{
		return mNumberOfTranslations;
	}
	
	public int getVerticalOffset()
	{
		return data[mIndex].getShort(4);
	}
	
	public int getVerticalAcceleration()
	{
		return data[mIndex].getShort(8);
	}
	
	public void load(ByteBuffer translationData, ByteBuffer translationIndices)
	{
		mNumberOfTranslations = 0;
		for(int i = 0; i < 4; i++) {
			int index = ROMUtil.unsigned(translationIndices.get(i));
			if(index > 0)
				mNumberOfTranslations++;
			translationData.position(index * 10);
			data[i] = translationData.slice();
		}
		
		setIndex(0);
	}
	
	public void setIndex(int index)
	{
		if(index < 0 || index > 3)
			index = -1; // TODO exception
		
		mIndex = index;
		
		mTranslationDuration = getDuration();
		mHorizontalOffset = getHorizontalOffset();
		mVerticalOffset = getVerticalOffset();
	}
}
