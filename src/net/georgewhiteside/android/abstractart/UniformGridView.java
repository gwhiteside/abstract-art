package net.georgewhiteside.android.abstractart;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

public class UniformGridView extends GridView
{
	private int mColumnWidth = 0;
	
	public UniformGridView(Context context)
	{
		super(context);
	}
	
	public UniformGridView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public UniformGridView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	@Override
	public void setColumnWidth(int columnWidth)
	{
		super.setColumnWidth(columnWidth);
		
		if(columnWidth > 0)
		{
			mColumnWidth = columnWidth;
		}
	}
	
	@Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
    {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        if(xNew > 0 && mColumnWidth > 0)
        {
        	int width = getWidth();
            int columns = xNew / mColumnWidth;
            
            if(columns > 1)
            {
            	int paddingTop = ((View)this.getParent()).getPaddingTop();
	            int paddingLeft = ((View)this.getParent()).getPaddingLeft();
	            int paddingRight = ((View)this.getParent()).getPaddingRight();
	            
	            // can't seem to figure out how to calculate the extra view space... width % mColumnWidth
	            // was what I figured, and when the number of columns is as expected it *looks* perfect,
	            // but it causes the grid to drop a column way too soon when you start shrinking the spacing...
	            // subtracting the verticalscrollbarwidth lets me get much tighter, within a couple pixels of spacing,
	            // but it's still not quite right.
	            int extraViewSpace = width % mColumnWidth - getVerticalScrollbarWidth();
	            int extraColumnSpace = extraViewSpace / columns;
	            
	            setHorizontalSpacing(extraColumnSpace);
	            setVerticalSpacing(extraColumnSpace);
            }
        }
    }
}
