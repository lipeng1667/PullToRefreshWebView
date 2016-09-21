package com.microwu.ptrwebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/***************************************************************************************************
 * File Description:
 *
 * Created by   Michael Lee            lipeng@microwu.com           9/20/16 17:26
 * CopyRight    北京小悟科技有限公司      http://www.microwu.com
 *
 * Classes And Methods:
 *
 *
 * Updated History:
 * Author       Date            Content
/**************************************************************************************************/
public class PullToRefreshWebView extends RelativeLayout implements View.OnTouchListener {
    /**
     * for log message output
     */
    private final String TAG = "PullToRefreshWebView";

    /**
     * webview object
     */
    private WebView web_view_;

    /**
     * current context
     */
    private Context current_context_;

    /**
     * current url is loading
     */
    private String current_url_;

    /**
     * allow pull-to-refresh, modify it's value in webview's onScrollChanged method.
     */
    private boolean can_pull_to_refresh_ = true;

    /**
     * pulling down state
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;

    /**
     * release to refresh state
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;

    /**
     * refreshing state
     */
    public static final int STATUS_REFRESHING = 2;

    /**
     * finished state
     */
    public static final int STATUS_REFRESH_FINISHED = 3;

    /**
     * current refresh state
     */
    private int current_status_ = STATUS_REFRESH_FINISHED;

    /**
     * when use press on screen, record the location of y coordinate.
     */
    private float pressed_coords_y;

    /**
     * header view and it's layout parameters
     */
    private View header_;
    private MarginLayoutParams header_layout_params_;

    /**
     * height of the header
     */
    private int hide_header_height_;

    /**
     * header's original location's y coordinate, to check if it's able to pull
     */
    private float original_location_y_;

    /**
     * record the web_view's coordinate
     */
    private int[] location_array_ = new int[2];

    /**
     * trigger for only load once in {@link #onLayout(boolean, int, int, int, int)}
     */
    private boolean load_once_ = false;

    /**
     * max distance the finger can move
     */
    private int touchSlop;

    /**
     * record latest status
     */
    private int last_status_;

    /**
     * Description: Default Constructor
     * Created by Michael Lee on 9/21/16 10:41
     */
    public PullToRefreshWebView(Context aContext, AttributeSet attrs) {
        super(aContext,attrs);
        LayoutInflater.from(aContext).inflate(R.layout.sample_pull_to_refresh_web_view,this);
        current_context_ = aContext;

        // pull to refresh header
        header_ = findViewById(R.id.base_web_view_header_layout);
        touchSlop = ViewConfiguration.get(aContext).getScaledTouchSlop();
    }

    /**
     * Description: init the webview object
     *  We add webView object into view with codes but not in XML, the benefits is that we can release
     *  the memory more deeply and clearly when we destroy webview's instance.
     * Created by Michael Lee on 9/21/16 10:47
     * @param   aContext        current context
     */
    private void initWebView(Context aContext) {
        RelativeLayout webview_container = (RelativeLayout) findViewById(R.id.base_web_view_container);
        web_view_ = new MyWebView(aContext,null);
        web_view_.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        web_view_.setOnTouchListener(this);

        webview_container.addView(web_view_);
    }

    /**
     * Description: this method is 'public' for outer class to load url into progressWebView
     *  manually.
     *  In some case, outer class may want to load url into webView.
     *  For example, when login success, the inside activity should refresh the url before goto login.
     * Created by Michael Lee on 9/21/16 10:41
     * @param   url     url for navigating
     */
    public void loadUrl(String url) {
        if (web_view_ == null && current_context_ != null) {
            Log.d(TAG,"current web view has not be init, do it now");
            initWebView(current_context_);
        }
        if (url != null && !url.equals("")) {
            web_view_.loadUrl(url);
        } else {
            Log.e(TAG,"Url navigating is NULL or empty");
        }
    }

    /**
     * Description: webView's canGoBack
     * Created by Michael Lee on 9/21/16 15:26
     * @return  can go back? true = yes, false = no.
     */
    public boolean canGoBack() {
        return web_view_.canGoBack();
    }

    /**
     * Description: webView's goBack
     * Created by Michael Lee on 9/21/16 15:30
     */
    public void goBack() {
        if (canGoBack()) {
            web_view_.goBack();
        } else {
            Log.d(TAG,"Can NOT go back anymore");
        }
    }

    /**
     * Description: see {@link LinearLayout#onLayout(boolean, int, int, int, int)}
     *  After views are initialized, get web_view's location in screen to check if able to pull_to_
     *  refresh. And these codes only be execute once.
     * Created by Michael Lee on 9/21/16 15:46
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l,t,r,b);
        if (changed && !load_once_) {
            // confirm the WebView's original location
            web_view_.getLocationOnScreen(location_array_);
            original_location_y_ = location_array_[1];

            header_layout_params_ = (MarginLayoutParams) header_.getLayoutParams();
            hide_header_height_ = -header_.getHeight();

            Log.d(TAG,"onLayout. WebView's locationY = " + original_location_y_ + "; It's " +
                    "height is : " + hide_header_height_);
            load_once_ = true;
        }
    }

    /**
     * Description: update header's view
     * Created by Michael Lee on 9/21/16 16:23
     */
    private void updateHeaderView() {
        if (last_status_ != current_status_) {
            if (current_status_ == STATUS_PULL_TO_REFRESH) {
                ((TextView) findViewById(R.id.pull_to_refresh_description))
                        .setText(getResources().getString(R.string.pull_to_refresh));
                findViewById(R.id.pull_to_refresh_arrow).setVisibility(VISIBLE);
                findViewById(R.id.pull_to_refresh_progress_bar).setVisibility(GONE);
                rotateArrow();
            } else if (current_status_ == STATUS_RELEASE_TO_REFRESH) {
                ((TextView) findViewById(R.id.pull_to_refresh_description))
                        .setText(getResources().getString(R.string.release_to_refresh));
                findViewById(R.id.pull_to_refresh_arrow).setVisibility(VISIBLE);
                findViewById(R.id.pull_to_refresh_progress_bar).setVisibility(GONE);
                rotateArrow();
            } else if (current_status_ == STATUS_REFRESHING) {
                ((TextView) findViewById(R.id.pull_to_refresh_description))
                        .setText(getResources().getString(R.string.refreshing));
                findViewById(R.id.pull_to_refresh_arrow).clearAnimation();
                findViewById(R.id.pull_to_refresh_arrow).setVisibility(GONE);
                findViewById(R.id.pull_to_refresh_progress_bar).setVisibility(VISIBLE);
            }
        }
    }

    /**
     * Description: update arrow's state
     * Created by Michael Lee on 9/21/16 16:24
     */
    private void rotateArrow() {
        ImageView arrow = (ImageView) findViewById(R.id.pull_to_refresh_arrow);
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        float fromDegrees = 0f;
        float toDegrees = 0f;
        if (current_status_ == STATUS_PULL_TO_REFRESH) {
            fromDegrees = 180f;
            toDegrees = 360f;
        } else if (current_status_ == STATUS_RELEASE_TO_REFRESH) {
            fromDegrees = 0f;
            toDegrees = 180f;
        }
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(150);
        animation.setFillAfter(true);
        arrow.startAnimation(animation);
    }

    /**
     * Description: refreshing state
     *  1. set top margin
     *  2. show progress view and hide arrow
     *  3. reload webview
     * Created by Michael Lee on 9/21/16 16:24
     */
    private void refreshingAction() {
        header_layout_params_.topMargin = 0;
        header_.setLayoutParams(header_layout_params_);
        current_status_ = STATUS_REFRESHING;
        Log.i(TAG,"current url is : " + current_url_);
        loadUrl(current_url_);
    }

    /**
     * Description: hide the header
     * Created by Michael Lee on 9/21/16 16:24
     */
    private void hideHeader() {
        header_layout_params_.topMargin = hide_header_height_;
        header_.setLayoutParams(header_layout_params_);
        current_status_ = STATUS_REFRESH_FINISHED;
    }

    /**
     * Description: View.OnTouchListener
     * Created by Michael Lee on 9/21/16 16:23
     * @param   v       touched view
     * @param   event   motion event
     * @return 'false' means do nothing, 'true' means do custom actions.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        web_view_.getLocationOnScreen(location_array_);
        // Log.i(TAG,"current locationY is " + location_array_[1] + ", original location is : " +
        //         original_location_y_ );
        // we should reduce the original coordinate by 1, cause when convert float to int, there will
        // be 1 pix error.
        if ((original_location_y_ - 1) <= location_array_[1] && can_pull_to_refresh_) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressed_coords_y = event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float y_move = event.getRawY();
                    int distance = (int) (y_move - pressed_coords_y);

                    if (distance <= 0 && header_layout_params_.topMargin <= hide_header_height_) {
                        return false;
                    }

                    // distance is too small
                    if (distance < touchSlop) {
                        return false;
                    }

                    if (current_status_ != STATUS_REFRESHING) {
                        if (header_layout_params_.topMargin > 0) {
                            current_status_ = STATUS_RELEASE_TO_REFRESH;
                        } else {
                            current_status_ = STATUS_PULL_TO_REFRESH;
                        }
                        // add the header's top margin
                        header_layout_params_.topMargin = (distance / 5 * 2) + hide_header_height_;
                        // LogUtils.d(TAG,"header layout's top margin is : " +
                        //        header_layout_params_.topMargin);
                        header_.setLayoutParams(header_layout_params_);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                default:
                    // when release the finger
                    if (current_status_ == STATUS_RELEASE_TO_REFRESH) {
                        refreshingAction();
                    } else {
                        // hide header
                        hideHeader();
                    }
                    break;
            }

            if (current_status_ != STATUS_REFRESH_FINISHED) {
                updateHeaderView();
                last_status_ = current_status_;
                return true;
            }
        }
        return false;
    }

    /**---------------------------------------------------------------------------------------------
     * Description: mywebview extends {@link WebView}
     *  override onScrollChanged method to set if we should allow pull-to-refresh, cause when user
     *  is navigating the web page, we should NOT show the pull-to-refresh header.
     *--------------------------------------------------------------------------------------------*/
    public class MyWebView extends WebView {
        /**
         * Description: Default Constructor
         * Created by Michael Lee on 9/21/16 17:13
         * @param   context     context
         */
        public MyWebView(Context context) {
            this(context,null);
        }

        /**
         * Description: copy constructor.
         *  Do NOT invoke this(context, attrs,0), cause if style type was 0, would not response click
         *  event, and if type is 'com.android.internal.R.attr.webViewStyle', there would be on compile
         *  error:
         *  You cannot access id's of com.android.internal.R at compile time, but you can access the
         *  defined internal resources at runtime and get the resource by name.
         *  You should be aware that this is slower than direct access and there is no guarantee.
         * Created by Michael Lee on 9/21/16 17:13
         * @param   context     context
         * @param   attrs       attributes Set
         */
        public MyWebView(Context context, AttributeSet attrs) {
            this(context, attrs, Resources.getSystem().getIdentifier("webViewStyle", "attr", "android"));
        }

        /**
         * Description: Copy Constructor
         * Created by Michael Lee on 9/21/16 17:13
         * @param   context     context
         * @param   attrs       attributes set
         * @param   defStyle    sytle type
         */
        @SuppressLint({"SetJavaScriptEnabled"})
        public MyWebView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            // Set WebView client
            setWebViewClient(new MyWebViewClient());
            // set JS
            getSettings().setJavaScriptEnabled(true);
            // Block press and hold event
            this.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
        }

        /**
         * Description: see {@link WebView#onScrollChanged}
         *  When webview scrolling, keep the progress bar on the top.
         * Created by Michael Lee on 9/21/16 17:13
         * @param   l       Current horizontal scroll origin
         * @param   t       current vertical scroll origin
         * @param   oldl    Previous horizontal scroll origin
         * @param   oldt    Previous vertical scroll origin
         */
        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            // if current vertical scroll origin is not 0, CAN NOT pull to refresh
            can_pull_to_refresh_ = (t == 0);
            super.onScrollChanged(l, t, oldl, oldt);
        }
    }


    /**---------------------------------------------------------------------------------------------
     * Description: webview client
     *  Record current url when onPageStarted and hide the pull-to-refresh header after page load is
     *  finished.
     *--------------------------------------------------------------------------------------------*/
    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG,"onPageStarted");
            current_url_ = url;
            super.onPageStarted(view,url,favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG,"onPageFinished");
            hideHeader();
            super.onPageFinished(view,url);
        }
    }
}