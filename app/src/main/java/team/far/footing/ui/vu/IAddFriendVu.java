package team.far.footing.ui.vu;

import android.support.v7.widget.RecyclerView;

/**
 * Created by luoyy on 2015/8/18 0018.
 */
public interface IAddFriendVu {


    void showfriends(RecyclerView.Adapter adapter);

    void stopRefresh();

    void showEmpty(String s);

    void showSearchProgress();

    void dismissSearchProgress();
}
