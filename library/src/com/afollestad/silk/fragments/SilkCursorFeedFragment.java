package com.afollestad.silk.fragments;

import android.content.ContentResolver;
import android.os.Bundle;
import com.afollestad.silk.caching.SilkComparable;
import com.afollestad.silk.caching.SilkCursorItem;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class SilkCursorFeedFragment<ItemType extends SilkCursorItem & SilkComparable> extends SilkCursorListFragment<ItemType> {

    protected boolean mInitialLoadOnResume;
    private boolean mVisibileChangedHandled = false;

    @Override
    protected void onVisibilityChanged(boolean visible) {
        super.onVisibilityChanged(visible);
        if (visible && getView() != null) {
            mVisibileChangedHandled = true;
            onInitialRefresh();
        } else mVisibileChangedHandled = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mVisibileChangedHandled || (!isActuallyVisible() && mInitialLoadOnResume)) {
            // If onVisibilityChanged() wasn't able to handle refreshing, or; if it is visible, then onVisibilityChanged() will handle it instead
            onInitialRefresh();
        }
    }

    protected void onPreLoad() {
        clearProvider();
    }

    protected void onPostLoad(List<ItemType> items) {
        setLoadComplete(false);
        if (items.size() == 0) return;
        ContentResolver resolver = getActivity().getContentResolver();
        for (ItemType item : items)
            resolver.insert(getLoaderUri(), item.getContentValues());
        super.onInitialRefresh();
    }

    protected abstract List<ItemType> refresh() throws Exception;

    protected abstract void onError(Exception e);

    @Override
    protected void onCursorEmpty() {
        super.onCursorEmpty();
        performRefresh(true);
    }

    public void performRefresh(boolean showProgress) {
        if (isLoading()) return;
        setLoading(showProgress);
        onPreLoad();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ItemType> items = refresh();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onPostLoad(items);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onError(e);
                            setLoadComplete(true);
                        }
                    });
                }
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!mInitialLoadOnResume) onInitialRefresh();
    }
}