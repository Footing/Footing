package team.far.footing.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.balysv.materialripple.MaterialRippleLayout;

import java.util.List;

import team.far.footing.R;
import team.far.footing.model.bean.MapBean;
import team.far.footing.model.impl.MapModel;
import team.far.footing.ui.activity.ShowMapActivity;
import team.far.footing.util.TimeUtils;

/**
 * Created by luoyy on 2015/8/19 0019.
 */
public class MapRyViewAdapter extends RecyclerView.Adapter<MapRyViewAdapter.ViewHolder> {

    private List<MapBean> mapBeanList;
    private Context context;

    public MapRyViewAdapter(List<MapBean> mapBeanList, Context context) {
        this.mapBeanList = mapBeanList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.mymap_recycler_item, null);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.tvAlltime.setText("总时间： " +TimeUtils.formatTime(Long.parseLong( mapBeanList.get(position).getAll_time())));
        holder.tvDistance.setText("总距离： " + mapBeanList.get(position).getAll_distance()+" m");
        holder.tvStarttime.setText("时间：" + mapBeanList.get(position).getStart_time());
        holder.ripple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ShowMapActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("map", mapBeanList.get(position));
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
        holder.ripple.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new MaterialDialog.Builder(context).title("提示").content("是否删除该足迹").positiveText("删除").negativeText("取消").theme(Theme.LIGHT).callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        MapModel.getInstance().delete_mapbean(mapBeanList.get(position));
                        mapBeanList.remove(position);
                        MapRyViewAdapter.this.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                }).show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mapBeanList.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStarttime;
        private TextView tvDistance;
        private TextView tvAlltime;
        private CardView CVFgToday;
        private MaterialRippleLayout ripple;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDistance = (TextView) itemView.findViewById(R.id.tv_distance);
            tvStarttime = (TextView) itemView.findViewById(R.id.tv_starttime);
            tvAlltime = (TextView) itemView.findViewById(R.id.tv_alltime);
            CVFgToday = (CardView) itemView.findViewById(R.id.cv_fg_friends);
            ripple = (MaterialRippleLayout) itemView.findViewById(R.id.ripple);
        }
    }


}
