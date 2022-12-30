package com.milylg.spinner;

import android.content.res.Configuration;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.List;


public class InputTextAdapter extends AbsSpinnerAdapter implements InputTextFilter {

    private int textColor;
    private float textSize;
    private int itemBackgroundSelectorResId;
    private String filterKeyColor =  "#E09070";
    private boolean filterKeyVisible = false;


    public InputTextAdapter(List<String> data) {
        super(data);
    }

    public InputTextAdapter(String[] data) {
        super(data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.layout_list_item, parent, false);
            holder = new ViewHolder(convertView, textColor, textSize, itemBackgroundSelectorResId);
            convertView.setTag(holder);
        } else {
            holder = ((ViewHolder) convertView.getTag());
        }
        holder.mTextView.setText(Html.fromHtml(getItem(position)));
        return convertView;
    }

    @Override
    public boolean hasFilterResultAbout(String keyword) {
        matchingItemData.clear();
        if (keyword == null || keyword.isEmpty()) {

            initDisplayData(dataSource);
            for (int i = 0; i < indexSet.length; i++) {
                indexSet[i] = i;
            }
        } else {

            String keyPatten = "[^\\s]*" + keyword + "[^\\s]*";

            for (int i = 0; i < dataSource.size(); i++) {
                String itemValue = getDataSourceItemValue(i);
                if (itemValue.replaceAll("\\s+", "|").matches(keyPatten)) {
                    indexSet[matchingItemData.size()] = i;
                    if (filterKeyVisible) {
                        String markKey = "<font color=\"" + filterKeyColor + "\">" + keyword + "</font>";
                        String markKeyText = getDataSourceItemValue(i).replaceFirst(keyword, markKey);
                        matchingItemData.add(markKeyText);
                    } else {
                        matchingItemData.add(getDataSourceItemValue(i));
                    }
                }
            }

        }
        notifyDataSetChanged();
        return matchingItemData.size() > 0;
    }

    public void applyTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
    }

    public void applyTextSize(float textSize) {
        this.textSize = textSize;
    }

    @Override
    public void applyBackgroundSelector(@DrawableRes int backgroundSelector) {
        this.itemBackgroundSelectorResId = backgroundSelector;
    }

    @Override
    void applyFilterKeyColor(String filterColor) {
        this.filterKeyColor = filterColor;
    }

    @Override
    public void applyFilterKeyVisible(boolean filterKeyVisible) {
        this.filterKeyVisible = filterKeyVisible;
    }

    private static class ViewHolder {

        private final TextView mTextView;

        private ViewHolder(@NonNull View convertView,
                           @ColorInt int textColor,
                           float textSize,
                           @DrawableRes int backgroundSelector) {

            mTextView = convertView.findViewById(R.id.tv_tinted_spinner);
            mTextView.setTextColor(textColor);
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            if (backgroundSelector != 0) {
                mTextView.setBackgroundResource(backgroundSelector);
            }
            Configuration config = convertView.getResources().getConfiguration();
            if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mTextView.setTextDirection(View.TEXT_DIRECTION_RTL);
            }
        }
    }

}
