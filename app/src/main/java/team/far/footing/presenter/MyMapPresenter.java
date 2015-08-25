package team.far.footing.presenter;

import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.bmob.v3.listener.FindListener;
import team.far.footing.model.IMapModel;
import team.far.footing.model.bean.MapBean;
import team.far.footing.model.impl.MapModel;
import team.far.footing.ui.adapter.MapRyViewAdapter;
import team.far.footing.ui.vu.IMyMapVu;
import team.far.footing.util.BmobUtils;

/**
 * Created by luoyy on 2015/8/19 0019.
 */
public class MyMapPresenter {
    private IMyMapVu iMyMapVu;
    private IMapModel mapModel;
    private MapRyViewAdapter mapRyViewAdapter;


    public MyMapPresenter(IMyMapVu iMyMapVu) {
        this.iMyMapVu = iMyMapVu;
        mapModel = MapModel.getInstance();
        showmlist();
    }


    private void showmlist() {
        if (iMyMapVu != null) iMyMapVu.showLoading("玩命加载中...");

        mapModel.get_map_byuserbean(BmobUtils.getCurrentUser(), new FindListener<MapBean>() {
            @Override
            public void onSuccess(List<MapBean> list) {
                // 倒叙
                Collections.reverse(list);
                List<MapBean> tempList  = new ArrayList<MapBean>();
                if(list.size() != 0) {
                    for (MapBean mapBean : list) {
                        if (Double.parseDouble(mapBean.getAll_distance()) > 0) {
                            tempList.add(mapBean);
                        }
                    }
                    if (iMyMapVu != null) iMyMapVu.stopLoading();
                    if (tempList.size() != 0) {
                        if (iMyMapVu != null) {
                            mapRyViewAdapter = new MapRyViewAdapter(tempList, iMyMapVu.getActivity());
                            iMyMapVu.showmaplist(mapRyViewAdapter);
                        }
                    } else {
                        if (iMyMapVu != null) {
                            iMyMapVu.stopLoading();
                            iMyMapVu.showEmpty();
                        }
                    }
                } else {
                    if (iMyMapVu != null) {
                        iMyMapVu.stopLoading();
                        iMyMapVu.showEmpty();
                    }
                }
            }

            @Override
            public void onError(int i, String s) {
                iMyMapVu.stopLoading();
                if (iMyMapVu != null) Toast.makeText((Activity) iMyMapVu, BmobUtils.searchCode(i), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void deletmap(MapBean mapBean) {
        mapModel.delete_mapbean(mapBean);
    }

    public void onRelieveView() {
        if (iMyMapVu != null) iMyMapVu = null;
    }
}
