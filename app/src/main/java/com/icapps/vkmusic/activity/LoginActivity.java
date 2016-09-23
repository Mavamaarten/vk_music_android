package com.icapps.vkmusic.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseActivity;
import com.icapps.vkmusic.databinding.ActivityLoginBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(VKSdk.isLoggedIn()){
            createUserComponentAndLaunchMainActivity(VKAccessToken.currentToken());
            return;
        }

        ActivityLoginBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.login.setOnClickListener(v -> VKSdk.login(LoginActivity.this, "audio", "offline"));
    }

    @Override
    protected void inject() {

    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(final VKAccessToken res) {
                createUserComponentAndLaunchMainActivity(res);
            }

            @Override
            public void onError(VKError error) {
                new AlertDialog.Builder(LoginActivity.this)
                        .setMessage("Login failed: " + error.errorReason)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressWarnings("unchecked")
    private void createUserComponentAndLaunchMainActivity(final VKAccessToken token){
        VKApi.users().get(VKParameters.from(VKApiConst.FIELDS, "photo_big")).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKList<VKApiUser> users = (VKList<VKApiUser>) response.parsedModel;

                ((VkApplication) getApplication()).createUserComponent(token, users.get(0));

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
