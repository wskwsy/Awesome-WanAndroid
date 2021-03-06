package json.chao.com.wanandroid.ui.main.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import json.chao.com.wanandroid.R;
import json.chao.com.wanandroid.app.Constants;
import json.chao.com.wanandroid.base.activity.BaseActivity;
import json.chao.com.wanandroid.component.RxBus;
import json.chao.com.wanandroid.contract.main.SearchListContract;
import json.chao.com.wanandroid.core.DataManager;
import json.chao.com.wanandroid.core.bean.main.collect.FeedArticleData;
import json.chao.com.wanandroid.core.bean.main.collect.FeedArticleListData;
import json.chao.com.wanandroid.core.bean.main.collect.FeedArticleListResponse;
import json.chao.com.wanandroid.core.event.CollectEvent;
import json.chao.com.wanandroid.presenter.main.SearchListPresenter;
import json.chao.com.wanandroid.ui.mainpager.adapter.ArticleListAdapter;
import json.chao.com.wanandroid.utils.CommonUtils;
import json.chao.com.wanandroid.utils.JudgeUtils;
import json.chao.com.wanandroid.utils.StatusBarUtil;

/**
 * @author quchao
 * @date 2018/3/13
 */

public class SearchListActivity extends BaseActivity<SearchListPresenter> implements SearchListContract.View {

    @BindView(R.id.common_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.common_toolbar_title_tv)
    TextView mTitleTv;
    @BindView(R.id.search_list_refresh_layout)
    RefreshLayout mRefreshLayout;
    @BindView(R.id.search_list_recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.search_list_floating_action_btn)
    FloatingActionButton mFloatingActionButton;

    @Inject
    DataManager mDataManager;
    private int articlePosition;
    private int mCurrentPage;
    private List<FeedArticleData> mArticleList;
    private ArticleListAdapter mAdapter;
    private boolean isAddData;
    private String searchText;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_search_list;
    }

    @Override
    protected void initEventAndData() {
        initToolbar();
        mPresenter.getSearchList(mCurrentPage, searchText);
        mArticleList = new ArrayList<>();
        mAdapter = new ArticleListAdapter(R.layout.item_search_pager, mArticleList);
        mAdapter.isSearchPage();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            articlePosition = position;
            JudgeUtils.startArticleDetailActivity(this,
                    mAdapter.getData().get(position).getId(),
                    mAdapter.getData().get(position).getTitle(),
                    mAdapter.getData().get(position).getLink(),
                    mAdapter.getData().get(position).isCollect(),
                    false,
                    false);
        });

        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            if (!mDataManager.getLoginStatus()) {
                startActivity(new Intent(this, LoginActivity.class));
                CommonUtils.showMessage(this, getString(R.string.login_tint));
                return;
            }
            if (mAdapter.getData().get(position).isCollect()) {
                mPresenter.cancelCollectArticle(position, mAdapter.getData().get(position));
            } else {
                mPresenter.addCollectArticle(position, mAdapter.getData().get(position));
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setRefresh();
    }

    @Override
    protected void initInject() {
        getActivityComponent().inject(this);
    }

    @Override
    public void showSearchList(FeedArticleListResponse feedArticleListResponse) {
        if (feedArticleListResponse == null
                || feedArticleListResponse.getData() == null
                || feedArticleListResponse.getData().getDatas() == null) {
            showSearchListFail();
            return;
        }
        FeedArticleListData articleData = feedArticleListResponse.getData();
        mArticleList = articleData.getDatas();
        if (isAddData) {
            mAdapter.addData(mArticleList);
        } else {
            mAdapter.replaceData(mArticleList);
        }
    }

    @Override
    public void showCollectArticleData(int position, FeedArticleData feedArticleData, FeedArticleListResponse feedArticleListResponse) {
        mAdapter.setData(position, feedArticleData);
        CommonUtils.showMessage(this, getString(R.string.collect_success));
    }

    @Override
    public void showCancelCollectArticleData(int position, FeedArticleData feedArticleData, FeedArticleListResponse feedArticleListResponse) {
        mAdapter.setData(position, feedArticleData);
        CommonUtils.showMessage(this, getString(R.string.cancel_collect_success));
    }

    @Override
    public void showSearchListFail() {
        CommonUtils.showMessage(this, getString(R.string.failed_to_obtain_search_data_list));
    }

    @Override
    public void showCollectSuccess() {
        if (mAdapter.getData().size() > articlePosition) {
            mAdapter.getData().get(articlePosition).setCollect(true);
            mAdapter.setData(articlePosition, mAdapter.getData().get(articlePosition));
        }
    }

    @Override
    public void showCancelCollectSuccess() {
        if (mAdapter.getData().size() > articlePosition) {
            mAdapter.getData().get(articlePosition).setCollect(false);
            mAdapter.setData(articlePosition, mAdapter.getData().get(articlePosition));
        }
    }

    @OnClick({R.id.search_list_floating_action_btn})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_list_floating_action_btn:
                mRecyclerView.smoothScrollToPosition(0);
                break;
            default:
                break;
        }
    }

    private void initToolbar() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }
        searchText = ((String) bundle.get(Constants.SEARCH_TEXT));
        if (!TextUtils.isEmpty(searchText)) {
            mTitleTv.setText(searchText);
            mTitleTv.setTextColor(ContextCompat.getColor(this, R.color.title_black));
        }
        StatusBarUtil.immersive(this, ContextCompat.getColor(this, R.color.transparent), 0.3f);
        StatusBarUtil.setPaddingSmart(this, mToolbar);
        mToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        mToolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_grey_24dp));
        mToolbar.setNavigationOnClickListener(v -> onBackPressedSupport());
    }

    private void setRefresh() {
        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            mCurrentPage = 0;
            isAddData = false;
            mPresenter.getSearchList(mCurrentPage, searchText);
            refreshLayout.finishRefresh(2000);
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            mCurrentPage++;
            isAddData = true;
            mPresenter.getSearchList(mCurrentPage, searchText);
            refreshLayout.finishLoadMore(2000);
        });
    }

}
