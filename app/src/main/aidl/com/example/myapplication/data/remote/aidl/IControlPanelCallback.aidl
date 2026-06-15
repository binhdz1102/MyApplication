package com.example.myapplication.data.remote.aidl;

interface IControlPanelCallback {
    void onStateChanged(int rating, int temperature, boolean toggledOn);
}
