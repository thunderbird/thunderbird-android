package com.fsck.k9.account;


import java.io.IOException;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.SpecificOAuth2TokenProvider;
import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public class GmailOAuth2TokenStore implements SpecificOAuth2TokenProvider {
    private XOauth2PromptRequestHandler promptRequestHandler;
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

    public void setPromptRequestHandler(XOauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
    }

    @Override
    public String refreshToken(String username, String refreshToken) throws AuthenticationFailedException {
        Call<RefreshResponse> call = service.refreshToken(CLIENT_ID, refreshToken, "refresh_token");
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
        promptRequestHandler.handleGmailRedirectUrl(AUTHORIZATION_URL);
    }

    @Override
    public OAuth2TokenProvider.Tokens exchangeCode(String username, String code) throws AuthenticationFailedException {
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

        return new OAuth2TokenProvider.Tokens(exchangeResponse.accessToken, exchangeResponse.refreshToken);
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
