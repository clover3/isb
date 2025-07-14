package com.postech.isb.readThread;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EdgeEffect;
import android.widget.ScrollView;


public class MyScrollView extends ScrollView
{
	private EdgeEffect mRightEdge;
	private EdgeEffect mLeftEdge;
	private final int STATE_LEFT = -1;
	private final int STATE_NONE = 0;
	private final int STATE_RIGHT = +1;
	int mOpt;
	
	
	public MyScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		Init(context);
	}

    public MyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		Init(context);
    }
    
    private void Init(Context context)
    {
    	mOpt = STATE_NONE;
		mRightEdge = new EdgeEffect(context);
		mLeftEdge = new EdgeEffect(context);
		
    }
	@Override
	public void draw(Canvas canvas)
	{
		super.draw(canvas);

        if (mOpt == STATE_LEFT ) 
		{
        	final int restoreCount = canvas.save();
            final int height = getHeight() ;
            final int width = getWidth();

            canvas.rotate(270);
            canvas.translate(-height + getPaddingTop() - computeVerticalScrollOffset() , 0);
            mLeftEdge.setSize(height, width);
            mLeftEdge.onPull((float) 0.01);
			mLeftEdge.draw(canvas);
            canvas.restoreToCount(restoreCount);
		}
        else if (mOpt == STATE_RIGHT ) 
		{
            final int restoreCount = canvas.save();
            final int height = getHeight();
            final int width = getWidth();

            canvas.rotate(90);
            canvas.translate(-getPaddingTop() + computeVerticalScrollOffset(), (float) -(width));
            mRightEdge.setSize(height, width);
            mRightEdge.onPull((float) 0.01);
			mRightEdge.draw(canvas);
            canvas.restoreToCount(restoreCount);
		}
        else 
        {
        	mLeftEdge.onRelease();
        	mRightEdge.onRelease();
        	invalidate();
        }

	}	
	public void SetEffect(int opt)
	{
		mOpt = opt;
	}
}
