package json.chao.com.wanandroid.ui.navigation.fragment;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import json.chao.com.wanandroid.component.RxBus;
import json.chao.com.wanandroid.core.DataManager;
import json.chao.com.wanandroid.core.bean.navigation.NavigationListData;
import json.chao.com.wanandroid.core.bean.navigation.NavigationListResponse;
import json.chao.com.wanandroid.R;
import json.chao.com.wanandroid.app.Constants;
import json.chao.com.wanandroid.base.fragment.BaseFragment;
import json.chao.com.wanandroid.contract.navigation.NavigationContract;
import json.chao.com.wanandroid.core.event.DismissErrorView;
import json.chao.com.wanandroid.core.event.ShowErrorView;
import json.chao.com.wanandroid.presenter.navigation.NavigationPresenter;
import json.chao.com.wanandroid.ui.navigation.adapter.NavigationAdapter;
import json.chao.com.wanandroid.utils.CommonUtils;
import q.rorbin.verticaltablayout.VerticalTabLayout;
import q.rorbin.verticaltablayout.adapter.TabAdapter;
import q.rorbin.verticaltablayout.widget.ITabView;
import q.rorbin.verticaltablayout.widget.TabView;


/**
 * @author quchao
 * @date 2018/2/11
 */

public class NavigationFragment extends BaseFragment<NavigationPresenter> implements NavigationContract.View {

    @BindView(R.id.navigation_tab_layout)
    VerticalTabLayout mTabLayout;
    @BindView(R.id.navigation_group)
    LinearLayout mNavigationGroup;
    @BindView(R.id.navigation_divider)
    View mDivider;
    @BindView(R.id.navigation_RecyclerView)
    RecyclerView mRecyclerView;

    @Inject
    DataManager mDataManager;
    private LinearLayoutManager mManager;
    private boolean needScroll;
    private int index;
    private boolean isClickTab;

    public static NavigationFragment getInstance(String param1, String param2) {
        NavigationFragment fragment = new NavigationFragment();
        Bundle args = new Bundle();
        args.putString(Constants.ARG_PARAM1, param1);
        args.putString(Constants.ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initInject() {
        getFragmentComponent().inject(this);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_navigation;
    }

    @Override
    protected void initEventAndData() {
        mPresenter.getNavigationListData();
    }

    @Override
    public void showNavigationListData(NavigationListResponse navigationListResponse) {
        if (navigationListResponse == null || navigationListResponse.getData() == null) {
            showNavigationListFail();
            return;
        }
        RxBus.getDefault().post(new DismissErrorView());
        List<NavigationListData> navigationListData = navigationListResponse.getData();
        mTabLayout.setTabAdapter(new TabAdapter() {
            @Override
            public int getCount() {
                return navigationListData == null ? 0 : navigationListData.size();
            }

            @Override
            public ITabView.TabBadge getBadge(int i) {
                return null;
            }

            @Override
            public ITabView.TabIcon getIcon(int i) {
                return null;
            }

            @Override
            public ITabView.TabTitle getTitle(int i) {
                return new TabView.TabTitle.Builder()
                        .setContent(navigationListData.get(i).getName())
                        .setTextColor(0xFF36BC9B, 0xFF757575)
                        .build();
            }

            @Override
            public int getBackground(int i) {
                return -1;
            }
        });
        if (mDataManager.getCurrentPage() == Constants.THIRD) {
            mNavigationGroup.setVisibility(View.VISIBLE);
            mDivider.setVisibility(View.VISIBLE);
        } else {
            mNavigationGroup.setVisibility(View.INVISIBLE);
            mDivider.setVisibility(View.INVISIBLE);
        }
        NavigationAdapter adapter = new NavigationAdapter(R.layout.item_navigation, navigationListData);
        mRecyclerView.setAdapter(adapter);
        mManager = new LinearLayoutManager(_mActivity);
        mRecyclerView.setLayoutManager(mManager);
        leftRightLinkage();
    }

    @Override
    public void showNavigationListFail() {
        CommonUtils.showMessage(_mActivity, getString(R.string.failed_to_obtain_navigation_list));
    }

    @Override
    public void showError() {
        mTabLayout.setBackgroundColor(ContextCompat.getColor(_mActivity, R.color.transparent));
        mNavigationGroup.setVisibility(View.INVISIBLE);
        mDivider.setVisibility(View.INVISIBLE);
        RxBus.getDefault().post(new ShowErrorView());
    }

    /**
     * Left tabLayout and right recyclerView linkage
     */
    private void leftRightLinkage() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (needScroll && (newState == RecyclerView.SCROLL_STATE_IDLE)) {
                    needScroll = false;
                    int indexDistance = index - mManager.findFirstVisibleItemPosition();
                    if (indexDistance >= 0 && indexDistance < mRecyclerView.getChildCount()) {
                        int top = mRecyclerView.getChildAt(indexDistance).getTop();
                        mRecyclerView.smoothScrollBy(0, top);
                    }
                }
                rightLinkageLeft(newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needScroll) {
                    needScroll = false;
                    int indexDistance = index - mManager.findFirstVisibleItemPosition();
                    if (indexDistance >= 0 && indexDistance < mRecyclerView.getChildCount()) {
                        int top = mRecyclerView.getChildAt(indexDistance).getTop();
                        mRecyclerView.smoothScrollBy(0, top);
                    }
                }
            }
        });

        mTabLayout.addOnTabSelectedListener(new VerticalTabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabView tabView, int i) {
                isClickTab = true;
                selectTag(i);
            }

            @Override
            public void onTabReselected(TabView tabView, int i) {
            }
        });
    }

    /**
     * Right recyclerView linkage left tabLayout
     * SCROLL_STATE_IDLE just call once
     *
     * @param newState RecyclerView new scroll state
     */
    private void rightLinkageLeft(int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (isClickTab) {
                isClickTab = false;
                return;
            }
            int firstPosition = mManager.findFirstVisibleItemPosition();
            if (index != firstPosition) {
                index = firstPosition;
                setChecked(index);
            }
        }
    }

    private void selectTag(int i) {
        index = i;
        mRecyclerView.stopScroll();
        smoothScrollToPosition(i);
    }

    /**
     * Smooth right to select the position of the left tab
     *
     * @param position checked position
     */
    private void setChecked(int position) {
        if (isClickTab) {
            isClickTab = false;
        } else {
            mTabLayout.setTabSelected(index);
        }
        index = position;
    }

    private void smoothScrollToPosition(int currentPosition) {
        int firstPosition = mManager.findFirstVisibleItemPosition();
        int lastPosition = mManager.findLastVisibleItemPosition();
        if (currentPosition <= firstPosition) {
            mRecyclerView.smoothScrollToPosition(currentPosition);
        } else if (currentPosition <= lastPosition) {
            int top = mRecyclerView.getChildAt(currentPosition - firstPosition).getTop();
            mRecyclerView.smoothScrollBy(0, top);
        } else {
            mRecyclerView.smoothScrollToPosition(currentPosition);
            needScroll = true;
        }
    }

    public void reLoad() {
        if (mPresenter != null && mNavigationGroup.getVisibility() == View.INVISIBLE) {
            mPresenter.getNavigationListData();
        }
    }

    public void jumpToTheTop() {
        if (mTabLayout != null) {
            mTabLayout.setTabSelected(0);
        }
    }


}
