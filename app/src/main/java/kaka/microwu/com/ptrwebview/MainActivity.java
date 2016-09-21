package kaka.microwu.com.ptrwebview;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.microwu.ptrwebview.PullToRefreshWebView;

public class MainActivity extends AppCompatActivity {

    /**
     * ptr_webView instance
     */
    PullToRefreshWebView ptr_web_view_;

    /**
     * Description: see {@link android.app.Activity#onCreate(Bundle)}
     * Created by Michael Lee on 9/12/16 20:07
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ptr_web_view_ = new PullToRefreshWebView(this,null);
        RelativeLayout container = (RelativeLayout) findViewById(R.id.web_view_container);
        container.addView(ptr_web_view_);
        ptr_web_view_.loadUrl("http://www.baidu.com");
    }

    /**
     * Description: When user click the back button on the phone.
     *    Double click will quit and kill the process
     * Created by Michael Lee on 2/11/16 16:55
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (ptr_web_view_.canGoBack()) {
                    ptr_web_view_.goBack();
                    return true;
                } else {
                    Toast.makeText(this,"This is the first page, can NOT going bcak"
                            ,Toast.LENGTH_LONG).show();
                    return true;
                }
            }
        }
        return false;
    }
}
