package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.app.Application;

public class AbstractArt extends Application
{
	public AbstractArt()
	{
		
	}
	
	public ByteBuffer loadData(int rawResource)
	{
		// TODO: rewrite data loader
		ByteBuffer romData;
		InputStream input = getResources().openRawResource(rawResource);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		int bytesRead;
		byte[] buffer = new byte[16384];
		
		try {
			while((bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		romData = ByteBuffer.wrap(output.toByteArray());
		romData.order(ByteOrder.LITTLE_ENDIAN);
		
		return romData;
	}
}
