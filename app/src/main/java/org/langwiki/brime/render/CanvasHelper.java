package org.langwiki.brime.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;

public class CanvasHelper extends View {
 
        String mText;
        Context mContext;
        Paint paint = new Paint();
 
        public CanvasHelper(Context context) {
                super(context);
                mContext = context;
                paint.setColor(Color.WHITE); // You could setup the background etc here
                paint.setStyle(Paint.Style.FILL);
        }
 
        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                drawTextOnCanvas(canvas, mText);
        }
 
        private void drawTextOnCanvas(Canvas canvas, String text) {
                // maybe color the bacground..
                canvas.drawPaint(paint);
 
                // Setup a textview like you normally would with your activity context
                TextView tv = new TextView(mContext);
 
                // setup text
                tv.setText(mText);
 
                // maybe set textcolor
                tv.setTextColor(Color.BLACK);
 
                // you have to enable setDrawingCacheEnabled, or the getDrawingCache will return null
                tv.setDrawingCacheEnabled(true);
 
                // we need to setup how big the view should be..which is exactly as big as the canvas
                tv.measure(MeasureSpec.makeMeasureSpec(canvas.getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(canvas.getHeight(), MeasureSpec.EXACTLY));
 
                // assign the layout values to the textview
                tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
 
                // draw the bitmap from the drawingcache to the canvas
                canvas.drawBitmap(tv.getDrawingCache(), 0, 0, paint);
 
                // disable drawing cache
                tv.setDrawingCacheEnabled(false);
        }
 
        public void setText(String text) {
                this.mText = text;
        }
}
