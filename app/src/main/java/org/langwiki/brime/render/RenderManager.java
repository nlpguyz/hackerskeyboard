package org.langwiki.brime.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.langwiki.brime.utils.AnimatedGifEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*
  Check out other text drawing code:
  https://android--examples.blogspot.com/2015/11/android-how-to-draw-text-on-canvas.html
 */
public class RenderManager {
    private static final String TAG = "BRime";
    private static RenderManager sInstance;
    private Context context;

    private static final int DEFAULT_TEXT_MAX_WIDTH = 400;
    private static final int DEFAULT_TEXT_SIZE = 12;

    private int textMaxWidth;
    private int textSize;

    private int colorText;
    private int colorBackground;

    public static RenderManager getInstance(Context context) {
        if (sInstance != null)
            return sInstance;

        synchronized (RenderManager.class) {
            sInstance = new RenderManager(context);
            return sInstance;
        }
    }

    public RenderManager(Context context) {
        this.context = context;
        this.textMaxWidth = DEFAULT_TEXT_MAX_WIDTH;
        this.textSize = DEFAULT_TEXT_SIZE;

        this.colorBackground = Color.WHITE;
        this.colorText = Color.BLACK;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getTextSize() {
        return textSize;
    }

    public File renderGif(String text, Typeface typeface) throws IOException {
        Log.d(TAG, "renderGif " + text);
        // Use the cache directory (files will be automatically deleted)

        final File outputDir = new File(context.getFilesDir(), "images");
        outputDir.mkdirs();

        File outputFile = File.createTempFile("text", ".gif", outputDir);

        Bitmap bmp = drawText(text, typeface, getTextSize());

        OutputStream fos = new FileOutputStream(outputFile);
        AnimatedGifEncoder.bitmapToStream(bmp, fos);

        return outputFile;
    }

    // Draw a single-line text and return the bitmap
    private Bitmap drawText(String text, Typeface typeface, int sizePx) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(sizePx);

        // Text Color
        paint.setColor(Color.BLACK);

        // Text Overlapping Pattern
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(colorText);

        Bitmap bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);

        try {
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(colorBackground);
            canvas.drawText(text, 0, paint.getTextSize(), paint);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bmp;
    }

    // For drawing multiline text
    private void drawTextOnCanvas(Canvas canvas, Paint paint, String text) {
        // maybe color the bacground..
        canvas.drawPaint(paint);

        // Setup a textview like you normally would with your activity context
        TextView tv = new TextView(context);

        // setup text
        tv.setText(text);

        // maybe set textcolor
        tv.setTextColor(Color.BLACK);

        // you have to enable setDrawingCacheEnabled, or the getDrawingCache will return null
        tv.setDrawingCacheEnabled(true);

        // we need to setup how big the view should be..which is exactly as big as the canvas
        tv.measure(View.MeasureSpec.makeMeasureSpec(canvas.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(canvas.getHeight(),
                        View.MeasureSpec.EXACTLY));

        // assign the layout values to the textview
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

        // draw the bitmap from the drawingcache to the canvas
        canvas.drawBitmap(tv.getDrawingCache(), 0, 0, paint);

        // disable drawing cache
        tv.setDrawingCacheEnabled(false);
    }
}