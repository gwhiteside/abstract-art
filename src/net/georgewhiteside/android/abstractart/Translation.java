package net.georgewhiteside.android.abstractart;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

/*
AF458

[AAAA BBBB CCCC DDDD EEEE] 10-byte entries (5 shorts)

AA AA - duration, given in 1/60ths of a second
BB BB - horizontal velocity
CC CC - vertical velocity
DD DD - horizontal acceleration
EE EE - vertical acceleration

Positive and negative directions:

      +
     
      |
+  ---+---  -
      |
     
      -
      
 */

public class Translation
{
	private ByteBuffer[] data = new ByteBuffer[4];
	private int mIndex;
	private int mNumberOfTranslations;
	
	private int mTranslationDuration;
	private float mHorizontalVelocity;
	private float mVerticalVelocity;
	private float mHorizontalAcceleration;
	private float mVerticalAcceleration;
	private float mHorizontalOffset;
	private float mVerticalOffset;
	private float mHorizontalInitial;
	private float mVerticalInitial;
	
	private int mTicker;
	
	public Translation(ByteBuffer translationData, ByteBuffer translationIndices)
	{
		load(translationData, translationIndices);
	}
	
	/**
	 * Calculates the total X offset for this frame
	 * @return X offset of layer
	 */
	public float getHorizontalOffset()
	{
		return mHorizontalOffset;
	}
	
	/**
	 * Calculates the total Y offset for this frame
	 * @return Y offset of layer
	 */
	public float getVerticalOffset()
	{
		return mVerticalOffset;
	}
	
	// translation effect
	
	// v(t) = v(0) + a*t
	// x(t) = x(0) + v(0)*t + 0.5*a*t^2
	
	//dx += (x_velocity / 256.0) * time + 0.5 * (x_acceleration / 256.0) * time * time;
	//dy += (y_velocity / 256.0) * time + 0.5 * (y_acceleration / 256.0) * time * time;
	
	/*
	 * This is based on observation and rough guesswork; be warned when referencing this implementation
	 */
	public void doTick()
	{
		// x(t) = x0 + v0*t + 1/2*a*t^2
		
		// translation effect
		//dx += (x_velocity / 256.0) * time + 0.5 * (x_acceleration / 256.0) * time * time;
		//dy += (y_velocity / 256.0) * time + 0.5 * (y_acceleration / 256.0) * time * time;
		
		// TODO make this a more efficient check, like once per setIndex()
		if(getHorizontalAcceleration() != 0 || getHorizontalVelocity() != 0 || getVerticalAcceleration() != 0 || getVerticalVelocity() != 0)
		{
			//float time = getDuration() - mTranslationDuration;
			float time = mTicker;
			
			//if(getDuration() != 0) time = mTicker % getDuration();
			//else time = mTicker;
			
			mHorizontalOffset = mHorizontalInitial + mHorizontalVelocity * time + 0.5f * mHorizontalAcceleration * time * time;
			mVerticalOffset = mVerticalInitial + mVerticalVelocity * time + 0.5f * mVerticalAcceleration * time * time;
			
			mTicker++;
		}
		
		if(mTranslationDuration != 0)
		{
			mTranslationDuration--;
			
			if(mTranslationDuration == 0)
			{
				mIndex++;
				
				mHorizontalInitial = mHorizontalOffset;
				mVerticalInitial = mVerticalOffset;
				
				if(mIndex >= mNumberOfTranslations)
				{
					mIndex = 0;
					mTicker = 0;
				}
				
				mTicker = 0; // bug fix?
				
				setIndex(mIndex);
			}
		}

		/*
		if(mTranslationDuration != 0)
		{
			mTranslationDuration--;
			
			float time = getDuration() - mTranslationDuration;
			
			//mHorizontalVelocity += (float)getHorizontalAcceleration() / 256.0f;
			mHorizontalVelocity = 0 + getHorizontalAcceleration() / 256.0f * time;
			mHorizontalOffset = 0 + getHorizontalVelocity() * time + 0.5f * getHorizontalAcceleration() * time * time;
			
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
			
		}*/
	}
	
	public void dump(int index)
	{
		//int original = index;
		//setIndex(index);
		Log.d("Translation", "duration: " + getDuration());
		Log.d("Translation", "horizontal velocity: " + getHorizontalVelocity());
		Log.d("Translation", "horizontal accel: " + getHorizontalAcceleration());
		Log.d("Translation", "vertical velocity: " + getVerticalVelocity());
		Log.d("Translation", "vertical accel: " + getVerticalAcceleration());
		//setIndex(original);
	}
	
	public int getDuration()
	{
		return RomUtil.unsigned(data[mIndex].getShort(0));
	}
	
	public int getHorizontalVelocity()
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
	
	public int getVerticalVelocity()
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
			int index = RomUtil.unsigned(translationIndices.get(i));
			if(index > 0)
				mNumberOfTranslations++;
			translationData.position(index * 10);
			translationData.order(ByteOrder.LITTLE_ENDIAN);
			data[i] = translationData.slice().order(ByteOrder.LITTLE_ENDIAN);
		}
		
		mHorizontalOffset = 0;
		mVerticalOffset = 0;
		mHorizontalInitial = 0;
		mVerticalInitial = 0;
		mTicker = 0;
		
		setIndex(0);
	}
	
	public void setIndex(int index)
	{
		if(index < 0 || index > 3)
			index = -1; // TODO exception
		
		mIndex = index;
		
		mTranslationDuration = getDuration();
		mHorizontalAcceleration = getHorizontalAcceleration() / 256.0f;
		mVerticalAcceleration = getVerticalAcceleration() / 256.0f;
		mHorizontalVelocity = getHorizontalVelocity() / 256.0f;
		mVerticalVelocity = getVerticalVelocity() / 256.0f;
	}
}
