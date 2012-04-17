package net.georgewhiteside.android.abstractart.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import net.georgewhiteside.android.abstractart.ImageLoader;
import net.georgewhiteside.android.abstractart.ImageLoader.ImageLoadListener;
import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.Cache;
import net.georgewhiteside.android.abstractart.GLOffscreenSurface;
import net.georgewhiteside.android.abstractart.Renderer;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

// TODO: add code path in Renderer to disregard frame limit when thumbnailing... otherwise it causes a silly slowdown :)

public class ThumbnailAdapter extends BaseAdapter implements ImageLoadListener {

	private static final String TAG = "ThumbnailAdapter";
	private Context context;
	private LayoutInflater mInflater;
	private ImageLoader imageLoader;
	private Handler handler;
	
	private int thumbnailWidth, thumbnailHeight;
	private GLOffscreenSurface glOffscreenSurface;
	private Renderer renderer;
	
	private static final int LOADING_VIEW = 0;
	private static final int THUMBNAIL_VIEW = 1;
	
	public ThumbnailAdapter(Context context) {
		this.context = context;
		
		thumbnailWidth = 128; thumbnailHeight = 128;
		
		renderer = new Renderer(context);
		
		glOffscreenSurface = new GLOffscreenSurface(thumbnailWidth, thumbnailHeight);
		glOffscreenSurface.setEGLContextClientVersion(2);
		//glOffscreenSurface.setRenderer(renderer);
		
        mInflater = LayoutInflater.from(context); // Cache the LayoutInflater to avoid asking for a new one each time
        
        handler = new Handler();
        
        imageLoader = new ImageLoader(context, renderer, glOffscreenSurface, this);
        imageLoader.start();
        
		
	}
	
	public int getCount() {
		return renderer.getBackgroundsTotal();
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
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.thumbnail_layout, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.viewSwitcher = (ViewSwitcher) convertView.findViewById(R.id.thumbnail_view_switcher);
            holder.text = (TextView) convertView.findViewById(R.id.thumbnail_text);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail_image);
            
            FrameLayout frameLayout = (FrameLayout) convertView.findViewById(R.id.loading_layout);
            frameLayout.setLayoutParams(new ViewSwitcher.LayoutParams(thumbnailWidth, thumbnailHeight));
            
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }
        
        holder.index = position; // set the image index we want for the on-screen GridView element so onImageLoaded doesn't override it if it scrolls out of view
        
        
        
        String cacheFileName = String.valueOf(position); //String.format("%03d", index);
 		File cacheDir = new File(context.getCacheDir(), "thumbnails");
 		File cacheFile = new File(cacheDir, cacheFileName);
		
		Bitmap thumbnail = null;
		
		// hit up the image cache first
 		// if there's no cached copy, we've got to generate it
		
		if(cacheFile.exists())
 		{
 			holder.viewSwitcher.setDisplayedChild(THUMBNAIL_VIEW);
 			
 			Log.i(TAG, "reading thumbnail from disk cache");
 			thumbnail = BitmapFactory.decodeFile(cacheFile.getPath());
 			
 			// set the bitmap in the ImageView
			holder.thumbnail.setImageBitmap(thumbnail);
			
			//holder.text.setText(String.valueOf(position));
			holder.text.setText(String.valueOf(renderer.getRomBackgroundIndex(position)));
 			
 			// look into BitmapFactory.inPurgeable, probably not relevant once I cache the PNGs myself
 		}
		else
		{
			holder.viewSwitcher.setDisplayedChild(LOADING_VIEW);
	        imageLoader.queueImageLoad(position, holder);
		}
        
        

        // Bind the data efficiently with the holder.
 		//holder.text.setText(String.valueOf(position));
 		//holder.thumbnail.setImageBitmap(thumbnail);

        return convertView;
    }

    public static class ViewHolder {
    	int index;
    	ViewSwitcher viewSwitcher;
        TextView text;
        ImageView thumbnail;
    }

	public void onImageLoaded(final ViewHolder viewHolder, final Bitmap bitmap, final int position) {
		handler.post(new Runnable() {
			public void run() {
				
				// set the grid item to the image thumbnail only if it hasn't scrolled off screen and been recycled
				// this does, however, tie up the past scrolled thumbnails from showing up until the rest of the queue
				// catches up
				if(viewHolder.index == position)
				{
					// set the bitmap in the ImageView
					viewHolder.thumbnail.setImageBitmap(bitmap);
					
					viewHolder.text.setText(String.valueOf(position));
					
					// explicitly tell the view switcher to show the second view
					viewHolder.viewSwitcher.setDisplayedChild(THUMBNAIL_VIEW);
				}
			}
		});
	}
}




