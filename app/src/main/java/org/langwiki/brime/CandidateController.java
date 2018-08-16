package org.langwiki.brime;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public class CandidateController {
    private static final String TAG = "BRime";

    // Test flags
    static final boolean TEST_POPUP_WINDOW = false;

    private InputMethodService mContext;
    private View mWindow;

    // The popup candidate window (for scroll candidates)
    // mCandidatePopupWindow --> candidateWindow (candidate_window.xml) --> MultilineCandidateView mCandidateView
    private PopupWindow mCandidatePopupWindow;
    private PopupWindow mImagePopopWindow; // For testing

    // The candidate view layout
    // mCandidateViewContainer (candidates.xml) <>--> candidate holder + expand button
    private LinearLayout mCandidateViewContainer;

    // The placeholder for showing candidates. The real candidates are shown in popup aligned to this view
    private View mCandidateViewPlaceholder;

    private ImageButton mCandidateExpandButton;

    // Holds the scroll candidate view and all candidates
    private MultilineCandidateView mCandidateView;

    public CandidateController(InputMethodService context) {
        mContext = context;
    }

    // Intelligent choice of candidate display method (in-place or pop-up)
    public void showCandidates() {
        if (mWindow == null) {
            Log.d(TAG, "showCandidates: mWindow is null");
            return;
        }

        // Finally, show the popup window at the center location of root relative layout
        // popupWindow.showAtLocation(anyViewOnlyNeededForWindowToken, Gravity.CENTER, 0, 0);
        // mCandidatePopupWindow.showAtLocation(mCandidateViewContainer, Gravity.TOP | Gravity.START,0,0);
        mCandidatePopupWindow.showAtLocation(
                mWindow,
                Gravity.NO_GRAVITY,0,0); // TODO move to row 2

        // Update location of the popup
        int pos[] = new int[2];
        mCandidateViewPlaceholder.getLocationInWindow(pos);
        Log.i(TAG, String.format("CandidateViewPlaceholder getLocationInWindow (%d, %d)", pos[0], pos[1]));

        if (TEST_POPUP_WINDOW) {
            mImagePopopWindow.showAtLocation(mWindow, Gravity.NO_GRAVITY, 0, 0);
        }
    }

    public View onCreateCandidatesView(View window) {
        mWindow = window;

        if (mCandidateViewContainer != null)
            return mCandidateViewContainer;

        //Log.i(TAG, "onCreateCandidatesView(), mCandidateViewContainer=" + mCandidateViewContainer);
        //mKeyboardSwitcher.makeKeyboards(true);

        // This is the candidate view layout
        // For scrolling candidate view, R.layout.candidates acts as a place holder, containing only
        // a TextView with default height. It can be used to show the first line of candidates, and
        // any button for toggling the scroll candidate view. A CandidateView may be added to it later.
        mCandidateViewContainer = (LinearLayout) mContext.getLayoutInflater().inflate(
                R.layout.candidates, null);

        mCandidateViewPlaceholder = mCandidateViewContainer.findViewById(R.id.candidates_placeholder);
        setCandidatesView(mCandidateViewContainer);

        mCandidateExpandButton = mCandidateViewContainer.findViewById(R.id.candidate_expand_button);
        mCandidateExpandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCandidateExpansion();
            }
        });

        // This is the window that contains a MultilineCandidateView which shows candidates
        View candidateWindow = mContext.getLayoutInflater().inflate(R.layout.candidate_window,null);
        mCandidateView = candidateWindow.findViewById(R.id.candidates);

        mCandidateView.setPadding(0, 0, 0, 0);
        // TODO check the interaction
        mCandidateView.setService((LatinIME) mContext);

        mCandidatePopupWindow = new PopupWindow(
                candidateWindow,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21){
            mCandidatePopupWindow.setElevation(5.0f);
        }

        if (TEST_POPUP_WINDOW) {
            // FIXME For testing popup windows. This should be removed and use "mCandidatePopupWindow" for popup scroll view
            View simpleImageView = mContext.getLayoutInflater().inflate(R.layout.simple_image_view, null);
            mImagePopopWindow = new PopupWindow(
                    simpleImageView,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            mImagePopopWindow.setElevation(5.0f);
        }

        return mCandidateViewContainer;
    }

    public MultilineCandidateView getCandidateView() {
        return mCandidateView;
    }

    // TODO check necessasity
    private void setCandidatesView(LinearLayout view) {
        mContext.setCandidatesView(view);
    }

    private void toggleCandidateExpansion() {
        mCandidateView.setExpanded(!mCandidateView.isExpanded());
    }

    public boolean hasContainer() {
        return mCandidateViewContainer != null;
    }

    public void removeContainer() {
        //Log.i(TAG, "removeCandidateViewContainer(), mCandidateViewContainer=" + mCandidateViewContainer);
        if (mCandidateViewContainer != null) {
            mCandidateViewContainer.removeAllViews();
            ViewParent parent = mCandidateViewContainer.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mCandidateViewContainer);
            }
            mCandidateViewContainer = null;
            mCandidateView = null;
        }
    }

    public boolean dismissAddToDictionaryHint() {
        return mCandidateView != null && mCandidateView.dismissAddToDictionaryHint();
    }
}
