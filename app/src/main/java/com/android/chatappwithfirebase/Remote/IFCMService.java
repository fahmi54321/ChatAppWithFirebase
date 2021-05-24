package com.android.chatappwithfirebase.Remote;

import com.android.chatappwithfirebase.Model.FCMResponse;
import com.android.chatappwithfirebase.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

//todo 3 send notification (next RetrofitFCMClient)
public interface IFCMService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAFwREwBI:APA91bHf4dYuaixnn7kkv1u549qPkV_4n61oo0q_RCwPSODXaGpH1XJHflPl3ZcIc_eTGQNPtaEjJ8d_I87D5UAHQyEq-1cxJPRh7DLtpXbuYRsy_qmhGTl5_cZhTtssw4sV6kjzIdp4"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
