package org.vosk.service.download;

import org.vosk.service.ui.selector.ModelItem;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface VoskModelStorage {
    @Streaming
    @GET
    Observable<ResponseBody> downloadFile(@Url String url);

    @GET("model-list.json")
    Observable<List<ModelItem>> getModelList();
}