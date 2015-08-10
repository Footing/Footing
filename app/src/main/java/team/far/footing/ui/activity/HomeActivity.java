package team.far.footing.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import team.far.footing.R;
import team.far.footing.app.APP;
import team.far.footing.app.BaseActivity;
import team.far.footing.model.IFriendModel;
import team.far.footing.model.IUserModel;
import team.far.footing.model.Listener.OnQueryFriendListener;
import team.far.footing.model.bean.Userbean;
import team.far.footing.model.impl.FriendModel;
import team.far.footing.model.impl.UserModel;
import team.far.footing.presenter.HomePresenter;
import team.far.footing.ui.adpter.HomePagerAdapter;
import team.far.footing.ui.fragment.FriendsFragment;
import team.far.footing.ui.fragment.SquareFragment;
import team.far.footing.ui.fragment.WalkFragment;
import team.far.footing.ui.vu.IHomeVu;
import team.far.footing.util.BmobUtils;
import team.far.footing.util.LogUtils;
import team.far.footing.util.SPUtils;

public class HomeActivity extends BaseActivity implements IHomeVu, View.OnClickListener {

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;
    @InjectView(R.id.tabLayout)
    TabLayout mTabLayout;
    @InjectView(R.id.fabBtn_home)
    FloatingActionButton mFabBtn;
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.view_pager)
    ViewPager mViewPager;
    @InjectView(R.id.tv_home_user_name)
    TextView userName;
    @InjectView(R.id.tv_home_user_lv)
    TextView userLV;
    @InjectView(R.id.iv_home_user_image)
    ImageView userPic;
    @InjectView(R.id.tv_home_user_signature)
    TextView userSignature;
    @InjectView(R.id.navigation)
    NavigationView navigation;
    private HomePresenter presenter;
    private MaterialMenuIconToolbar materialMenu;
    private List<Fragment> fragmentList = new ArrayList<Fragment>();
    private HomePagerAdapter fragmentPagerAdapter;
    private boolean isDrawerOpened;
    // 保存page的选择，默认为第一页
    private int pageSelect = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.inject(this);
        initToolbar();
        initNavIcon();
        init();
        presenter = new HomePresenter(this);
        Test();

        // 测试退出，以后会放在设置里面
        navigation.getMenu().findItem(R.id.navItem4).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                SPUtils.put(APP.getContext(), "isLogin", Boolean.FALSE);
                BmobUtils.LogOutUser();
                Intent intent = new Intent(APP.getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                finish();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onRelieveView();
    }

    private void initNavIcon() {
        materialMenu = new MaterialMenuIconToolbar(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN) {
            @Override
            public int getToolbarViewId() {
                return R.id.toolbar;
            }
        };
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                materialMenu.setTransformationOffset(MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                        isDrawerOpened ? 2 - slideOffset : slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                isDrawerOpened = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                isDrawerOpened = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_IDLE) {
                    if (isDrawerOpened) materialMenu.setState(MaterialMenuDrawable.IconState.ARROW);
                    else materialMenu.setState(MaterialMenuDrawable.IconState.BURGER);
                }
            }
        });
    }

    private void initToolbar() {
        mToolbar.setTitle(getResources().getString(R.string.app_name));
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDrawerOpened) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void init() {
        mFabBtn.setOnClickListener(this);
        fragmentList.add(new WalkFragment());
        fragmentList.add(new FriendsFragment());
        fragmentList.add(new SquareFragment());
        fragmentPagerAdapter = new HomePagerAdapter(getSupportFragmentManager(), fragmentList);
        mViewPager.setAdapter(fragmentPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                pageSelect = position;
                switch (position) {
                    case 0:
                        mFabBtn.setImageResource(R.mipmap.ic_run);
                        break;
                    case 1:
                        mFabBtn.setImageResource(R.mipmap.ic_back);
                        break;
                    case 2:
                        mFabBtn.setImageResource(R.mipmap.ic_plus);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        // 去死吧！接受我的愤怒吧！tabLayout
        // 这货的绑定viewpager之后的title不是自己的，是adapter给的
        // 没有找到返回图标的方法，字还那么小一个
        // 这里总耗时：48min，先让你呆在这里，之后我不把你换掉我不是人！！！
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabsFromPagerAdapter(fragmentPagerAdapter);
        mTabLayout.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.white));
    }

    @Override
    public void showUserInformation(Userbean userbean) {
        LogUtils.d(userbean.getUsername());
        // TODO: 完善bean
        if (!(userbean.getNickName() == null)) {
            userName.setText(userbean.getNickName());
        } else {
            userName.setText("未取名");
        }
        //userPic.setImageBitmap();
        userLV.setText("Lv." + userbean.getLevel());
        userSignature.setText(userbean.getSignature());
    }

    @Override
    public void refreshUserInforimation() {
        presenter.refreshUserInformation();
    }

    /**
     * 任务
     */
    @Override
    public void showMission() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabBtn_home:
                switch (pageSelect) {
                    case 0:
                        presenter.startWalkActivity(this);
                        break;
                    case 1:
                        Toast.makeText(this, "好友测试", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(this, "广场测试", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
    }




    //我用来测试的方法
    public void Test() {


    }

}
