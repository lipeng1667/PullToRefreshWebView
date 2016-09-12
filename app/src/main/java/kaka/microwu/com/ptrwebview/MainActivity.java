package kaka.microwu.com.ptrwebview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {
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
    private ViewGroup.MarginLayoutParams header_layout_params_;

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
     * trigger for only load once in 
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
     * current page's pull_to_refresh state, default is false, if current page's property json part
     * contains pull_to_refresh, we say it's allowed to pull_to_refresh
     */
    private boolean allow_pull_to_refresh = false;

    /**
     * Description: see {@link android.app.Activity#onCreate(Bundle)}
     * Created by Michael Lee on 9/12/16 20:07
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((WebView) findViewById(R.id.web_view)).loadUrl("https://lipeng1667.github.io");
    }
}
