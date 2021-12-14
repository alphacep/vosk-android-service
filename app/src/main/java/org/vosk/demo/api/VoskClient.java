package org.vosk.demo.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class VoskClient {

    private static final String TAG = "DownloadAPI";
    private static final int DEFAULT_TIMEOUT = 15;
    public static Retrofit retrofit;
    private static final String BASE_URL = "https://alphacephei.com/vosk/models/";

    public static VoskService getClient(DownloadProgressListener listener, ServiceType serviceType) {

        DownloadProgressInterceptor interceptor = new DownloadProgressInterceptor(listener);

        OkHttpClient client = serviceType == ServiceType.DOWNLOAD_MODEL ? new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addNetworkInterceptor(interceptor)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build()
                : new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
        return retrofit.create(VoskService.class);
    }

    public enum ServiceType {
        DOWNLOAD_MODEL,
        DOWNLOAD_MODEL_LIST
    }
}
