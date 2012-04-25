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
	            int extraViewSpace = width % mColumnWidth - getVerticalScrollbarWidth();
	            int extraColumnSpace = extraViewSpace / columns;
	            
	            int testWidth = columns * mColumnWidth + extraColumnSpace * (columns - 0) + getVerticalScrollbarWidth();
	            int paddingTop = ((View)this.getParent()).getPaddingTop();
	            int paddingLeft = ((View)this.getParent()).getPaddingLeft();
	            int paddingRight = ((View)this.getParent()).getPaddingRight();
	            
	            Log.i("aa-debug", String.format("columns: %d extraViewSpace: %d extraColumnSpace: %d width: %d testWidth: %d", columns, extraViewSpace, extraColumnSpace, width, testWidth));
	            Log.i("aa-debug", "padding top: " + paddingTop + " padding left: " + paddingLeft + " padding right: " + paddingRight);
	            
	            setHorizontalSpacing(extraColumnSpace);
	            setVerticalSpacing(extraColumnSpace);
            }
        }
    }
}
