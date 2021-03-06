package com.zhonglushu.example.hovertab;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.zhonglushu.example.hovertab.adapter.CacheFragmentStatePagerAdapter;
import com.zhonglushu.example.hovertab.fragment.ObservableBaseFragment;
import com.zhonglushu.example.hovertab.fragment.ObservableListFragment;
import com.zhonglushu.example.hovertab.observable.ScrollUtils;
import com.zhonglushu.example.hovertab.observable.Scrollable;
import com.zhonglushu.example.hovertab.views.CustomPullDownRefreshLinearLayout;
import com.zhonglushu.example.hovertab.views.ObservableListView;

/**
 * Created by zhonglushu on 2016/4/11.
 */
public abstract class HoverTabActivity extends AppCompatActivity{

    private CustomPullDownRefreshLinearLayout customPullDownRefreshLinearLayout = null;
    private HoverTabFragmentStatePagerAdapter mPagerAdapter;
    private ViewPager mPager;

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener mOnPageChangeListener) {
        if(mPager != null)
            mPager.setOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.com_zhonglushu_example_hovertab_main);
        customPullDownRefreshLinearLayout = (CustomPullDownRefreshLinearLayout) this.findViewById(R.id.com_zhonglushu_example_hovertab_custom);
        customPullDownRefreshLinearLayout.setActivity(this);
        customPullDownRefreshLinearLayout.setmOnRefreshListener(new CustomPullDownRefreshLinearLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                //update the header by need
                onRefreshHeader();

                ObservableBaseFragment fragment =
                        (ObservableBaseFragment) mPagerAdapter.getItemAt(mPager.getCurrentItem());
                if (fragment == null) {
                    return;
                }
                fragment.pullDownRefresh();
            }
        });

        mPager = (ViewPager) findViewById(R.id.com_zhonglushu_example_hovertab_pager);
    }

    public void setmPagerAdapter(HoverTabFragmentStatePagerAdapter mPagerAdapter) {
        this.mPagerAdapter = mPagerAdapter;
        mPager.setAdapter(this.mPagerAdapter);
        mPager.setCurrentItem(0);
    }

    public void setHoverHeaderView(View view){
        if(customPullDownRefreshLinearLayout != null){
            customPullDownRefreshLinearLayout.setHoverHeaderView(view);
        }
    }

    public void setHoverTabView(View view){
        if(customPullDownRefreshLinearLayout != null){
            customPullDownRefreshLinearLayout.setHoverTabView(view);
        }
    }

    public void setViewPagerCurrentItem(int item){
        if(mPager != null){
            mPager.setCurrentItem(item);
        }
    }

    public void setManualRefreshing() {
        if(customPullDownRefreshLinearLayout != null){
            ScrollUtils.addOnGlobalLayoutListener(this.findViewById(android.R.id.content), new Runnable() {
                @Override
                public void run() {
                    customPullDownRefreshLinearLayout.setManualRefreshing();
                }
            });
        }
    }

    public void onRefreshHeader(){

    }

    public boolean isReadyForPullStart(){
        Scrollable sl = getCurrentScrollableView();
        if(sl != null)
            return sl.isReadyForPullStart();
        return false;
    }

    public Scrollable getCurrentScrollableView(){
        ObservableBaseFragment fragment =
                (ObservableBaseFragment) mPagerAdapter.getItemAt(mPager.getCurrentItem());
        if (fragment == null) {
            return null;
        }
        View view = fragment.getView();
        if (view == null) {
            return null;
        }
        return (Scrollable) view.findViewById(R.id.scroll);
    }

    /**
     * Called by children Fragments when their scrollY are changed.
     * They all call this method even when they are inactive
     * but this Activity should listen only the active child,
     * so each Fragments will pass themselves for Activity to check if they are active.
     *
     * @param scrollY scroll position of Scrollable
     * @param s       caller Scrollable view
     */
    public void onScrollChanged(int scrollY, Scrollable s) {
        ObservableBaseFragment fragment =
                (ObservableBaseFragment) mPagerAdapter.getItemAt(mPager.getCurrentItem());
        if (fragment == null) {
            return;
        }
        View view = fragment.getView();
        if (view == null) {
            return;
        }
        Scrollable scrollable = (Scrollable) view.findViewById(R.id.scroll);
        if (scrollable == null) {
            return;
        }
        if (scrollable == s) {
            // This method is called by not only the current fragment but also other fragments
            // when their scrollY is changed.
            // So we need to check the caller(S) is the current fragment.
            int adjustedScrollY = Math.min(scrollY, customPullDownRefreshLinearLayout.getHeaderHeight() - customPullDownRefreshLinearLayout.getTabHeight());
            customPullDownRefreshLinearLayout.translateTab(adjustedScrollY, false);
            propagateScroll(adjustedScrollY);
        }
    }

    private void propagateScroll(int scrollY) {
        // Set scrollY for the fragments that are not created yet
        mPagerAdapter.setScrollY(scrollY);

        // Set scrollY for the active fragments
        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            // Skip current item
            if (i == mPager.getCurrentItem()) {
                continue;
            }

            // Skip destroyed or not created item
            ObservableBaseFragment f =
                    (ObservableBaseFragment) mPagerAdapter.getItemAt(i);
            if (f == null) {
                continue;
            }

            View view = f.getView();
            if (view == null) {
                continue;
            }
            f.setScrollY(scrollY, customPullDownRefreshLinearLayout.getHeaderHeight());
            //f.updateFlexibleSpace(scrollY);
        }
    }

    public abstract class HoverTabFragmentStatePagerAdapter extends CacheFragmentStatePagerAdapter {

        private int mScrollY;

        public HoverTabFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setScrollY(int scrollY) {
            mScrollY = scrollY;
        }

        @Override
        protected Fragment createItem(int position) {
            ObservableBaseFragment f = createTab(position);
            f.setOnRefreshCompleteListener(new ObservableBaseFragment.OnRefreshCompleteListener(){

                @Override
                public void onRefreshComplete() {
                    if(customPullDownRefreshLinearLayout != null){
                        customPullDownRefreshLinearLayout.onRefreshComplete();
                    }
                }
            });
            f.setArguments(mScrollY, customPullDownRefreshLinearLayout.getmHeadHeight());
            return f;
        }

        @Override
        public int getCount() {
            return getTabCount();
        }

        public abstract int getTabCount();
        public abstract ObservableBaseFragment createTab(int position);
    }
}
