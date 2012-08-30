package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;

public class TimerPicker extends DialogPreference {
	private EditText hoursEditText;
	private EditText minutesEditText;
	private EditText secondsEditText;
	
	private static final int MAX_VALUE = 24 * 60 * 60 * 1000;
	private static final int MIN_VALUE = 1 * 1000;
	
	private int maxHours = toHours(MAX_VALUE);
	private int maxMinutes = 59;
	private int maxSeconds = 59;
	
	private String currentValue;
	
	OnFocusChangeListener focusChangeListener = new OnFocusChangeListener() {
		public void onFocusChange(View view, boolean hasFocus) {
			
			if(hasFocus == false) {
				// focus moved away, so do some pretty-fication / visual sanity checks
				EditText editText = (EditText) view;
				int value = sanitize(editText);
				editText.setText(String.valueOf(value));
			}
		}
	};
	
	private int sanitize(EditText editText) {
		String stringValue = editText.getText().toString();
		int intValue = Integer.valueOf(stringValue.equals("") ? "0" : stringValue);
		
		if(editText == hoursEditText && intValue > maxHours) {
			intValue = maxHours;
		} else if(editText == minutesEditText && intValue > maxMinutes) {
			intValue = maxMinutes;
		} else if(editText == secondsEditText && intValue > maxSeconds) {
			intValue = maxSeconds;
		}
		
		return intValue;
	}
	
	public TimerPicker(Context context) {
		this(context, null);
	}
	
	public TimerPicker(Context context, AttributeSet attrs) {
		//this(context, attrs, 0);
		
		super(context, attrs);
		init();
	}

	public TimerPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		setDialogLayoutResource(R.layout.timer_picker);
		
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
		
		setPersistent(true);
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		hoursEditText = (EditText)view.findViewById(R.id.hoursEditText);
		minutesEditText = (EditText)view.findViewById(R.id.minutesEditText);
		secondsEditText = (EditText)view.findViewById(R.id.secondsEditText);
		
		hoursEditText.setOnFocusChangeListener(focusChangeListener);
		minutesEditText.setOnFocusChangeListener(focusChangeListener);
		secondsEditText.setOnFocusChangeListener(focusChangeListener);
		
		long value = Integer.valueOf(currentValue);
		
		int hours = toHours(value);
		int minutes = toMinutes(value);
		int seconds = toSeconds(value);
		
		hoursEditText.setText(String.valueOf(hours));
		minutesEditText.setText(String.valueOf(minutes));
		secondsEditText.setText(String.valueOf(seconds));
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if(positiveResult) {
			
			int hours = sanitize(hoursEditText);
			int minutes = sanitize(minutesEditText);
			int seconds = sanitize(secondsEditText);
			
			long delayMillis = toMilliseconds(hours, minutes, seconds);
			
			if(delayMillis < MIN_VALUE) {
				Log.e("CycleDelayPref", "Attempted to save a bad value: " + delayMillis);
				delayMillis = MIN_VALUE;
			}
			
			if(delayMillis > MAX_VALUE) {
				Log.e("CycleDelayPref", "Attempted to save a bad value: " + delayMillis);
				delayMillis = MAX_VALUE;
			}
			
			String outValue = String.valueOf(delayMillis);
			if(callChangeListener(outValue)) {
				setValue(outValue);
			}
		}
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray typedArray, int index) {
		return typedArray.getString(index);
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		String initialValue = restoreValue ? getPersistedString(null) : (String)defaultValue;
		setValue(initialValue);
	}
	
	private void setValue(String value) {
		persistString(value); // persists the value in the preferences
		currentValue = value; // persists the value for this instance of the preference screen
	}
	
	private int toHours(long milliseconds) {
		return (int) ((milliseconds / (1000 * 60 * 60)));
	}
	
	private int toMinutes(long milliseconds) {
		return (int) ((milliseconds / (1000 * 60)) % 60);
	}
	
	private int toSeconds(long milliseconds) {
		return (int) ((milliseconds / 1000) % 60);
	}
	
	private long toMilliseconds(int hours, int minutes, int seconds) {
		return hours * 1000 * 60 * 60 + minutes * 1000 * 60 + seconds * 1000;
	}
}
