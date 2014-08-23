package com.s16.drawing;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class PatternDrawable extends Drawable {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, Bitmap> mBitmapCache = new HashMap<Integer, Bitmap>();
	
	private final Paint mDrawPaint;
	private final Paint mPaint;
	private int mBitmapWidth = 0;
	private int mBitmapHeight = 0;
	private Bitmap mSrcBitmap;
	private Bitmap mBitmap;
	private int numRectanglesHorizontal;
	private int numRectanglesVertical;
	
	public PatternDrawable(Bitmap bitmap) {
		mDrawPaint = new Paint();
		mPaint = new Paint();
		if (bitmap != null) {
			mSrcBitmap = bitmap;
			mBitmapWidth = mSrcBitmap.getWidth();
			mBitmapHeight = mSrcBitmap.getHeight();
		}
	}
	
	public PatternDrawable(Resources res, int id) {
		mDrawPaint = new Paint();
		mPaint = new Paint();
		if ((res != null) && (id > 0)) {
			mSrcBitmap = BitmapFactory.decodeResource(res, id);
			mBitmapWidth = mSrcBitmap.getWidth();
			mBitmapHeight = mSrcBitmap.getHeight();
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, null, getBounds(), mDrawPaint);
	}

	@Override
	public void setAlpha(int alpha) {
		throw new UnsupportedOperationException("Alpha is not supported by this drawwable.");
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		throw new UnsupportedOperationException("ColorFilter is not supported by this drawwable.");
	}

	@Override
	public int getOpacity() {
		return 0;
	}
	
	@Override
	public int getIntrinsicWidth() {
        return mBitmapWidth;
    }
	
	@Override
	public int getIntrinsicHeight() {
        return mBitmapHeight;
    }
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		
		int height = bounds.height();
		int width = bounds.width();

		numRectanglesHorizontal = (int) Math.ceil((width / mBitmapWidth));
		numRectanglesVertical = (int) Math.ceil(height / mBitmapHeight);

		generatePatternBitmap();
	}

	private void generatePatternBitmap(){

		if(getBounds().width() <= 0 || getBounds().height() <= 0) return;
		if (mSrcBitmap == null) return;
		
		Bitmap bitmapCache = mBitmapCache.get(numRectanglesHorizontal * numRectanglesVertical);
		if (bitmapCache != null) {
			mBitmap = bitmapCache;
			return;
		}
		
		Bitmap bitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), mSrcBitmap.getConfig());
		Canvas canvas = new Canvas(bitmap);
		
		for (int i = 0; i <= numRectanglesVertical; i++) {
			for (int j = 0; j <= numRectanglesHorizontal; j++) {
				float left = j * mBitmapWidth;
				float top = i * mBitmapHeight;
				canvas.drawBitmap(mSrcBitmap, left, top, mPaint);
			}
		}
		
		mBitmapCache.put(numRectanglesHorizontal * numRectanglesVertical, bitmap);
		mBitmap = bitmap;
	}
}
