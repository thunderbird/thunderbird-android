package com.fsck.k9.account;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.oauth.OAuth2AuthorizationCodeFlowTokenProvider;
import com.fsck.k9.mail.oauth.SpecificOAuth2TokenProvider;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

class OutlookOAuth2TokenStore extends SpecificOAuth2TokenProvider {
    private Oauth2PromptRequestHandler promptRequestHandler;
    private static final String OUTLOOK_BASE_URL = "https://login.live.com/";
    private static final String REDIRECT_URI = "msala41aa976-c5ad-405f-a8e3-ed18c07bb13a://auth";
    private static final String CLIENT_ID = "a41aa976-c5ad-405f-a8e3-ed18c07bb13a";
    private static final String AUTHORIZATION_URL = "https://login.live.com/oauth20_authorize.srf?" +
            "client_id=" + CLIENT_ID + "&" +
            "scope=wl.imap wl.offline_access&" +
            "response_type=code&" +
            "redirect_uri=" + REDIRECT_URI;
    private Retrofit retrofit;
    private OutlookService service;

    OutlookOAuth2TokenStore() {
        retrofit = new Retrofit.Builder()
                .baseUrl(OUTLOOK_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(OutlookService.class);
    }

    @Override
    public OAuth2AuthorizationCodeFlowTokenProvider.Tokens exchangeCode(String username, String code) throws AuthenticationFailedException {
        Call<ExchangeResponse> call = service.exchangeCode(code, CLIENT_ID, "authorization_code", REDIRECT_URI);
        ExchangeResponse exchangeResponse;
        Response<ExchangeResponse> response;
        try {
            response = call.execute();
            exchangeResponse = response.body();
        } catch (Exception e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
        if (exchangeResponse == null || exchangeResponse.accessToken.isEmpty()) return null;

        return new OAuth2AuthorizationCodeFlowTokenProvider.Tokens(exchangeResponse.accessToken, exchangeResponse.refreshToken);
    }

    @Override
    public String refreshToken(String username, String refreshToken) throws AuthenticationFailedException {
        Call<RefreshResponse> call = service.refreshToken(CLIENT_ID, refreshToken, "refresh_token", "https://login.live.com/oauth20_desktop.srf");
        RefreshResponse response;
        try {
            response = call.execute().body();
        } catch (IOException e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
        if (response == null) {
            throw new AuthenticationFailedException("Error when getting refresh token");
        }
        return response.accessToken;
    }

    @Override
    public void showAuthDialog() {
        promptRequestHandler.handleOutlookRedirectUrl(AUTHORIZATION_URL);
    }

    void setPromptRequestHandler(Oauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
    }

    private interface OutlookService {
        @FormUrlEncoded
        @POST("oauth20_token.srf")
        Call<RefreshResponse> refreshToken(@Field("client_id") String clientId,
                                           @Field("refresh_token") String refreshToken,
                                           @Field("grant_type") String grantType,
                                           @Field("redirect_uri") String redirectUri);
        @FormUrlEncoded
        @POST("oauth20_token.srf")
        Call<ExchangeResponse> exchangeCode(@Field("code") String code,
                                            @Field("client_id") String clientId,
                                            @Field("grant_type") String grantType,
                                            @Field("redirect_uri") String redirectUri);
    }

    private static class ExchangeResponse {
        @SerializedName("access_token")
        String accessToken;
        @SerializedName("id_token")
        String idToken;
        @SerializedName("refresh_token")
        String refreshToken;
        @SerializedName("expires_in")
        int expiresIn;
        @SerializedName("token_type")
        String tokenType;
    }

    private static class RefreshResponse {
        @SerializedName("access_token")
        String accessToken;
        @SerializedName("expires_in")
        int expiresIn;
        @SerializedName("token_type")
        String tokenType;
    }
}
