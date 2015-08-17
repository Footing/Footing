package team.far.footing.presenter;

import android.app.Activity;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;

import team.far.footing.R;
import team.far.footing.app.APP;
import team.far.footing.ui.vu.IWalkVu;
import team.far.footing.util.LogUtils;
import team.far.footing.util.listener.MyOrientationListener;


/**
 * Created by moi on 2015/8/10.
 */
public class WalkPresenter {

    private IWalkVu v;
    // 定位
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    private boolean isFirstIn = true;

    private BitmapDescriptor mMapPointer;
    private MyOrientationListener mOrientationListener;
    private float mCurrentX;
    // 存放绘制路线的端点
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private double firstDistance = 0;
    private int span = 3;
    private double acceleration = 0;
    private boolean isWalking = false;
    private double distanceTotal = 0;
    // app状态
    private int appStatus = 0;
    public static final int STATUS_ACTIVITY_ON = 0;
    public static final int STATUS_HOME = 1;
    public static final int STATUS_HOME_BACK = 2;

    public WalkPresenter(IWalkVu v) {
        this.v = v;
        // 定位
        initLocation();
    }

    // 按下home键
    public void onHome() {
        appStatus = STATUS_HOME;
    }

    // 从home返回
    public void onHomeBack() {
        appStatus = STATUS_HOME_BACK;
    }

    // 开始定位
    public void startLocation() {
        LogUtils.d("开始定位了");
        v.getBaiduMap().setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }
        mOrientationListener.start();
    }

    // 停止定位
    public void stopLocation() {
        LogUtils.d("停止定位了");
        v.getBaiduMap().setMyLocationEnabled(false);
        mLocationClient.stop();
        mOrientationListener.stop();
    }

    public void startWalk() {
        isWalking = true;
    }

    // 停止步行这里还有些麻烦，第二段开始走得时候没法保存第一段的数据
    // 之后好友内分享估计会遇到麻烦……
    public void pauseWalk() {
        isWalking = false;
        // 坐标点清零
        latLngs.clear();
    }

    public void stopWalk() {
        isWalking = false;
        // 坐标点清零
        latLngs.clear();
        // 清除地图上的轨迹
        v.getBaiduMap().clear();
        // 总距离清零
        distanceTotal = 0;
        // 总距离的显示清零
        v.showDistanceTotal(distanceTotal);
    }

    private void initLocation() {
        mLocationClient = new LocationClient(APP.getContext());
        mLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        // stop时不杀死service
        option.setIgnoreKillProcess(true);
        // 请求的频率
        option.setScanSpan(span * 1000);
        mLocationClient.setLocOption(option);

        mMapPointer = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_pointer);
        mOrientationListener = new MyOrientationListener((Activity)v);
        mOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    // 解除view的绑定
    public void onRelieveView() {
        v = null;
    }

    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            LogUtils.d("现在的状态：" + appStatus);
            LogUtils.d("LatLng's size: " + latLngs.size());
            MyLocationData data = new MyLocationData.Builder().direction(mCurrentX)
                    .accuracy(bdLocation.getRadius()).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mMapPointer);
            v.getBaiduMap().setMyLocationData(data);
            v.getBaiduMap().setMyLocationConfigeration(config);
            LogUtils.d(bdLocation.getLatitude() + " , " + bdLocation.getLongitude());
            LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            // 第一次定位把镜头移向用户当前位置
            if (isFirstIn) {
                v.moveCamera2Location(latLng);
                isFirstIn = false;
            }
            // 开始步行才记录（请求失败就不要记录了）
            if (isWalking && bdLocation.getLatitude() != 4.9E-324) {
                // 如果不是刚从home回来，就慢慢画
                if (appStatus != STATUS_HOME_BACK) {
                    // 先不管怎么样，加入第一个点再说
                    if (latLngs.isEmpty()) latLngs.add(latLng);
                    // 先从距离和加速度两方面控制某一点是否添加到数组中
                    // 还是有些问题，如果第二点是跳点的话没法判断加速度把它删去，所以要先开启定位再开启绘制工作
                    // 当前距离
                    double secondDistance = 0;
                    if (latLngs.size() > 0) {
                        secondDistance = DistanceUtil
                                .getDistance(latLng, latLngs.get(latLngs.size() - 1));
                    }
                    // 距离大于5
                    if (secondDistance >= 5) {
                        if (firstDistance == 0) {
                            latLngs.add(latLng);
                            v.drawPolyline(latLngs);
                            firstDistance = secondDistance;
                        } else {
                            // 加速度
                            acceleration = (secondDistance - firstDistance) / (span * span);
                            // 加速度小于10
                            if (acceleration <= 10) {
                                latLngs.add(latLng);
                                firstDistance = secondDistance;
                                distanceTotal += secondDistance;
                                // activity开启的时候才画，home的时候不画，只记录点
                                if (appStatus == STATUS_ACTIVITY_ON) {
                                    v.drawPolyline(latLngs);
                                    v.showDistanceTotal(distanceTotal);
                                }
                            }
                        }
                    }
                } else {
                    // 刚从home回来，直接画
                    v.drawAllPolyline(latLngs);
                    v.showDistanceTotal(distanceTotal);
                    // 画完了马上换状态
                    appStatus = STATUS_ACTIVITY_ON;
                }
            }
        }
    }
}
