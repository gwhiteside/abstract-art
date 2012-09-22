package net.georgewhiteside.android.abstractart.settings;

import java.util.List;

import net.georgewhiteside.android.abstractart.ImageLoader;
import net.georgewhiteside.android.abstractart.ImageLoader.ImageLoadListener;
import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.GLOffscreenSurface;
import net.georgewhiteside.android.abstractart.Renderer;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

// TODO: add code path in Renderer to disregard frame limit when thumbnailing... otherwise it causes a silly slowdown :)

public class ThumbnailAdapter extends BaseAdapter implements ImageLoadListener
{
	private LayoutInflater mInflater;
	private ImageLoader imageLoader;
	private Handler handler;
	
	private int thumbnailWidth, thumbnailHeight;
	private GLOffscreenSurface glOffscreenSurface;
	private Renderer renderer;
	
	private List<Integer> backgroundList;
	
	private static LruCache<Integer, Bitmap> lruCache;
	
	private static final int LOADING_VIEW = 0;
	private static final int THUMBNAIL_VIEW = 1;
	
	public static class ViewHolder {
    	public int index;
    	ViewSwitcher viewSwitcher;
        TextView text;
        ImageView thumbnail;
        ImageView thumbnailCheckmark;
    }
	
	public ThumbnailAdapter(Context context, List<Integer> backgroundList, boolean renderEnemies) {
		this.backgroundList = backgroundList;
		thumbnailWidth = 128; thumbnailHeight = 112;
		
		renderer = new Renderer(context);
		
		glOffscreenSurface = new GLOffscreenSurface(thumbnailWidth, thumbnailHeight);
		glOffscreenSurface.setEGLContextClientVersion(2);
		
        mInflater = LayoutInflater.from(context); // Cache the LayoutInflater to avoid asking for a new one each time
        
        handler = new Handler();
        
        
        imageLoader = new ImageLoader(context, renderer, glOffscreenSurface, this, renderEnemies);
        imageLoader.start();
	}
	
	public int getCount() {
		return 180; // TODO _NEWFIX 
		// TODO _NEWFIX return renderer.getBackgroundsTotal();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}
	
	/**
     * Make a view to hold each row.
     *
     * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/List14.html
     */
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // A ViewHolder keeps references to children views to avoid unnecessary calls to findViewById() on each row
        ViewHolder holder;

        if (convertView == null) // Create a new view
        {
            convertView = mInflater.inflate(R.layout.thumbnail_layout, null);
            
            // Creates a ViewHolder and stores references to the children views we want to bind data to
            holder = new ViewHolder();
            holder.viewSwitcher = (ViewSwitcher) convertView.findViewById(R.id.thumbnail_view_switcher);
            holder.text = (TextView) convertView.findViewById(R.id.thumbnail_text);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail_image);
            holder.thumbnailCheckmark = (ImageView) convertView.findViewById(R.id.thumbnail_checkmark);
            
            FrameLayout frameLayout = (FrameLayout) convertView.findViewById(R.id.loading_layout);
            frameLayout.setLayoutParams(new ViewSwitcher.LayoutParams(thumbnailWidth, thumbnailHeight));
            
            convertView.setTag(holder);
        }
        else // Recycle an old view
        {
            holder = (ViewHolder) convertView.getTag();
        }
 		
 		holder.index = position; // set the image index for the on-screen GridView element so onImageLoaded doesn't override it if it scrolls out of view
 		
 		if(lruCache == null) {
 			int safetyBuffer = 1024 * 1024;
 			double availableMemory = Math.max(getAvailableMemory() - safetyBuffer, 0);
 			int thumbnailBytes = thumbnailWidth * thumbnailHeight * 4;
 			// I have no idea how reasonable this calculation is (if at all)
 			int maxCacheThumbs = Math.min((int) Math.ceil(availableMemory / thumbnailBytes), getCount());
 			lruCache = new LruCache<Integer, Bitmap>(maxCacheThumbs);
 			Log.i("ThumbnailAdapter", "Created LruCache for up to " + maxCacheThumbs + " of " + getCount() + " thumbnails");
 		}
 		
 		Bitmap bitmap = lruCache.get(Integer.valueOf(position));
 		
 		if(bitmap != null) {
			setThumb(holder, bitmap, position);
 		} else {
	 		holder.viewSwitcher.setDisplayedChild(LOADING_VIEW);
	        imageLoader.queueImageLoad(position, holder);
 		}
        
        return convertView;
    }

    public void setBackgroundList(List<Integer> backgroundList) {
    	this.backgroundList = backgroundList;
    }

    public void updateCheckmark(ViewHolder viewHolder) {
    	if(backgroundList.contains(Integer.valueOf(viewHolder.index))) {
			viewHolder.thumbnailCheckmark.setVisibility(ImageView.VISIBLE);
    	} else {
    		viewHolder.thumbnailCheckmark.setVisibility(ImageView.INVISIBLE);
    	}
    }

	public void onImageLoaded(final ViewHolder viewHolder, final Bitmap bitmap, final int position) {
		handler.post(new Runnable() {
			public void run() {
				// set the grid item to the image thumbnail only if it hasn't scrolled off screen and been recycled
				if(viewHolder.index == position) {
					setThumb(viewHolder, bitmap, position);
				}
			}
		});
		
		lruCache.put(Integer.valueOf(position), bitmap);
	}
	
	private void setThumb(ViewHolder viewHolder, Bitmap bitmap, int position) {
		viewHolder.thumbnail.setImageBitmap(bitmap);
		// TODO _NEWFIX viewHolder.text.setText(String.valueOf(renderer.getRomBackgroundIndex(position)));
		updateCheckmark(viewHolder);
		viewHolder.viewSwitcher.setDisplayedChild(THUMBNAIL_VIEW);
	}
	
    double getAvailableMemory() {
    	double heapSize =  Runtime.getRuntime().totalMemory();
    	double heapRemaining = Runtime.getRuntime().freeMemory();   
    	double nativeUsage  =  Debug.getNativeHeapAllocatedSize();
    	double memoryAvailable = Runtime.getRuntime().maxMemory() - (heapSize - heapRemaining) - nativeUsage;
    	return memoryAvailable;
    }
	
	public void cleanup() {
		Log.i("ThumbnailAdapter", "Cleaning up...");
		imageLoader.stopThread();
	}
}
