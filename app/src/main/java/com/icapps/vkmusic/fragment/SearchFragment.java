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
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentSearchBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import javax.inject.Inject;

public class SearchFragment extends BaseMusicFragment implements VkAudioAdapter.VkAudioAdapterListener {
    @Inject VKAccessToken accessToken;

    private FragmentSearchBinding binding;
    private VkAudioAdapter adapter;
    private VkAudioArray audioArray;

    public SearchFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioArray = new VkAudioArray();
        adapter = new VkAudioAdapter(audioArray, this, getContext(), false, null);
    }

    public void search(String searchQuery) {
        if(searchQuery == null){
            binding.loadingIndicator.setVisibility(View.GONE);
            return;
        }

        binding.loadingIndicator.setVisibility(View.VISIBLE);
        VKApi.audio().search(VKParameters.from(VKApiConst.Q, searchQuery, VKApiConst.COUNT, 100)).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                audioArray.clear();
                audioArray.addAll((VkAudioArray) response.parsedModel);
                adapter.notifyDataSetChanged();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(VKError error) {
                Snackbar.make(binding.rcvAudio, "Error loading search results", Snackbar.LENGTH_LONG);
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);

        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);

        if(getArguments() != null && getArguments().containsKey("query")){
            search(getArguments().getString("query"));
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.setCurrentAudio(currentAudio.get());
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    @Override
    protected void onCurrentAudioChanged(VKApiAudio currentAudio) {
        adapter.setCurrentAudio(currentAudio);
    }

    @Override
    public void onAudioClicked(VKApiAudio audio, int position) {
        musicService.playAudio(audioArray, position);
    }

    @Override
    public void onCurrentAudioMoved(int toPosition) {
        // Not applicable to SearchFragment
    }

    @Override
    public boolean onAudioMenuItemClicked(VKApiAudio audio, int position, int menuItemId) {
        switch(menuItemId){
            case R.id.action_play:
                musicService.playAudio(audioArray, position);
                break;

            case R.id.action_play_next:
                musicService.addTrackAsNextInQueue(audio);
                break;

            case R.id.action_add_to_playlist:
                // TODO: implement
                break;
        }
        return true;
    }
}
