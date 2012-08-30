package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

// TODO: this needs a lot of cleaning up

public class FrameratePreference extends DialogPreference
{
	private static final int mMinimum = 5;
	private static final int mMaximum = 60;
	private static final int mStep = 5;
	
	private String persistedValue;
	private int workingValue;
	
	private SeekBar framerateSeekBar;
	private TextView framerateTextViewValue;
	
	OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
		{
			int displayValue = progress + mMinimum / mStep;
			
			framerateTextViewValue.setText(String.valueOf(displayValue * mStep) + " FPS");
			workingValue = displayValue;
		}

		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	};
	
	
	public FrameratePreference(Context context) {
		this(context, null);
	}
	
	public FrameratePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FrameratePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		this.setDialogLayoutResource(R.layout.framerate_preference);
		
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
		
		setPersistent(true);
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		framerateTextViewValue = (TextView)view.findViewById(R.id.framerate_textview_value);
		
		workingValue = Integer.valueOf(persistedValue) / mStep;
		
		framerateSeekBar = (SeekBar)view.findViewById(R.id.framerate_seekbar);
		framerateSeekBar.setProgress(workingValue - mMinimum / mStep);
		framerateSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
		framerateSeekBar.setMax(mMaximum / mStep - 1);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);

		if(positiveResult) {
			String outValue = String.valueOf(workingValue * mStep);
			if(callChangeListener(outValue)) {
				setValue(outValue);
			}
		}
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray typedArray, int index)
	{
		return typedArray.getString(index);
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		String initialValue = restoreValue ? getPersistedString(null) : (String)defaultValue;
		setValue(initialValue);
	}
	
	private void setValue(String value) {
		persistString(value); // persists the value in the preferences
		persistedValue = value; // persists the value for this instance of the preference screen
	}
}
