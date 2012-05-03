package net.georgewhiteside.android.abstractart;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import net.georgewhiteside.android.abstractart.settings.ThumbnailAdapter.ViewHolder;

/*
 * Asynchronous thumbnail loading class. I'm trying to keep the UI as smooth as possible.
 */
public class ThumbnailCache extends Thread
{
	/*public void queueImageLoad(final ViewHolder holder, final int position)
	{
		String cacheFileName = String.valueOf(position); //String.format("%03d", index);
 		File cacheDir = new File(context.getCacheDir(), "thumbnails");
 		File cacheFile = new File(cacheDir, cacheFileName);
		
		Bitmap thumbnail = null;
		
		// hit up the image cache first
 		// if there's no cached copy, we've got to generate it
		
		if(cacheFile.exists())
 		{
 			holder.viewSwitcher.setDisplayedChild(THUMBNAIL_VIEW);
 			
 			//Log.i(TAG, "reading thumbnail from disk cache");
 			thumbnail = BitmapFactory.decodeFile(cacheFile.getPath());
			
			setThumbnail(holder, thumbnail, position);
 		}
		else
		{
			holder.viewSwitcher.setDisplayedChild(LOADING_VIEW);
	        imageLoader.queueImageLoad(position, holder);
		}
	}*/
}
