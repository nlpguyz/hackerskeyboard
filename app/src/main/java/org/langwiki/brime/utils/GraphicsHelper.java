package org.langwiki.brime.utils;

import android.content.Context;

public class GraphicsHelper {
    public static float dpFromPx(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static int pxFromDp(final Context context, final float dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }
}
