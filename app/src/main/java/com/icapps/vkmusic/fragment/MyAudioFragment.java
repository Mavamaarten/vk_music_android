package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.adapter.VkAudioAdapter;
import com.icapps.vkmusic.base.BaseFragment;
import com.icapps.vkmusic.databinding.FragmentMyAudioBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VkAudioArray;

import javax.inject.Inject;

public class MyAudioFragment extends BaseFragment {
    @Inject VKAccessToken accessToken;

    private FragmentMyAudioBinding binding;
    private VkAudioAdapter adapter;
    private VkAudioArray audioArray;

    public MyAudioFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioArray = new VkAudioArray();
        adapter = new VkAudioAdapter(audioArray);
    }

    private void loadData() {
        binding.swiperefresh.setRefreshing(true);
        VKApi.audio().get().executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                audioArray.clear();
                audioArray.addAll((VkAudioArray) response.parsedModel);
                adapter.notifyDataSetChanged();
                binding.swiperefresh.setRefreshing(false);
            }

            @Override
            public void onError(VKError error) {
                Snackbar.make(binding.swiperefresh, "Error loading audio", Snackbar.LENGTH_LONG);
                binding.swiperefresh.setRefreshing(false);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_audio, container, false);

        binding.swiperefresh.setOnRefreshListener(this::loadData);

        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);
        loadData();

        return binding.getRoot();
    }

    @Override
    protected void inject() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }
}
