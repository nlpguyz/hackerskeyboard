package org.langwiki.brime.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.net.Uri;

import org.langwiki.brime.utils.AnimatedGifEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RenderManager {
    private static final String TAG = "BRime";
    private static RenderManager sInstance;
    private Context context;

    private static final int DEFAULT_TEXT_HEIGHT = 30;
    private static final int DEFAULT_TEXT_WIDTH = 400;
    private static final int DEFAULT_TEXT_SIZE = 12;

    private int textSize;

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
        this.textSize = DEFAULT_TEXT_SIZE;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getTextSize() {
        return textSize;
    }

    public Uri renderGif(String text, Typeface typeface) throws IOException {
        File outputDir = context.getCacheDir(); // context being the Activity pointer
        File outputFile = File.createTempFile("text", "gif", outputDir);

        int width = DEFAULT_TEXT_WIDTH;
        int height = DEFAULT_TEXT_HEIGHT;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawText(bmp, text, typeface, getTextSize());

        OutputStream fos = new FileOutputStream(outputFile);
        AnimatedGifEncoder.bitmapToStream(bmp, fos);

        return Uri.fromFile(outputFile);
    }

    /*
      Check out other text drawing code:
      https://android--examples.blogspot.com/2015/11/android-how-to-draw-text-on-canvas.html
     */
    void drawText(Bitmap originalBitmap, String text, Typeface typeface, int sizePx) {
        try {
            Canvas canvas = new Canvas(originalBitmap);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(typeface);
            paint.setTextSize(sizePx);

            paint.setColor(Color.WHITE); // Text Color
            //paint.setStrokeWidth(12); // Text Size
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern

            canvas.drawBitmap(originalBitmap, 0, 0, paint);
            canvas.drawText(text, 10, 10, paint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}