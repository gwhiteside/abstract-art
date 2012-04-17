package net.georgewhiteside.android.abstractart;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class ServiceDialog extends Activity implements OnClickListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		String message = "";
		String title = "";
		
		Bundle extras = getIntent().getExtras();
		
		if(extras != null)
		{
			if(extras.containsKey("message"))
			{
				message = extras.getString("message");
			}
			
			if(extras.containsKey("title"))
			{
				title = extras.getString("title");
			}
		}
		
		Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(message);
		dialog.setPositiveButton("OK", this);
		dialog.setTitle(title);
		dialog.show();
	}

	public void onClick(DialogInterface arg0, int arg1)
	{
		finish();
	}
}
