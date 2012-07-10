package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import android.util.Log;

public final class Cache
{
	public static final String TAG = "Cache";
	
	/**
	 * Reads a cached file of arbitrary size into a new buffer. Not 100% foolproof; there's
	 * a remote possibility of mobile platform unexpectedness, but then that's always the
	 * case isn't it? :)
	 * 
	 * @param cacheFile {@code File} object representing the cached data on the file system
	 * @return a byte array containing the cached file
	 */
	public static byte[] read(File cacheFile)
	{
		byte[] output = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(cacheFile);
	        int bytesRead = 0;
	        int count = (int)cacheFile.length(); // assuming no jokers try to read a 2GB cache file...
	        output = new byte[count];
	        while(bytesRead < count) {
	        	bytesRead += fileInputStream.read(output, bytesRead, count - bytesRead);
	        }
	        fileInputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, String.format("Cache file %s could not be found", cacheFile.getPath()));
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, String.format("There was a problem reading cache file %s", cacheFile.getPath()));
			e.printStackTrace();
		}
		
		return output;
	}
	
	/**
	 * Reads a cached file of known size directly into a preallocated byte array. Not 100%
	 * foolproof, etc.
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
		write(cacheFile, input, input.length);
	}
	
	public static void write(File cacheFile, byte[] input, int length)
	{
		File parentFile = cacheFile.getParentFile();
		
		if(parentFile != null) parentFile.mkdirs(); // safely does nothing if path exists
		
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
			fileOutputStream.write(input, 0, length);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, String.format("Cache file %s could not be found", cacheFile.getPath()));
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, String.format("There was a problem writing to cache file %s", cacheFile.getPath()));
			e.printStackTrace();
		}
	}
	
	
	
	public static void readCompressed(File cacheFile, byte[] output, int count)
	{
		
		try {
			FileInputStream fileInputStream = new FileInputStream(cacheFile);
			InflaterInputStream inflaterInputStream = new InflaterInputStream(fileInputStream);
	        int bytesRead = 0;
	        while(bytesRead < count) {
	        	bytesRead += inflaterInputStream.read(output, bytesRead, count - bytesRead);
	        }
	        inflaterInputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, String.format("Cache file %s could not be found", cacheFile.getPath()));
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, String.format("There was a problem reading cache file %s", cacheFile.getPath()));
			e.printStackTrace();
		}
	}
	
	public static void writeCompressed(File cacheFile, byte[] input)
	{
		cacheFile.getParentFile().mkdirs(); // safely does nothing if path exists
		
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
			DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(fileOutputStream);
			deflaterOutputStream.write(input, 0, input.length);
			deflaterOutputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
