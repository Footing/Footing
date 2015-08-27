package team.far.footing.ui.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import butterknife.ButterKnife;
import butterknife.InjectView;
import team.far.footing.R;
import team.far.footing.app.APP;
import team.far.footing.app.BaseActivity;
import team.far.footing.presenter.AddFriendPresenter;
import team.far.footing.ui.vu.IAddFriendVu;
import team.far.footing.ui.widget.DividerItemDecoration;

public class AddFriendActivity extends BaseActivity implements IAddFriendVu, SwipeRefreshLayout.OnRefreshListener {

    @InjectView(R.id.edit_addfriend)
    EditText editAddfriend;
    @InjectView(R.id.firends_recyclerview)
    RecyclerView mRecyclerview;
    @InjectView(R.id.swipe_refresh_widget)
    SwipeRefreshLayout swipeRefreshWidget;
    @InjectView(R.id.bt_query)
    ImageView btQuery;
    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.add_firends_recycler_view_empty) TextView mEmptyView;
    @InjectView(R.id.progress_wheel) ProgressWheel progressWheel;

    private AddFriendPresenter addFriendPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        ButterKnife.inject(this);
        init();
        initToolbar();
        addFriendPresenter = new AddFriendPresenter(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        addFriendPresenter.onRelieveView();
    }

    private void initToolbar() {
        mToolbar.setTitle("找朋友");
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.ic_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void init() {
        mRecyclerview.setLayoutManager(new LinearLayoutManager(APP.getContext()));
        mRecyclerview.addItemDecoration(new DividerItemDecoration(APP.getContext(), LinearLayoutManager.VERTICAL));
        swipeRefreshWidget.setOnRefreshListener(this);
        swipeRefreshWidget.setColorSchemeResources(R.color.accent_color, R.color.primary_color);

        btQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriendPresenter.query(editAddfriend.getText().toString());
            }
        });
    }


    @Override
    public void showfriends(RecyclerView.Adapter adapter) {
        swipeRefreshWidget.setVisibility(View.VISIBLE);
        mRecyclerview.setAdapter(adapter);
    }

    @Override
    public void stopRefresh() {
        swipeRefreshWidget.setRefreshing(false);
    }

    @Override
    public void showEmpty(String s) {
        swipeRefreshWidget.setVisibility(View.GONE);
        mEmptyView.setText(s);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showSearchProgress() {
        progressWheel.setVisibility(View.VISIBLE);
    }

    @Override
    public void dismissSearchProgress() {
        progressWheel.setVisibility(View.GONE);
    }


    @Override
    public void onRefresh() {
        addFriendPresenter.Refresh();
    }
}
