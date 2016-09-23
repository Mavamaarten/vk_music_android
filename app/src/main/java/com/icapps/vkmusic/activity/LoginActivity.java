package com.icapps.vkmusic.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseActivity;
import com.icapps.vkmusic.databinding.ActivityLoginBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

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
        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VKSdk.login(LoginActivity.this, "audio", "offline");
            }
        });
    }

    @Override
    protected void inject() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
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

    private void createUserComponentAndLaunchMainActivity(VKAccessToken token){
        ((VkApplication) getApplication()).createUserComponent(token);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
