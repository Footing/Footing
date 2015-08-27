package team.far.footing.ui.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import team.far.footing.R;
import team.far.footing.app.BaseActivity;
import team.far.footing.util.SPUtils;

public class LaunchActivity extends BaseActivity {

    @InjectView(R.id.tv_launch_title) TextView tvLaunchTitle;
    private Boolean isLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launch);
        ButterKnife.inject(this);
        noUseBarTint();
        initFonts();
        // 每次进入显示LOGO的地方先判断是否登陆过
        isLogin = (Boolean) SPUtils.get(this, "isLogin", Boolean.FALSE);
        myIntent();
    }

    private void initFonts() {
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/square_fonts.TTF");
        tvLaunchTitle.setTypeface(face);
        tvLaunchTitle.setTypeface(face);
    }

    private void myIntent() {
        if (isLogin) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LaunchActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    LaunchActivity.this.startActivity(intent);
                    LaunchActivity.this.finish();
                }
            }, 1800);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LaunchActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    LaunchActivity.this.startActivity(intent);
                    LaunchActivity.this.finish();
                }
            }, 1800);
        }
    }
}
