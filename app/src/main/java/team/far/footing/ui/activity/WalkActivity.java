package team.far.footing.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import team.far.footing.R;
import team.far.footing.app.BaseActivity;
import team.far.footing.presenter.WalkPresenter;
import team.far.footing.ui.vu.IWalkVu;
import team.far.footing.ui.widget.HorizontalProgressBarWithNumber;
import team.far.footing.util.GPSUtils;
import team.far.footing.util.LogUtils;
import team.far.footing.util.animation.ScaleXYAnimation;

public class WalkActivity extends BaseActivity implements IWalkVu, View.OnClickListener {

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;
    @InjectView(R.id.map_walk)
    MapView mMapView;
    @InjectView(R.id.iv_walk_start)
    ImageView ivWalkStart;
    //@InjectView(R.id.iv_walk_pause)
    //ImageView ivWalkPause;
    @InjectView(R.id.iv_walk_stop)
    ImageView ivWalkStop;
    @InjectView(R.id.tv_walk_distance)
    TextView tvWalkDistance;
    private BaiduMap mBaiduMap;
    private WalkPresenter presenter;
    private me.drakeet.materialdialog.MaterialDialog materialDialog;
    private HorizontalProgressBarWithNumber barWithNumber;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);
        ButterKnife.inject(this);
        initToolbar();
        initMap();
        init();
        presenter = new WalkPresenter(this);
        // 开始定位
        presenter.startLocation();

        if (!GPSUtils.isGpsEnable()) {
            new MaterialDialog.Builder(WalkActivity.this).title("未开启GPS").content("没有找到GPS，足下不能工作了，快去系统设置打开GPS吧！")
                    .backgroundColor(getResources().getColor(R.color.white))
                    .positiveText("去开启").negativeText("不了").negativeColor(getResources().getColor(R.color.divider_color)).theme(Theme.LIGHT)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                            startActivityForResult(intent, 35);
                        }
                    }).show();
        }
    }

    private void init() {
        ivWalkStart.setOnClickListener(this);
        //ivWalkPause.setOnClickListener(this);
        ivWalkStop.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 从home返回
        LogUtils.d("从home回来");
        presenter.onHomeBack();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 去home
        LogUtils.d("去home");
        presenter.onHome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止定位
        presenter.stopLocation();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        presenter.onRelieveView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_walk, menu);
        return true;
    }

    private void initMap() {
        mBaiduMap = mMapView.getMap();
        // 隐藏比例尺
        mMapView.showScaleControl(false);
        // 隐藏缩放控件
        mMapView.showZoomControls(false);
        // 事实证明百度地图的logo是可以隐藏的……百度会不会不允许通过呢？先这样吧……
        // mMapView.removeViewAt(1);
        // 缩放比例
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(16.5f);
        mBaiduMap.setMapStatus(msu);
    }

    private void initToolbar() {
        mToolbar.setTitle(getResources().getString(R.string.app_name));
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.ic_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new MaterialDialog.Builder(WalkActivity.this).title("分享").content("是否分享此次步行？")
                        .backgroundColor(getResources().getColor(R.color.white))
                        .neutralText("其他分享")
                        .positiveText("QQ分享").negativeText("取消").theme(Theme.LIGHT)
                        .neutralColor(getResources().getColor(R.color.divider_color))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                presenter.QQshare();
                                dialog.dismiss();
                            }

                            @Override
                            public void onNeutral(MaterialDialog dialog) {
                                share();
                                super.onNeutral(dialog);
                            }
                        }).show();
                return false;
            }
        });
    }

    @Override
    public void showDistanceTotal(double distance) {
        if (distance != 0) {
            if (distance > 1000) {
                tvWalkDistance.setText(new DecimalFormat(".##").format(distance / 1000.0) + " km");
            } else {
                tvWalkDistance.setText(new DecimalFormat(".##").format(distance) + " m");
            }
        } else {
            tvWalkDistance.setText("稍微走动一下哦~");
        }
    }


    @Override
    public void drawPolyline(List<LatLng> latLngs) {
        LogUtils.d("开始画线");
        // 现在才反应过来……原来画线的方法每一次都重绘了整个图……走得时间长了会变得好卡好卡
        // 现在两点两点一画，但这样图就更抖了、还会出现断层、但是至少比界面卡住要好
        if (latLngs.size() > 1) {
            ArrayList<LatLng> tempLatLngs = new ArrayList<>();
            tempLatLngs.add(latLngs.get(latLngs.size() - 2));
            tempLatLngs.add(latLngs.get(latLngs.size() - 1));
            //构建用户绘制多边形的Option对象
            OverlayOptions polylineOptions = new PolylineOptions().points(tempLatLngs)
                    .color(getResources().getColor(R.color.accent_color))
                    .width(10);
            //在地图上添加多边形Option，用于显示
            mBaiduMap.addOverlay(polylineOptions);
        }
    }

    @Override
    public void drawAllPolyline(List<LatLng> latLngs) {
        LogUtils.d("开始画所有的线");
        mBaiduMap.clear();
        if (latLngs.size() > 1) {
            OverlayOptions polylineOptions = new PolylineOptions().points(latLngs)
                    .color(getResources().getColor(R.color.accent_color)).width(10);
            mBaiduMap.addOverlay(polylineOptions);
        }
    }

    @Override
    public void moveCamera2Location(LatLng latLng) {
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu);
    }

    @Override
    public BaiduMap getBaiduMap() {
        return mBaiduMap;
    }

    @Override
    public void startWalk() {
        presenter.startWalk();
        ivWalkStart.setVisibility(View.GONE);
        ivWalkStop.setVisibility(View.VISIBLE);
        ScaleXYAnimation.show(ivWalkStop, null);
        //ivWalkPause.setVisibility(View.VISIBLE);
    }

    @Override
    public void stopWalk() {
        presenter.stopWalk();
        ivWalkStop.setVisibility(View.GONE);
        //ivWalkPause.setVisibility(View.GONE);
        ivWalkStart.setVisibility(View.VISIBLE);
        ScaleXYAnimation.show(ivWalkStart, null);
        tvWalkDistance.setText("点击下方按钮开始！");
    }

/*    @Override
    public void pauseWalk() {
        ivWalkPause.setVisibility(View.GONE);
        cardWalkStatus.setVisibility(View.VISIBLE);
        ivWalkStart.setVisibility(View.VISIBLE);
        ivWalkStop.setVisibility(View.VISIBLE);
        presenter.pauseWalk();
    }*/

    @Override
    public void showstart() {

    }

    private void back() {
        if (presenter.isWalking()) {
            new MaterialDialog.Builder(this).theme(Theme.LIGHT).title("结束此次步行么").backgroundColor(getResources().getColor(R.color.white)).content("保留在这个页面退到后台时，足下也会帮你记录足迹哦").positiveText("结束").negativeText("继续步行").callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    new MaterialDialog.Builder(WalkActivity.this).title("停止步行").content("是否分享此次步行？")
                            .backgroundColor(getResources().getColor(R.color.white))
                            .neutralText("其他分享")
                            .positiveText("QQ分享").negativeText("不用了").theme(Theme.LIGHT)
                            .neutralColor(getResources().getColor(R.color.divider_color))
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    presenter.QQshare();
                                    stopWalk();
                                    dialog.dismiss();
                                }

                                @Override
                                public void onNeutral(MaterialDialog dialog) {
                                    share();
                                    // 停止步行
                                    stopWalk();
                                    dialog.dismiss();
                                }

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    stopWalk();
                                    dialog.dismiss();
                                    finish();
                                }
                            }).show();
                }
            }).show();
        } else {
            finish();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            back();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 分享的相关操作
    private void share() {
        mBaiduMap.snapshotScope(null, new BaiduMap.SnapshotReadyCallback() {

            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                Uri uri = Uri.parse(MediaStore.Images.Media
                        .insertImage(getContentResolver(), bitmap, null, null));
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/png");
                startActivity(Intent
                        .createChooser(shareIntent, getResources().getText(R.string.send_to)));
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_walk_start:
                startWalk();
                break;
            /*case R.id.iv_walk_pause:
                pauseWalk();
                break;*/
            case R.id.iv_walk_stop:
                new MaterialDialog.Builder(this).title("停止步行").content("是否分享此次步行？")
                        .backgroundColor(getResources().getColor(R.color.white))
                        .neutralText("其他分享")
                        .positiveText("QQ分享").negativeText("不用了").theme(Theme.LIGHT)
                        .neutralColor(getResources().getColor(R.color.divider_color))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                presenter.QQshare();
                            }

                            @Override
                            public void onNeutral(MaterialDialog dialog) {
                                share();
                                // 停止步行
                                stopWalk();
                                dialog.dismiss();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                stopWalk();
                                dialog.dismiss();
                            }
                        }).show();
                break;
        }
    }


    @Override
    public void show_shareProgress(int progress) {
        if (materialDialog == null) showdialog();
        barWithNumber.setProgress(progress);
    }

    @Override
    public void show_shareSuccess() {
        dismissProgress();
        materialDialog = null;
    }

    @Override
    public void show_shareError(String s) {
        textView.setText("分享失败,请稍后重试\n"  + s);
        barWithNumber.setVisibility(View.GONE);
        materialDialog = null;
    }

    @Override
    public void show_shareCancel() {
        dismissProgress();
        materialDialog = null;
    }

    private void showdialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null, false);
        barWithNumber = (HorizontalProgressBarWithNumber) v.findViewById(R.id.progressBar);
        textView = (TextView) v.findViewById(R.id.tv_text);
        materialDialog = new me.drakeet.materialdialog.MaterialDialog(WalkActivity.this).setView(v);
        materialDialog.show();
    }

    public void dismissProgress() {
        materialDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 35){
            if (!GPSUtils.isGpsEnable()) {
                new MaterialDialog.Builder(WalkActivity.this).title("未开启GPS").content("GPS还是没开启哦")
                        .backgroundColor(getResources().getColor(R.color.white))
                        .positiveText("继续去开启").negativeText("不了").negativeColor(getResources().getColor(R.color.divider_color)).theme(Theme.LIGHT)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                startActivityForResult(intent, 0);
                            }
                        }).show();
            }  else {
                new MaterialDialog.Builder(WalkActivity.this).title("已开启GPS").content("GPS可以正常使用")
                        .backgroundColor(getResources().getColor(R.color.white))
                        .positiveText("好的").theme(Theme.LIGHT).show();
            }
        } else {
            presenter.getTencent().onActivityResult(requestCode, resultCode, data);
            dismissProgress();
        }
    }
}
