package com.icapps.vkmusic.model.albumart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import rx.Observable;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class BingAlbumArtProvider implements AlbumArtProvider {

    private final static String image_delimiter = "class=\"thumb\" target=\"_blank\" href=\"";

    public Observable<String> getAlbumArtUrl(String query) {
        return Observable.fromCallable(() -> getAlbumArtUrlString(query));
    }

    private String getAlbumArtUrlString(String query) throws IOException, NoAlbumArtFoundException {
        URL url;
        url = new URL("http://www.bing.com/?q=" + URLEncoder.encode(query, "UTF-8") + "&scope=images&qft=+filterui:aspect-square");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Googlebot/2.1 (+http://www.google.com/bot.html)");
        urlConnection.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        StringBuilder result = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            result.append(inputLine);
        }

        return ParseImageUrlFromResult(result.toString());
    }

    private String ParseImageUrlFromResult(String result) throws NoAlbumArtFoundException {
        if (!result.contains(image_delimiter)) throw new NoAlbumArtFoundException();
        result = result.split(image_delimiter)[1];
        result = result.substring(0, result.indexOf('"'));
        return result;
    }

    public class NoAlbumArtFoundException extends Exception{

        @Override
        public String getMessage() {
            return "No album art was found";
        }
    }
}
