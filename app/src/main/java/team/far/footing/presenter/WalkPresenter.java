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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import team.far.footing.R;
import team.far.footing.app.APP;
import team.far.footing.model.IMapModel;
import team.far.footing.model.IUserModel;
import team.far.footing.model.bean.MapBean;
import team.far.footing.model.callback.OnUpdateMapListener;
import team.far.footing.model.callback.OnUpdateUserListener;
import team.far.footing.model.impl.MapModel;
import team.far.footing.model.impl.UserModel;
import team.far.footing.ui.vu.IWalkVu;
import team.far.footing.util.BmobUtils;
import team.far.footing.util.LogUtils;
import team.far.footing.util.TimeUtils;
import team.far.footing.util.listener.MyOrientationListener;


/**
 * Created by moi on 2015/8/10.
 */
public class WalkPresenter {

    private IWalkVu v;
    private Date start_date, end_date;
    // 定位
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    private boolean isFirstIn = true;

    private BitmapDescriptor mMapPointer;
    private MyOrientationListener mOrientationListener;
    private float mCurrentX;
    // 记录城市
    private String city;
    private String address;
    private String street;
    // 存放绘制路线的端点
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    // 上一次的距离
    private double lastDistance = 0;
    // 当前距离
    private double currentDistance = 0;
    // 距离上次记录点的间隔时间！！
    private int timeSpan = 3;
    private List<String> map_list = new ArrayList<>();
    private int span = 3;
    private double acceleration = 0;
    private boolean isWalking = false;
    private double distanceTotal = 0;
    // app状态
    private int appStatus = 0;
    public static final int STATUS_ACTIVITY_ON = 0;
    public static final int STATUS_HOME = 1;
    public static final int STATUS_HOME_BACK = 2;

    //model
    private IMapModel mapModel;
    private IUserModel userModel;

    public WalkPresenter(IWalkVu v) {
        this.v = v;
        // 定位
        mapModel = MapModel.getInstance();
        userModel = UserModel.getInstance();
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
        start_date = TimeUtils.getcurrentTime();
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
        save();
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
        mOrientationListener = new MyOrientationListener((Activity) v);
        mOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    private void cleanMap() {
        //清除数据
        map_list.clear();
        // 坐标点清零
        latLngs.clear();
        // 总距离清零
        distanceTotal = 0;
        // 清除地图上的轨迹
        if (v != null) {
            v.getBaiduMap().clear();
            // 总距离的显示清零
            v.showDistanceTotal(distanceTotal);
        }
    }

    private void save() {
        LogUtils.e("map_list", (map_list == null) + "");

        end_date = TimeUtils.getcurrentTime();
        // 保存地图数据
        mapModel.save_map_finish(BmobUtils.getCurrentUser(), "url",
                "map_file_name", map_list, end_date.getTime() - start_date.getTime() + "", new DecimalFormat(".##").format(distanceTotal) + "",TimeUtils.dateToString(start_date), city, address, street,
                new OnUpdateMapListener() {
                    @Override
                    public void onSuccess(MapBean mapBean) {
                        LogUtils.d("路线上传成功");
                        userModel.update_distance((int) (BmobUtils.getCurrentUser().getToday_distance() + distanceTotal),
                                (int) (BmobUtils.getCurrentUser().getAll_distance() + distanceTotal), new OnUpdateUserListener() {
                                    @Override
                                    public void onSuccess() {
                                        LogUtils.d("用户信息上传成功");
                                        cleanMap();
                                    }

                                    @Override
                                    public void onFailure(int i, String s) {
                                        LogUtils.e("上传失败");
                                        cleanMap();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        LogUtils.e("上传失败");
                        cleanMap();
                    }
                });
        // 保存用户 等级数据
        userModel.updateUser_level(BmobUtils.getCurrentUser().getLevel() + (int) distanceTotal, null);
        //保存用户行走记录
        if (TimeUtils.isToday(BmobUtils.getCurrentUser().getToday_date())) {
            userModel.update_today_distance(BmobUtils.getCurrentUser().getToday_distance() + (int) distanceTotal, TimeUtils.getTodayDate(), null);
        } else {
            userModel.update_today_distance((int) distanceTotal, TimeUtils.getTodayDate(), null);
        }

    }

    // 解除view的绑定
    public void onRelieveView() {
        v = null;
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            LogUtils.d("现在的状态：" + appStatus);
            LogUtils.d("LatLng's size: " + latLngs.size());
            map_list.add(bdLocation.getLongitude() + "=" + bdLocation.getLatitude());
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
                city = bdLocation.getCity();
                address = bdLocation.getDistrict();
                street = bdLocation.getStreet();
                LogUtils.d("城市：" + city + "，位置：" + address);
                LogUtils.d(bdLocation.getCity() + "," + bdLocation.getCountry()
                        + "," + bdLocation.getAddrStr() + "," + bdLocation.getFloor()
                        + "," + bdLocation.getProvince() + "," + bdLocation.getStreet());
            }
            // 开始步行才记录（请求失败就不要记录了）
            if (isWalking && bdLocation.getLatitude() != 4.9E-324) {
                // 如果不是刚从home回来，就慢慢画
                if (appStatus != STATUS_HOME_BACK) {
                    // 先不管怎么样，加入第一个点再说
                    if (latLngs.isEmpty()) latLngs.add(latLng);
                    // 先从距离和加速度两方面控制某一点是否添加到数组中
                    // 还是有些问题，如果第二点是跳点的话没法判断加速度把它删去，所以要先开启定位再开启绘制工作
                    if (latLngs.size() > 0) {
                        currentDistance = DistanceUtil
                                .getDistance(latLng, latLngs.get(latLngs.size() - 1));
                    }
                    // 距离大于10
                    if (currentDistance >= 10) {
                        if (lastDistance == 0) {
                            latLngs.add(latLng);
                            v.drawPolyline(latLngs);
                            lastDistance = currentDistance;
                        } else {
                            // 加速度
                            acceleration = (currentDistance - lastDistance) / (timeSpan * timeSpan);
                            // 加速度小于10
                            if (acceleration <= 10) {
                                latLngs.add(latLng);
                                // 加入点之后重置间隔时间
                                timeSpan = 0;
                                lastDistance = currentDistance;
                                distanceTotal += currentDistance;
                                v.drawPolyline(latLngs);
                                v.showDistanceTotal(distanceTotal);
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
                // 间隔时间记录
                timeSpan += span;
            }
        }
    }
}
