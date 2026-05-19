package com.example.myapplication.presentation.ui;

import androidx.annotation.StringRes;

public class ScreenConfiguration {

    @StringRes
    private final int titleRes;
    private final boolean showBackButton;
    private final boolean showFab;
    private final boolean showRefresh;

    public ScreenConfiguration(
            @StringRes int titleRes,
            boolean showBackButton,
            boolean showFab,
            boolean showRefresh
    ) {
        this.titleRes = titleRes;
        this.showBackButton = showBackButton;
        this.showFab = showFab;
        this.showRefresh = showRefresh;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public boolean isShowBackButton() {
        return showBackButton;
    }

    public boolean isShowFab() {
        return showFab;
    }

    public boolean isShowRefresh() {
        return showRefresh;
    }
}
