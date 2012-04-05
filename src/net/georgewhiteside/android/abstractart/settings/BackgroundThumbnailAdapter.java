package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.GLOffscreenSurface;
import net.georgewhiteside.android.abstractart.Renderer;
import net.jsemler.utility.graphics3d.pixelbuffer.PixelBuffer;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

// TODO: add code path in Renderer to disregard frame limit when thumbnailing... otherwise it causes a silly slowdown :)

public class BackgroundThumbnailAdapter extends BaseAdapter {

	private Context context;
	private GLOffscreenSurface glOffscreenSurface;
	private Renderer renderer;
	private int thumbnailWidth, thumbnailHeight;
	
	public BackgroundThumbnailAdapter(Context context) {
		this.context = context;
		renderer = new Renderer(context);
		thumbnailWidth = 128; thumbnailHeight = 128;
		
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

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		
		if(convertView == null) {
			imageView = new ImageView(context);
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			imageView.setLayoutParams(new GridView.LayoutParams(thumbnailWidth, thumbnailHeight));
		} else {
			imageView = (ImageView)convertView;
		}
		
		renderer.loadBattleBackground(position);
		imageView.setImageBitmap(glOffscreenSurface.getBitmap());
		
		return imageView;
	}

}
