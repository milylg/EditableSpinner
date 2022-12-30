package com.milylg.spinner;

import android.widget.BaseAdapter;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class AbsSpinnerAdapter extends BaseAdapter implements InputTextFilter {

    protected final List<String> dataSource;
    protected final List<String> matchingItemData;
    protected final int[] indexSet;


    public AbsSpinnerAdapter(List<String> data) {
        dataSource = data;
        matchingItemData = new ArrayList<>();
        indexSet = new int[dataSource.size()];
        initDisplayData(data);
    }

    public AbsSpinnerAdapter(String[] data) {
        dataSource = new ArrayList<>();
        dataSource.addAll(Arrays.asList(data));
        matchingItemData = new ArrayList<>();
        indexSet = new int[dataSource.size()];
        initDisplayData(dataSource);
    }

    protected void initDisplayData(List<String> data) {
        if (data != null && !data.isEmpty()) {
            matchingItemData.addAll(data);
        }
    }

    @Override
    public int getCount() {
        return  matchingItemData != null ? matchingItemData.size() : 0;
    }

    @Override
    public String getItem(int position) {
        if (position >= 0 && position < matchingItemData.size()) {
            return matchingItemData.get(position);
        } else {
            return "no data!";
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected String getItemValue(int position) {
        if (position >= 0 && position < dataSource.size()) {
            return dataSource.get(indexSet[position]);
        } else {
            return "";
        }
    }

    protected String getDataSourceItemValue(int index) {
        if (index >= 0 && index < dataSource.size()) {
            return dataSource.get(index);
        } else {
            return "";
        }
    }

    abstract void applyTextColor(@ColorInt int textColor);
    abstract void applyTextSize(float textSize);
    abstract void applyBackgroundSelector(@DrawableRes int backgroundSelector);
    abstract void applyFilterKeyColor(String filterColor);
    abstract void applyFilterKeyVisible(boolean isShowFilterKey);
}
