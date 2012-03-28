package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

// TODO: this needs a lot of cleaning up

public class FrameRatePreference extends DialogPreference implements OnSeekBarChangeListener
{
	private int mMinimum = 5;
	private int mMaximum = 60;
	private int mStep = 5;
	
	private int currentFramerate;
	private int persistedFramerate;
	
	private SeekBar framerateSeekBar;
	private TextView framerateTextViewValue;

	public FrameRatePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.setPersistent(true);
		this.setDialogLayoutResource(R.layout.framerate_preference);
	}
	
	@Override
	protected void onBindDialogView(final View view) {
		super.onBindDialogView(view);
		
		framerateTextViewValue = (TextView)view.findViewById(R.id.framerate_textview_value);
		
		framerateSeekBar = (SeekBar)view.findViewById(R.id.framerate_seekbar);
		framerateSeekBar.setProgress(currentFramerate - mMinimum);
		framerateSeekBar.setOnSeekBarChangeListener(this);
		framerateSeekBar.setMax(mMaximum - mMinimum);
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);

		if(positiveResult) {
			if(callChangeListener(currentFramerate))
			{
				// user hit OK
				persistInt(currentFramerate);
				persistedFramerate = currentFramerate;
				notifyChanged();	// allow the description to automatically update
			}
		}
		else
		{
			// user cancelled
			currentFramerate = persistedFramerate;
		}
	}
	
	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index)
	{
		final int value = a.getInt(index, 0);
		currentFramerate = value;
		persistedFramerate = currentFramerate;
		Log.d("pref", "onGetDefaultValue: " + currentFramerate);
		return value;
	}
	
	@Override
	protected void onSetInitialValue(final boolean restore, final Object defaultValue) {
		currentFramerate = getPersistedInt(defaultValue == null ? 0 : (Integer)defaultValue);
		persistedFramerate = currentFramerate;
		Log.d("pref", "onSetInitialValue: " + currentFramerate);
	}
	
	/**
	 * Interpolate the current value into the summary string
	 */
	@Override
    public CharSequence getSummary()
	{
		String summary = super.getSummary().toString();
		int value = currentFramerate;//getPersistedInt(framerate);
		return String.format(summary, value);
    }
	
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		int rawValue = progress - (progress % mStep);
		int displayValue = rawValue + mMinimum;
		
		seekBar.setProgress(rawValue);
		
		framerateTextViewValue.setText(String.valueOf(displayValue) + " FPS");
		currentFramerate = displayValue;
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
