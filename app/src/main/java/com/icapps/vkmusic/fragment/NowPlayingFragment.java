package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.databinding.FragmentNowPlayingBinding;


public class NowPlayingFragment extends Fragment {

    FragmentNowPlayingBinding binding;

    public NowPlayingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing, container, false);
        return binding.getRoot();
    }
}
