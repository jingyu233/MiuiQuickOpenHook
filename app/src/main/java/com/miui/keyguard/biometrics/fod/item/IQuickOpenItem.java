package com.miui.keyguard.biometrics.fod.item;

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;

/**
 * SystemUI 里的 IQuickOpenItem 的编译期 stub
 */
public abstract class IQuickOpenItem {

    public final Context mContext;
    public final RectF mRectF;
    public final Region mRegion;

    public IQuickOpenItem(RectF rectF, Region region, Context context) {
        this.mRectF = rectF;
        this.mRegion = region;
        this.mContext = context;
    }

    /** 启动意图 */
    public abstract Intent getIntent();

    /** 主标题 */
    public abstract String getTitle();

    /** 副标题 */
    public abstract String getSubTitle();

    /** 标签(用于日志等) */
    public abstract String getTag();

    /** 用于显示的 View */
    public abstract View getView();
}
