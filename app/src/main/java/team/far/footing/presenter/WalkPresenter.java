package team.far.footing.presenter;


import android.app.Activity;
import android.graphics.Bitmap;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.UpdateListener;
import team.far.footing.R;
import team.far.footing.app.APP;
import team.far.footing.model.IFileModel;
import team.far.footing.model.IMapModel;
import team.far.footing.model.IShareModel;
import team.far.footing.model.IUserModel;
import team.far.footing.model.bean.MapBean;
import team.far.footing.model.callback.OnUpdateMapListener;
import team.far.footing.model.callback.OnUploadListener;
import team.far.footing.model.impl.FileModel;
import team.far.footing.model.impl.MapModel;
import team.far.footing.model.impl.ShareModel;
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


    // 分享
    private IShareModel shareModel;
    private IFileModel fileModel;

    public WalkPresenter(IWalkVu v) {
        this.v = v;
        // 定位
        mapModel = MapModel.getInstance();
        userModel = UserModel.getInstance();
        initLocation();

        // 分享
        shareModel = ShareModel.getInstance();
        fileModel = FileModel.getInstance();
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
        if (v != null) {
            v.getBaiduMap().setMyLocationEnabled(true);
            if (!mLocationClient.isStarted()) {
                mLocationClient.start();
            }
            mOrientationListener.start();
        }
    }

    // 停止定位
    public void stopLocation() {
        LogUtils.d("停止定位了");
        if (v != null) {
            v.getBaiduMap().setMyLocationEnabled(false);
            mLocationClient.stop();
            mOrientationListener.stop();
        }

    }

    public boolean isWalking() {
        return isWalking;
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
                "map_file_name", map_list, end_date.getTime() - start_date.getTime() + "", new DecimalFormat(".##").format(distanceTotal) + "", TimeUtils.dateToString(start_date), city, address, street,
                new OnUpdateMapListener() {
                    @Override
                    public void onSuccess(MapBean mapBean) {
                        LogUtils.d("路线上传成功");
                        //保存用户行走记录
                        userModel.update_today_distance((int) distanceTotal, TimeUtils.getTodayDate(), null);
                        cleanMap();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        LogUtils.e("上传失败");
                        cleanMap();
                    }
                });
        // 保存用户 等级数据
        userModel.updateUser_level(BmobUtils.getCurrentUser().getLevel() + (int) distanceTotal, null);


    }

    // 解除view的绑定
    public void onRelieveView() {
        v = null;
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (v != null) {
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

                // 回来的时候把镜头移动到当前位置
                if (appStatus == STATUS_HOME_BACK) {
                    v.moveCamera2Location(latLng);
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



    // 下面都是分享相关的

    public void QQshare() {
        if (v != null) {
            v.show_shareProgress(0);
            v.getBaiduMap().snapshot(new BaiduMap.SnapshotReadyCallback() {
                @Override
                public void onSnapshotReady(Bitmap bitmap) {
                    String path = saveBitmap(bitmap);
                    uploadBitmap(path);
                }
            });
        }
    }

    public String saveBitmap(Bitmap bitmap) {
        final File f = new File(fileModel.getCacheThumbnailDir(), "bitmap.jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (v != null) v.show_shareError(e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            if (v != null) v.show_shareError(e.toString());
        }
        return f.getPath();
    }


    public void uploadBitmap(final String path) {
        fileModel.uploadPic(path, new OnUploadListener() {
            @Override
            public void onSuccess(String fileName, String url, BmobFile file) {
                saveMapBean(fileName, url, path);
            }

            @Override
            public void onProgress(int progress) {
                if (progress != 100 && v != null) v.show_shareProgress(progress);
            }

            @Override
            public void onError(int statuscode, String errormsg) {
                if (v != null) v.show_shareError(errormsg);
            }
        });
    }


    public void saveMapBean(final String fileName, final String url, final String path) {
        mapModel.save_map_finish(BmobUtils.getCurrentUser(), "url",
                "map_file_name", map_list, "0", "0", "", "", "", "",
                new OnUpdateMapListener() {
                    @Override
                    public void onSuccess(MapBean mapBean) {
                        LogUtils.d("路线上传成功");
                        mapBean.setMap_file_name(fileName);
                        mapBean.setMap_url(url);
                        mapBean.update(APP.getContext(), new UpdateListener() {
                            @Override
                            public void onSuccess() {
                                shareMap(url, path);
                            }

                            @Override
                            public void onFailure(int i, String s) {
                                if (v != null) v.show_shareError(s);
                            }
                        });
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        if (v != null) v.show_shareError(s);
                    }
                });
    }

    public void shareMap(String url, String path) {
        if (v != null) {
            shareModel.ShareToQQWithPT((Activity) v, path, url, new IUiListener() {
                @Override
                public void onComplete(Object o) {
                    LogUtils.e("完成分享");
                    if (v != null) v.show_shareSuccess();
                }

                @Override
                public void onError(UiError uiError) {
                    LogUtils.e("完成失败");
                    if (v != null) v.show_shareError(uiError.toString());
                }

                @Override
                public void onCancel() {
                    LogUtils.e("完成取消");
                    if (v != null) v.show_shareCancel();
                }
            });
        }
    }

    public Tencent getTencent(){
        return ShareModel.getmTencent();
    }
}
