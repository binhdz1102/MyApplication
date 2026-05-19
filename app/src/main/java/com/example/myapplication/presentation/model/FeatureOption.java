package com.example.myapplication.presentation.model;

import androidx.annotation.StringRes;

public class FeatureOption {

    public enum Destination {
        AIDL_USERS,
        ROOM_USERS,
        AIDL_ROOM_USERS,
        MUSIC_PLAYER
    }

    private final Destination destination;

    @StringRes
    private final int titleRes;

    @StringRes
    private final int messageRes;

    public FeatureOption(
            Destination destination,
            @StringRes int titleRes,
            @StringRes int messageRes
    ) {
        this.destination = destination;
        this.titleRes = titleRes;
        this.messageRes = messageRes;
    }

    public Destination getDestination() {
        return destination;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getMessageRes() {
        return messageRes;
    }
}
