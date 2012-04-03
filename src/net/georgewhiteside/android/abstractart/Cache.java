package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

public final class Cache
{
	public static final String TAG = "Cache";
	
	/**
	 * Reads a cached file of known size directly into a preallocated byte array. No
	 * buffering is done.
	 * 
	 * @param cacheFile {@code File} object representing the cached data on the file system
	 * @param output a preallocated byte array of {@code count} bytes
	 * @param count the exact length of the data being read
	 */
	public static void read(File cacheFile, byte[] output, int count)
	{
		try {
			FileInputStream fileInputStream = new FileInputStream(cacheFile);
	        int bytesRead = 0;
	        while(bytesRead < count) {
	        	bytesRead += fileInputStream.read(output, bytesRead, count);
	        }
	        fileInputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, String.format("Cache file %s could not be found", cacheFile.getPath()));
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, String.format("There was a problem reading cache file %s", cacheFile.getPath()));
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes data to the specified file location, overwriting any existing data.
	 * 
	 * @param cacheFile {@code File} object representing the output file path
	 * @param input the byte array to write to {@code cacheFile}
	 */
	public static void write(File cacheFile, byte[] input)
	{
		cacheFile.getParentFile().mkdirs(); // safely does nothing if path exists
		
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
			fileOutputStream.write(input);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, String.format("Cache file %s could not be found", cacheFile.getPath()));
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, String.format("There was a problem writing to cache file %s", cacheFile.getPath()));
			e.printStackTrace();
		}
	}
}
