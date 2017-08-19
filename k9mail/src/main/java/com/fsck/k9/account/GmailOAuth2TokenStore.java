package com.fsck.k9.account;


import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public class GmailOAuth2TokenStore extends AndroidSpecificOAuth2TokenProvider {
    private static final String GOOGLE_API_BASE_URL = "https://www.googleapis.com/";
    private static final String CLIENT_ID = "486728022013-39d7vq9t06r004r7ec9m2eti0p1ihs12.apps.googleusercontent.com";
    private static final String REDIRECT_URI = "com.fsck.k9.debug:/oauth2redirect";
    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth?" +
            "scope=https://mail.google.com/&" +
            "response_type=code&" +
            "redirect_uri=" + REDIRECT_URI + "&" +
            "client_id=" + CLIENT_ID;
    private Retrofit retrofit;
    private GoogleAPIService service;

    public GmailOAuth2TokenStore() {
        retrofit = new Retrofit.Builder()
                .baseUrl(GOOGLE_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GoogleAPIService.class);
    }

    @Override
    public void showAuthDialog() {
        promptRequestHandler.handleGmailRedirectUrl(AUTHORIZATION_URL);
    }

    @Override
    protected Call<RefreshResponse> getRefreshTokenCall(String refreshToken) {
        return service.refreshToken(CLIENT_ID, refreshToken, "refresh_token");
    }

    @Override
    protected Call<ExchangeResponse> getExchangeCodeCall(String code) {
        return service.exchangeCode(code, CLIENT_ID, "authorization_code", REDIRECT_URI);
    }

    private interface GoogleAPIService {
        @FormUrlEncoded
        @POST("oauth2/v4/token")
        Call<RefreshResponse> refreshToken(@Field("client_id") String clientId,
                                           @Field("refresh_token") String refreshToken, @Field("grant_type") String grantType);
        @FormUrlEncoded
        @POST("oauth2/v4/token")
        Call<ExchangeResponse> exchangeCode(@Field("code") String code, @Field("client_id") String clientId,
                                            @Field("grant_type") String grantType, @Field("redirect_uri") String redirectUri);
    }
}
