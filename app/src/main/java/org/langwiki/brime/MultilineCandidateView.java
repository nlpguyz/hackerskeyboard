/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.langwiki.brime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
   Make this view wrap after a width limit.

   TODO
     1. two pass drawing (d)
     2@. add two views into the window
         - add the two views as children
     2a. add an unexpanded view with a custom text view and a image view button
         - xml (d)
     2b. add an expanded view position below the unexpanded view and set its
         dimensions and scrollbar options
         - xml
     2c. drop candidate_preview.xml (use the two specialized versions instead)
     3. add an expand button on top-right corner (2a)
        a. when clicked on toggle visibility of the expanded view and the state of the
           button itself.
     4. handle selection
 */
public class MultilineCandidateView extends View {
    private final int WINDOW_ROWS = 6;

    private static final int OUT_OF_BOUNDS_WORD_INDEX = -1;

    private LatinIME mService;
    private final ArrayList<CharSequence> mSuggestions = new ArrayList<CharSequence>();
    private boolean mShowingCompletions;
    private CharSequence mSelectedString;
    private int mSelectedIndex;
    private int mTouchX = CandidateDrawer.OUT_OF_BOUNDS_X_COORD;
    private int mTouchY = CandidateDrawer.OUT_OF_BOUNDS_Y_COORD;

    private final TextView mPreviewText;
    private final PopupWindow mPreviewPopup;
    private int mCurrentWordIndex;

    private static final int MAX_SUGGESTIONS = 32;
    private static final int SCROLL_PIXELS = 20;

    private final int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private final int[] mWordX = new int[MAX_SUGGESTIONS];
    private final int[] mWordY = new int[MAX_SUGGESTIONS]; // NEW

    private int mPopupPreviewX;
    private int mPopupPreviewY;

    private boolean mScrolled;
    private boolean mShowingAddToDictionary;
    private CharSequence mAddToDictionaryHint;

    private int mTargetScrollX;
    private int mTargetScrollY; // NEW

    private final int mMinTouchableWidth;

    private boolean mTypedWordValid;
    private boolean mHaveMinimalSuggestion;

    // Full view size to hold all candidates (in px)
    // FIXME currently computed in onDraw. Better get some params from onDraw, and
    // compute on 'expand'
    private int mTotalWidth;
    private int mTotalHeight;

    private final GestureDetector mGestureDetector;
    private boolean mExpanded;

    // More data for multiline
    private CandidateDrawer mDrawer;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public MultilineCandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflate =
            (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Resources res = context.getResources();
        mPreviewPopup = new PopupWindow(context);
        mPreviewText = (TextView) inflate.inflate(R.layout.candidate_preview, null);
        mPreviewPopup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mPreviewPopup.setContentView(mPreviewText);
        mPreviewPopup.setBackgroundDrawable(null);
        mPreviewPopup.setAnimationStyle(R.style.KeyPreviewAnimation);
        mDrawer = new CandidateDrawer(context, mPreviewText.getTextSize(),
                mWordWidth,
                mWordX,
                mWordY);
        mAddToDictionaryHint = res.getString(R.string.hint_add_to_dictionary);

        mMinTouchableWidth = (int)res.getDimension(R.dimen.candidate_min_touchable_width);

        mGestureDetector = new GestureDetector(
                new CandidateStripGestureListener(mMinTouchableWidth));
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        scrollTo(0, getScrollY());

        mExpanded = false;
    }

    private class CandidateStripGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final int mTouchSlopSquare;

        public CandidateStripGestureListener(int touchSlop) {
            // Slightly reluctant to scroll to be able to easily choose the suggestion
            mTouchSlopSquare = touchSlop * touchSlop;
        }

        @Override
        public void onLongPress(MotionEvent me) {
            if (mSuggestions.size() > 0) {
                if (me.getX() + getScrollX() < mWordWidth[0] && getScrollX() < 10) {
                    longPressFirstWord();
                }
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            mScrolled = false;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            // TODO support vertical scroll
            if (!mScrolled) {
                // This is applied only when we recognize that scrolling is starting.
                final int deltaX = (int) (e2.getX() - e1.getX());
                final int deltaY = (int) (e2.getY() - e1.getY());
                final int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if (distance < mTouchSlopSquare) {
                    return true;
                }
                mScrolled = true;
            }

            final int width = getWidth();
            mScrolled = true;
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            scrollX += (int) distanceX;
            if (scrollX < 0) {
                scrollX = 0;
            }
            if (distanceX > 0 && scrollX + width > mTotalWidth) {
                scrollX -= (int) distanceX;
            }
            mTargetScrollX = scrollX;
            mTargetScrollY = scrollY;
            scrollTo(scrollX, getScrollY());
            hidePreview();
            invalidate();
            return true;
        }
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(LatinIME listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    public int computeVerticalScrollRange() {
        return mTotalHeight;
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     *
     * Draw the view and determine its total size. Apply X_LIMIT
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }

        boolean existsAutoCompletion = false;

        // NEW
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();

        mDrawer.setTouchXY(mTouchX, mTouchY);
        mDrawer.setScrolled(mScrolled, scrollX, scrollY);
        mDrawer.setSuggestions(mSuggestions);

        int[] sizes = mDrawer.measure(this, mMinTouchableWidth);
        mTotalWidth = sizes[0];
        mTotalHeight = sizes[1];

        CandidateDrawer.DrawStatus status = mDrawer.draw(this, canvas,
                mHaveMinimalSuggestion, mTypedWordValid, mShowingAddToDictionary);
        existsAutoCompletion = status.existsAutoCompletion;
        mSelectedString = status.selectedString;
        mSelectedIndex = status.selectIndex;

        if (!isInEditMode())
            mService.onAutoCompletionStateChanged(existsAutoCompletion);

        if (mTargetScrollX != scrollX || mTargetScrollY != scrollY) {
            scrollToTarget();
        }
    }

    private void scrollToTarget() {
        // TODO Handle Y scroll
        int scrollX = getScrollX();
        if (mTargetScrollX > scrollX) {
            scrollX += SCROLL_PIXELS;
            if (scrollX >= mTargetScrollX) {
                scrollX = mTargetScrollX;
                scrollTo(scrollX, getScrollY());
                requestLayout();
            } else {
                scrollTo(scrollX, getScrollY());
            }
        } else {
            scrollX -= SCROLL_PIXELS;
            if (scrollX <= mTargetScrollX) {
                scrollX = mTargetScrollX;
                scrollTo(scrollX, getScrollY());
                requestLayout();
            } else {
                scrollTo(scrollX, getScrollY());
            }
        }
        invalidate();
    }
    
    @SuppressLint("WrongCall")
    public void setSuggestions(List<CharSequence> suggestions, boolean completions,
                               boolean typedWordValid, boolean haveMinimalSuggestion) {
        clear();
        if (suggestions != null) {
            int insertCount = Math.min(suggestions.size(), MAX_SUGGESTIONS);
            for (CharSequence suggestion : suggestions) {
                mSuggestions.add(suggestion);
                if (--insertCount == 0)
                    break;
            }
        }
        mShowingCompletions = completions;
        mTypedWordValid = typedWordValid;
        scrollTo(0, getScrollY());
        mTargetScrollX = 0;
        mHaveMinimalSuggestion = haveMinimalSuggestion;
        // Compute the total width
        onDraw(null);
        invalidate();
        requestLayout();
    }

    public boolean isShowingAddToDictionaryHint() {
        return mShowingAddToDictionary;
    }

    public void showAddToDictionaryHint(CharSequence word) {
        ArrayList<CharSequence> suggestions = new ArrayList<CharSequence>();
        suggestions.add(word);
        suggestions.add(mAddToDictionaryHint);
        setSuggestions(suggestions, false, false, false);
        mShowingAddToDictionary = true;
    }

    public boolean dismissAddToDictionaryHint() {
        if (!mShowingAddToDictionary) return false;
        clear();
        return true;
    }

    /* package */ List<CharSequence> getSuggestions() {
        return mSuggestions;
    }

    public void clear() {
        // Don't call mSuggestions.clear() because it's being used for logging
        // in LatinIME.pickSuggestionManually().
        mSuggestions.clear();
        mTouchX = CandidateDrawer.OUT_OF_BOUNDS_X_COORD;
        mTouchY = CandidateDrawer.OUT_OF_BOUNDS_Y_COORD;
        mSelectedString = null;
        mSelectedIndex = -1;
        mShowingAddToDictionary = false;
        invalidate();
        Arrays.fill(mWordWidth, 0);
        Arrays.fill(mWordX, 0);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;
        mTouchY = y;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // TODO: For fling up, check delta instead of absolute pos
                // Fling up!?
                if (mSelectedString != null) {
                    // If there are completions from the application, we don't change the state to
                    // STATE_PICKED_SUGGESTION
                    if (!mShowingCompletions) {
                        // This "acceptedSuggestion" will not be counted as a word because
                        // it will be counted in pickSuggestion instead.
                        //TextEntryState.acceptedSuggestion(mSuggestions.get(0), mSelectedString);
                        //TextEntryState.manualTyped(mSelectedString);
                    }
                    mService.pickSuggestionManually(mSelectedIndex, mSelectedString);
                    mSelectedString = null;
                    mSelectedIndex = -1;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            // TODO: support clicking on all rows
            if (!mScrolled) {
                if (mSelectedString != null) {
                    if (mShowingAddToDictionary) {
                        longPressFirstWord();
                        clear();
                    } else {
                        if (!mShowingCompletions) {
                            //TextEntryState.acceptedSuggestion(mSuggestions.get(0), mSelectedString);
                            //TextEntryState.manualTyped(mSelectedString);
                        }
                        mService.pickSuggestionManually(mSelectedIndex, mSelectedString);
                        setExpanded(false);
                    }
                }
            }
            mSelectedString = null;
            mSelectedIndex = -1;
            requestLayout();
            hidePreview();
            invalidate();
            break;
        }
        return true;
    }

    private void hidePreview() {
        mTouchX = CandidateDrawer.OUT_OF_BOUNDS_X_COORD;
        mTouchY = CandidateDrawer.OUT_OF_BOUNDS_Y_COORD;
        mCurrentWordIndex = OUT_OF_BOUNDS_WORD_INDEX;
        mPreviewPopup.dismiss();
    }
    
    private void showPreview(int wordIndex, String altText) {
        int oldWordIndex = mCurrentWordIndex;
        mCurrentWordIndex = wordIndex;
        // If index changed or changing text
        if (oldWordIndex != mCurrentWordIndex || altText != null) {
            if (wordIndex == OUT_OF_BOUNDS_WORD_INDEX) {
                hidePreview();
            } else {
                CharSequence word = altText != null? altText : mSuggestions.get(wordIndex);
                mPreviewText.setText(word);
                mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int wordWidth = (int) (mDrawer.measureText(word, 0, word.length()) + mDrawer.mXGap * 2);
                final int popupWidth = wordWidth
                        + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight();
                final int popupHeight = mPreviewText.getMeasuredHeight();
                //mPreviewText.setVisibility(INVISIBLE);
                mPopupPreviewX = mWordX[wordIndex] - mPreviewText.getPaddingLeft() - getScrollX()
                        + (mWordWidth[wordIndex] - wordWidth) / 2;
                mPopupPreviewY = - popupHeight;
                int [] offsetInWindow = new int[2];
                getLocationInWindow(offsetInWindow);
                if (mPreviewPopup.isShowing()) {
                    mPreviewPopup.update(mPopupPreviewX, mPopupPreviewY + offsetInWindow[1], 
                            popupWidth, popupHeight);
                } else {
                    mPreviewPopup.setWidth(popupWidth);
                    mPreviewPopup.setHeight(popupHeight);
                    mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, mPopupPreviewX, 
                            mPopupPreviewY + offsetInWindow[1]);
                }
                mPreviewText.setVisibility(VISIBLE);
            }
        }
    }

    private void longPressFirstWord() {
        CharSequence word = mSuggestions.get(0);
        if (word.length() < 2) return;
        if (mService.addWordToDictionary(word.toString())) {
            showPreview(0, getContext().getResources().getString(R.string.added_word, word));
        }
    }
    
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hidePreview();
    }

    /**
     * Check if the multiline window is expanded.
     *
     * @return <code>true</code> if expanded.
     */
    public boolean isExpanded() {
        return mExpanded;
    }

    /**
     * Expand or collapse the the multiline window.
     * @param expanded <code>true</code> to expand, <code>false</code> to collapse.
     */
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;

        int[] fullSize;
        if (expanded) {
            int height = Math.min(mTotalHeight, WINDOW_ROWS * mDrawer.mRowHeight);
            fullSize = new int[] {LayoutParams.MATCH_PARENT, height};
        } else {
            fullSize = new int[] {LayoutParams.MATCH_PARENT, mDrawer.mRowHeight};
        }

        setLayoutParams(new LinearLayout.LayoutParams(fullSize[0], fullSize[1]));
    }
}
