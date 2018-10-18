package org.langwiki.brime;

import android.content.Context;
import android.util.AttributeSet;

public class VibratePreference extends SeekBarPreferenceString {
    public VibratePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void onChange(float val) {
        if (LatinIME.sInstance instanceof  LatinIME)
            return;
        LatinIME ime = (LatinIME) LatinIME.sInstance;
        if (ime != null) ime.vibrate((int) val);
    }
}