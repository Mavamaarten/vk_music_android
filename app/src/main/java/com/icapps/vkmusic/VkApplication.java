package com.icapps.vkmusic;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.icapps.vkmusic.activity.LoginActivity;
import com.icapps.vkmusic.di.application.AppComponent;
import com.icapps.vkmusic.di.application.AppModule;
import com.icapps.vkmusic.di.application.DaggerAppComponent;
import com.icapps.vkmusic.di.user.UserComponent;
import com.icapps.vkmusic.di.user.UserModule;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

/**
 * Created by maartenvangiel on 13/09/16.
 */
public class VkApplication extends Application {
    private AppComponent appComponent;
    private UserComponent userComponent;
    private VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(@Nullable VKAccessToken oldToken, @Nullable VKAccessToken newToken) {
            if (newToken == null) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();

        vkAccessTokenTracker.startTracking();
        VKSdk.initialize(this);
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    public UserComponent getUserComponent() {
        return userComponent;
    }

    public UserComponent createUserComponent(VKAccessToken accessToken) {
        userComponent = appComponent.plus(new UserModule(accessToken));
        return userComponent;
    }

    public void releaseUserComponent() {
        userComponent = null;
    }
}
