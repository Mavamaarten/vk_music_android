package com.icapps.vkmusic.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import icepick.Icepick;

/**
 * Created by maartenvangiel on 22/09/16.
 */
public abstract class BaseFragment extends Fragment {
    protected abstract void inject();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        inject();
        Icepick.restoreInstanceState(this, savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }
}
