package org.langwiki.brime;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.List;

class CandidateDrawer {
    public static final int OUT_OF_BOUNDS_X_COORD = -1;
    public static final int OUT_OF_BOUNDS_Y_COORD = -1;

    private final int mColorNormal;
    private final int mColorRecommended;
    private final int mColorOther;
    private final Paint mPaint;
    private final int mDescent;
    private List<CharSequence> mSuggestions;

    int mWordX[];
    int mWordY[];
    int mWordWidth[];
    boolean mWordAtRightBoundary[];

    private Rect mBgPadding;
    private Drawable mDivider;
    private int mTouchX;
    private int mTouchY;
    private boolean mScrolled;

    private final Drawable mSelectionHighlight;

    protected int mRowHeight;
    protected int mXGap;
    private int mXRightMargin;
    private int mScrollX;
    private int mScrollY;
    private int mStartY;

    protected static class DrawStatus {
        public CharSequence selectedString;
        public int selectIndex;
        public boolean existsAutoCompletion;
    }

    private DrawStatus mDrawStatus;

    public CandidateDrawer(Context context, float textSize, int[] mWordWidth, int[] mWordX, int[] mWordY) {
        this.mWordWidth = mWordWidth;
        this.mWordX = mWordX;
        this.mWordY = mWordY;
        this.mWordAtRightBoundary = new boolean[mWordWidth.length];

        Resources res = context.getResources();
        mColorNormal = res.getColor(R.color.candidate_normal);
        mColorRecommended = res.getColor(R.color.candidate_recommended);
        mColorOther = res.getColor(R.color.candidate_other);

        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(textSize * LatinIME.sKeyboardSettings.candidateScalePref);
        mPaint.setStrokeWidth(0);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mDescent = (int) mPaint.descent();

        mDivider = res.getDrawable(R.drawable.keyboard_suggest_strip_divider);

        mSelectionHighlight = context.getResources().getDrawable(
                R.drawable.list_selector_background_pressed);

        mRowHeight = res.getDimensionPixelSize(R.dimen.candidate_strip_height);
        mXGap = res.getDimensionPixelOffset(R.dimen.candidate_x_gap);
        mXRightMargin = res.getDimensionPixelOffset(R.dimen.candidate_window_right_margin);

        mDrawStatus = new DrawStatus();
    }

    public void setSuggestions(List<CharSequence> suggestions) {
        mSuggestions = suggestions;
    }

    public int[] measure(View view, int minTouchableWidth) {
        int mTotalWidth = 0;
        int mTotalHeight;

        // Limit for drawing. Only place widgets beyond the limit.
        int xLimit = view.getWidth() - mXRightMargin;

        // Create background padding (the padding between drawable and the text)
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (view.getBackground() != null) {
                view.getBackground().getPadding(mBgPadding);
            }
            mDivider.setBounds(0, 0, mDivider.getIntrinsicWidth(),
                    mDivider.getIntrinsicHeight());
        }

        final int count = mSuggestions.size();
        final Rect bgPadding = mBgPadding;
        final int touchX = mTouchX;
        final int touchY = mTouchY; // NEW

        // The start y for drawing text
        final int sy = (int) (mRowHeight + mPaint.getTextSize() - mDescent) / 2;
        mStartY = sy;

        int x = 0;
        int y = sy;
        int totalRows = 1;

        // measure pass
        mWordAtRightBoundary[count-1] = true;
        for (int i = 0; i < count; i++) {
            CharSequence suggestion = mSuggestions.get(i);
            if (suggestion == null) continue;
            final int wordLength = suggestion.length();

            // Measure word width
            int wordWidth;
            if ((wordWidth = mWordWidth[i]) == 0) {
                float textWidth =  mPaint.measureText(suggestion, 0, wordLength);
                wordWidth = Math.max(minTouchableWidth, (int) textWidth + mXGap * 2);
                mWordWidth[i] = wordWidth;
            }

            // Determine the position of the word
            boolean newLine = x + wordWidth >= xLimit;
            if (i >= 1)
                mWordAtRightBoundary[i - 1] = newLine;
            if (newLine) {
                x = 0;
                y += mRowHeight;
                totalRows++;
            }

            mWordX[i] = x;
            mWordY[i] = y;

            x += wordWidth;

            mTotalWidth = Math.max(mTotalWidth, x);
        }

        mTotalHeight = totalRows * mRowHeight;
        return new int[] { mTotalWidth, mTotalHeight };
    }

    // Returns true if there is autocompletion
    public DrawStatus draw(View view, Canvas canvas,
                        boolean hasMinimalSuggestion,
                        boolean typedWordValid,
                        boolean showAddToDictionary) {
        final boolean scrolled = mScrolled;
        mDrawStatus.existsAutoCompletion = false;

        int touchX = mTouchX;
        int touchY = mTouchY;

        int count = mSuggestions.size();
        int x, y;
        int sy = mStartY;

        // draw pass
        for (int i = 0; i < count; i++) {
            x = mWordX[i];
            y = mWordY[i];

            CharSequence suggestion = mSuggestions.get(i);
            if (suggestion == null) continue;
            final int wordLength = suggestion.length();
            final int wordWidth = mWordWidth[i];

            // Handle color
            mPaint.setColor(mColorNormal);
            if (hasMinimalSuggestion
                    && ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid))) {
                mPaint.setTypeface(Typeface.DEFAULT_BOLD);
                mPaint.setColor(mColorRecommended);
                mDrawStatus.existsAutoCompletion = true;
            } else if (i != 0 || (wordLength == 1 && count > 1)) {
                // HACK: even if i == 0, we use mColorOther when this suggestion's length is 1 and
                // there are multiple suggestions, such as the default punctuation list.
                mPaint.setColor(mColorOther);
            }

            // Draw the highlight
            // TODO process touchY (highlight on row > 1)
            boolean touchXHit = touchX != OUT_OF_BOUNDS_X_COORD && !scrolled
                    && touchX + mScrollX >= x && touchX + mScrollX < x + wordWidth;
            boolean touchYHit = touchY != OUT_OF_BOUNDS_X_COORD && !scrolled
                    && touchY + mScrollY >= y - sy && touchY + mScrollY < y + mRowHeight - sy;
            if (touchXHit && touchYHit) {
                if (canvas != null && !showAddToDictionary) {
                    canvas.translate(x, y - sy);
                    mSelectionHighlight.setBounds(0, mBgPadding.top, wordWidth, mRowHeight);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, -(y - sy));
                }
                mDrawStatus.selectedString = suggestion;
                mDrawStatus.selectIndex = i;
            }

            // Draw the text
            // resized
            if (canvas != null) {
                canvas.drawText(suggestion, 0, wordLength, x + wordWidth / 2, y, mPaint);
                if (!mWordAtRightBoundary[i]) {
                    mPaint.setColor(mColorOther);
                    canvas.translate(x + wordWidth, y - sy);
                    // Draw a divider unless it's after the hint
                    if (!(showAddToDictionary && i == 1)) {
                        mDivider.draw(canvas);
                    }
                    canvas.translate(-(x + wordWidth), -(y - sy));
                }
            }
            mPaint.setTypeface(Typeface.DEFAULT);
        }

        return mDrawStatus;
    }

    public void setTouchXY(int touchX, int touchY) {
        mTouchX = touchX;
        mTouchY = touchY;
    }

    public void setScrolled(boolean scrolled, int scrollX, int scrollY) {
        mScrolled = scrolled;
        mScrollX = scrollX;
        mScrollY = scrollY;
    }

    public float measureText(CharSequence word, int i, int length) {
        return mPaint.measureText(word, i, length);
    }
}
