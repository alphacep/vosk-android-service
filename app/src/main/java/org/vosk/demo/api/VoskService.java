package org.vosk.demo.api;

import org.vosk.demo.ui.model_list.ModelItem;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface VoskService {
    @Streaming
    @GET
    Observable<ResponseBody> downloadFile(@Url String url);

    @GET("model-list.json")
    Observable<List<ModelItem>> getModelList();
}