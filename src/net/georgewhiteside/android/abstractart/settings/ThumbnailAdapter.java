package net.georgewhiteside.android.abstractart.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

// TODO: add code path in Renderer to disregard frame limit when thumbnailing... otherwise it causes a silly slowdown :)

public class ThumbnailAdapter extends BaseAdapter {

	private static final String TAG = "ThumbnailAdapter";
	private Context context;
	private LayoutInflater mInflater;
	private GLOffscreenSurface glOffscreenSurface;
	private Renderer renderer;
	private int thumbnailWidth, thumbnailHeight;
	
	public ThumbnailAdapter(Context context) {
		this.context = context;
		renderer = new Renderer(context);
		thumbnailWidth = 128; thumbnailHeight = 128;
		
        mInflater = LayoutInflater.from(context); // Cache the LayoutInflater to avoid asking for a new one each time
		
		glOffscreenSurface = new GLOffscreenSurface(thumbnailWidth, thumbnailHeight);
		glOffscreenSurface.setEGLContextClientVersion(2);
		glOffscreenSurface.setRenderer(renderer);
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
            holder.text = (TextView) convertView.findViewById(R.id.thumbnail_text);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail_image);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }
        
        // hit up the image cache first
 		// if there's no cached copy, we've got to generate it
 		
 		String cacheFileName = String.valueOf(position); //String.format("%03d", index);
 		File cacheDir = new File(context.getCacheDir(), "thumbnails");
 		File cacheFile = new File(cacheDir, cacheFileName);
 		
 		Bitmap thumbnail = null;
 		
 		if(cacheFile.exists())
 		{
 			Log.i(TAG, "reading thumbnail from disk cache");
 			thumbnail = BitmapFactory.decodeFile(cacheFile.getPath());
 			
 			// look into BitmapFactory.inPurgeable, probably not relevant once I cache the PNGs myself
 		} else {
 			Log.i(TAG, "generating thumbnail");
 			renderer.loadBattleBackground(position);
 			thumbnail = glOffscreenSurface.getBitmap();
 			
 			cacheFile.getParentFile().mkdirs(); // safely does nothing if path exists
 			
 			try {
 				Log.i(TAG, "writing thumbnail to disk cache");
 				FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
 				thumbnail.compress(CompressFormat.PNG, 80, fileOutputStream); // quality is irrelevant for PNGs
 			} catch (FileNotFoundException e) {
 				e.printStackTrace();
 			}
 		}

        // Bind the data efficiently with the holder.
 		holder.text.setText(String.valueOf(position));
 		holder.thumbnail.setImageBitmap(thumbnail);

        return convertView;
    }

    static class ViewHolder {
        TextView text;
        ImageView thumbnail;
    }
}
