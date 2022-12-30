package com.milylg.spinner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 可编辑的Spinner
 * <p>
 * //顺时针旋转45°
 * ObjectAnimator rotate = ObjectAnimator.ofFloat(iv_shai_float_menu, "rotation", 0f, 45f).setDuration(300);
 * rotate.setInterpolator(new LinearInterpolator());
 * rotate.start();
 * <p>
 * //逆时针旋转45°
 * ObjectAnimator rotate = ObjectAnimator.ofFloat(iv_shai_float_menu, "rotation", 45f, 0f).setDuration(300);
 * rotate.setInterpolator(new LinearInterpolator());
 * rotate.start();
 *
 * 设置箭头图标的大小
 * symbolArrowSize = typedArray.getDimensionPixelSize(
 *                   R.styleable.editable_spinner_symbolArrowSize,
 *                   dropDownDrawable.getMinimumWidth());
 * 必须设置边框，否则图标不显示
 * dropDownDrawable.setBounds(0, 0, symbolArrowSize, symbolArrowSize);
 * this.setCompoundDrawables(dropDownDrawable, null, null, null);
 *
 * setCompoundDrawablesWithIntrinsicBounds是画的drawable的宽高是按drawable固定的宽高，
 * 即通过getIntrinsicWidth()与getIntrinsicHeight()获得
 */
public class EditableSpinner extends AppCompatEditText
        implements AdapterView.OnItemClickListener, Observable, TextWatcher {

    private static final int DEFAULT_MAX_LINE = 1;
    private static final int TOGGLE_POPUP_WINDOW_INTERVAL = 200;
    private static final int DRAWABLE_RIGHT = 2;

    private AdapterView.OnItemClickListener itemClickExpandAction;
    private WeakReference<ListPopupWindow> popupWindow;
    private AbsSpinnerAdapter adapter;
    private long popupWindowHideTime;

    // Attributes Set
    private boolean filterDataVisible = true;
    private boolean filterKeyVisible = false;
    private Drawable dropDownDrawable;
    private int dropdownOffset;
    private Drawable dropDownBackground;
    private int popAnimationStyle;



    public EditableSpinner(Context context) {
        this(context, null);
    }


    public EditableSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spinner_style);
    }


    public EditableSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addTextChangedListener(this);
        loadAttributes(context, attrs, defStyleAttr);
        initializedAnimation();
    }


    private void loadAttributes(Context context, AttributeSet attrs, int defStyleAttr) {

        @SuppressLint("CustomViewStyleable")
        TypedArray typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.editable_spinner, defStyleAttr, 0);

        // 设置Spinner的箭头图标与输入框边界的间隔距离
        int arrowMargin = typedArray.getDimensionPixelOffset(
                R.styleable.editable_spinner_arrowMargin, 0);
        setCompoundDrawablePadding(arrowMargin);

        // 设置输入框的上下内边距
        int verticalOffset = resolveDimension(context, R.attr.verticalPadding);
        setPadding(getPaddingLeft(),verticalOffset, getPaddingRight(), verticalOffset);

        // 设置箭头图标的位置（左边/右边）
        dropDownDrawable = typedArray.getDrawable(
                R.styleable.editable_spinner_arrowDrawableLeft);
        if (dropDownDrawable != null) {
            this.setCompoundDrawablesWithIntrinsicBounds(
                    dropDownDrawable, null, null, null);
        }

        dropDownDrawable = typedArray.getDrawable(
                R.styleable.editable_spinner_arrowDrawableRight);
        if (dropDownDrawable != null) {
            this.setCompoundDrawablesWithIntrinsicBounds(
                    null, null, dropDownDrawable, null);
        }

        // 在xml中配置Spinner的下拉数据源
        int entriesId = typedArray.getResourceId(R.styleable.editable_spinner_entries, 0);
        if (entriesId != 0) {
            setItems(getResources().getStringArray(entriesId));
        }

        // 默认设置单行
        setMaxLines(DEFAULT_MAX_LINE);

        // 设置DropDownPopView的背景图片
        dropDownBackground = getDrawableAttrRes(getContext(), typedArray,
                R.styleable.editable_spinner_dropdownBackground);

        // 设置输入下拉框在输入时是否显示筛选到的信息
        filterDataVisible = typedArray.getBoolean(
                R.styleable.editable_spinner_filterDataVisible, true);
        // 设置输入下拉框显示筛选信息时，是否显示key为醒目的颜色
        filterKeyVisible = typedArray.getBoolean(
                R.styleable.editable_spinner_filterKeyVisible, false);

        // 设置下拉弹窗的偏移距离
        dropdownOffset = typedArray.getDimensionPixelSize(
                R.styleable.editable_spinner_dropdownOffset, 10);

        // 设置点击箭头动画反馈 - 旋转180度
        popAnimationStyle = typedArray.getResourceId(
                R.styleable.editable_spinner_popupAnimationStyle, -1);

        typedArray.recycle();
    }


    private int resolveDimension(Context context, @AttrRes int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getDimensionPixelSize(0, -1);
        } finally {
            a.recycle();
        }
    }

    private Drawable getDrawableAttrRes(Context context,
                                        TypedArray typedArray,
                                        @StyleableRes int styleableResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return typedArray.getDrawable(styleableResId);
        } else {
            int resourceId = typedArray.getResourceId(styleableResId, -1);
            if (resourceId != -1) {
                return AppCompatResources.getDrawable(context, resourceId);
            }
        }
        return null;
    }

    private void initializedAnimation() {
        if (getCompoundDrawables()[2] != null) {
            // TODO:初始化输入框右边图标动画
        }

        if (getCompoundDrawables()[0] != null) {
            // TODO:初始化输入框左边图标动画
        }
    }


    private ListPopupWindow buildPopupWindow() {

        ListPopupWindow popupWindow = new ListPopupWindow(getContext()) {

            @Override
            public void show() {
                if (!isShowing()) {
                    // TODO: 执行ArrowBitmap顺时针旋转180°
                }
                super.show();
            }
        };

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popupWindow.setOnItemClickListener(this);
        popupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        popupWindow.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);
        popupWindow.setAnchorView(this);
        popupWindow.setVerticalOffset(dropdownOffset);
        popupWindow.setListSelector(
                AppCompatResources.getDrawable(getContext(), R.drawable.item_selector));
        popupWindow.setOnDismissListener(
                () -> {
                    popupWindowHideTime = System.currentTimeMillis();
                    // TODO:执行ArrowBitmap逆时针旋转180°
                }
        );
        if (dropDownBackground != null) {
            popupWindow.setBackgroundDrawable(dropDownBackground);
        } else {
            popupWindow.setBackgroundDrawable(
                    AppCompatResources.getDrawable(getContext(), R.drawable.bg_drop_down_radius)
            );
        }
        if (popAnimationStyle != -1) {
            popupWindow.setAnimationStyle(popAnimationStyle);
        }
        return popupWindow;
    }

    @Override
    public final void onItemClick(
            AdapterView<?> parent, View view, int position, long id) {
        setSelectText(parent, position);
        dismissDropDown();
        handleItemClickExpandAction(parent, view, position, id);
        clearFocus();
        setCursorVisible(false);
    }

    private void setSelectText(AdapterView<?> parent, int position) {
        AbsSpinnerAdapter adapter = ((AbsSpinnerAdapter) parent.getAdapter());
        if (adapter != null) {
            String selectContent = adapter.getItemValue(position);
            setText(selectContent);
        } else {
            throw new RuntimeException("AbsSpinnerAdapter is null!");
        }
    }

    private void handleItemClickExpandAction(
            AdapterView<?> parent, View view, int position, long id) {
        if (itemClickExpandAction != null) {
            itemClickExpandAction.onItemClick(parent, view, position, id);
        }
    }

    // Solve multiple cursor problems
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            requestFocus();
            setCursorVisible(true);
        } else {
            clearFocus();
            setCursorVisible(false);
        }
    }


    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * are about to be replaced by new text with length <code>after</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing...
    }

    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * have just replaced old text that had length <code>before</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     */
    @Override
    public final void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing...
    }

    /**
     * This method is called to notify you that, somewhere within
     * <code>s</code>, the text has been changed.
     * It is legitimate to make further changes to <code>s</code> from
     * this callback, but be careful not to get yourself into an infinite
     * loop, because any changes you make will cause this method to be
     * called again recursively.
     * (You are not told where the change took place because other
     * afterTextChanged() methods may already have made other changes
     * and invalidated the offsets.  But if you need to know here,
     * you can use {@link Spannable#setSpan} in {@link #onTextChanged}
     * to mark your place and then look up from here where the span
     * ended up.
     */
    @Override
    public void afterTextChanged(Editable s) {
        String key = s.toString();
        if (!TextUtils.isEmpty(key)) {
            if (filterDataVisible && isFocused()) {
                showFilterData(key);
            }
        } else {
            dismissDropDown();
        }
    }

    private void showFilterData(String key) {
        if (popupWindow == null
                || adapter == null) {
            dismissDropDown();
            return;
        }

        if (adapter.hasFilterResultAbout(key)) {
            showDropDown();
        } else {
            dismissDropDown();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP
                && getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
            int drawableStartX = this.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
            float touchX = event.getX();
            if (touchX >= (this.getWidth() - drawableStartX)) {
                this.togglePopupWindow();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void showDropDown() {
        ListPopupWindow popupWindow = getPopupWindow();
        if (popupWindow != null) {
            popupWindow.show();
        }
    }

    private void dismissDropDown() {
        ListPopupWindow popupWindow = getPopupWindow();
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }


    private void togglePopupWindow() {

        if (adapter == null || popupWindow == null) {
            return;
        }

        long toggleIntervalTime = System.currentTimeMillis() - popupWindowHideTime;
        boolean moreThanToggleIntervalTime = toggleIntervalTime > TOGGLE_POPUP_WINDOW_INTERVAL;
        if (moreThanToggleIntervalTime) {
            showFilterData("");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycle();
    }


    private void recycle() {
        if (dropDownDrawable != null) {
            // TODO:清除动画，如果可以。
        }
        // TODO:关闭动画资源
        setAdapter(null);
        dismissDropDown();
    }

    private ListPopupWindow getPopupWindow() {
        return popupWindow != null ? popupWindow.get() : null;
    }


    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // 设置drawable的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE
                ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    // User Configuration Interface

    public void setItems(String[] data) {
        adapter = new InputTextAdapter(data);
        adapter.applyTextColor(this.getCurrentTextColor());
        adapter.applyTextSize(this.getTextSize());
        adapter.applyFilterKeyVisible(filterKeyVisible);
        setAdapter(adapter);
    }

    public void setItems(List<String> data) {
        adapter = new InputTextAdapter(data);
        adapter.applyTextColor(this.getCurrentTextColor());
        adapter.applyTextSize(this.getTextSize());
        adapter.applyFilterKeyVisible(filterKeyVisible);
        setAdapter(adapter);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        itemClickExpandAction = listener;
    }

    public String textValue() {
        return getText() == null ? "" : getText().toString();
    }

    public void setTextColors(ColorStateList colors) {
        if (colors != null) {
            setTextColor(colors);
            if (adapter != null) {
                adapter.applyTextColor(colors.getDefaultColor());
            }
        }
    }

    public void applyTextColor(@ColorInt int color) {

        setTextColor(color);
        if (adapter != null) {
            adapter.applyTextColor(color);
        }
    }

    public void applyFilterKeyVisible(boolean isShowFilterKey) {
        if (adapter != null) {
            adapter.applyFilterKeyVisible(isShowFilterKey);
        }
    }

    public void applyFilterColor(String filterColor) {
        if (adapter != null) {
            adapter.applyFilterKeyColor(filterColor);
        }
    }

    public void applyTextSize(float textSize) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        if (adapter != null) {
            adapter.applyTextSize(textSize);
        }
    }

    public void applyBackgroundColorResId(int backgroundColorResId) {
        if (adapter != null) {
            adapter.applyBackgroundSelector(backgroundColorResId);
        }
    }

    public void setAdapter(AbsSpinnerAdapter adapter) {

        this.adapter = adapter;

        if (popupWindow == null) {
            popupWindow = new WeakReference<>(buildPopupWindow());
        }

        ListPopupWindow popupWindow = getPopupWindow();
        if (popupWindow != null) {
            popupWindow.setAdapter(adapter);
        }
    }

    private transient PropertyChangeRegistry mCallbacks;

    @Override
    public void addOnPropertyChangedCallback(@NonNull OnPropertyChangedCallback callback) {
        synchronized (this) {
            if (mCallbacks == null) {
                mCallbacks = new PropertyChangeRegistry();
            }
        }
        mCallbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(@NonNull OnPropertyChangedCallback callback) {
        synchronized (this) {
            if (mCallbacks == null) {
                return;
            }
        }
        mCallbacks.remove(callback);
    }

}
