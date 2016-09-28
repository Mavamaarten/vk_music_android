package com.icapps.vkmusic.model.api;

/**
 * Created by maartenvangiel on 28/09/16.
 */
abstract class VkApiResponse<T> {
    private T response;
    private Object error;

    public T getResponse() {
        return response;
    }

    public Object getError() {
        return error;
    }
}
