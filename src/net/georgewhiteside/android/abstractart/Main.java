package net.georgewhiteside.android.abstractart;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class Main extends Activity
{
	private GLSurfaceView mGLView;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mGLView = new AbstractArtSurfaceView(this);
        setContentView(mGLView);
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	mGLView.onPause();
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	mGLView.onResume();
    }
}

class AbstractArtSurfaceView extends GLSurfaceView implements OnGestureListener
{
	private net.georgewhiteside.android.abstractart.Renderer mRenderer;
	private GestureDetector mDetector;
	
	public AbstractArtSurfaceView( Context context )
	{
		super( context );
		setEGLContextClientVersion( 2 );
		mDetector = new GestureDetector( this );
		mRenderer = new net.georgewhiteside.android.abstractart.Renderer( context );
		setRenderer( mRenderer );
	}

	public boolean onDown( MotionEvent e )
	{
		queueEvent( new Runnable()
		{
			public void run()
			{
				mRenderer.RandomBackground();
			}
		});
		
		return false;
	}

	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void onLongPress( MotionEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void onShowPress( MotionEvent e )
	{
		// TODO Auto-generated method stub
		
	}

	public boolean onSingleTapUp( MotionEvent e )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean onTouchEvent( final MotionEvent event )
	{
		mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
}