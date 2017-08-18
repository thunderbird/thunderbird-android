package com.fsck.k9.account;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public class GmailOAuth2TokenStore implements OAuth2TokenProvider {
    private Map<String,String> authTokens = new HashMap<>();
    private GMailXOauth2PromptRequestHandler promptRequestHandler;
    private static final String REFRESH_TOKEN_SP = "refresh_token";
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
    private Context context;

    public GmailOAuth2TokenStore(Context context) {
        setContext(context);
        retrofit = new Retrofit.Builder()
                .baseUrl(GOOGLE_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GoogleAPIService.class);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setPromptRequestHandler(GMailXOauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
    }

    public SharedPreferences getSharedPreference() {
        return context.getSharedPreferences(REFRESH_TOKEN_SP, Context.MODE_PRIVATE);
    }

    @Override
    public List<String> getAccounts() {
        return null;
    }

    @Override
    public String getToken(String username, long timeoutMillis)
            throws AuthenticationFailedException, OAuth2NeedUserPromptException {
        if (!authTokens.containsKey(username)) {
            try {
                fetchNewToken(username);
            } catch (OAuth2NeedUserPromptException e) {
                throw e;
            } catch (Exception e) {
                throw new AuthenticationFailedException(e.getMessage());
            }
        }
        return authTokens.get(username);
    }

    private void fetchNewToken(String username) throws IOException, OAuth2NeedUserPromptException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(REFRESH_TOKEN_SP, Context.MODE_PRIVATE);
        String refreshToken = sharedPreferences.getString(username, "");
        if (!refreshToken.equals("")) {
            Call<RefreshResponse> call = service.refreshToken(CLIENT_ID, refreshToken, "refresh_token");
            RefreshResponse response = call.execute().body();
            if (response == null) return;
            authTokens.put(username, response.accessToken);
        } else {
            // The first time
            promptRequestHandler.handleRedirectUrl(AUTHORIZATION_URL);
            throw new OAuth2NeedUserPromptException();
        }
    }

    @Override
    public boolean exchangeCode(String username, String code) {
        Call<ExchangeResponse> call = service.exchangeCode(code, CLIENT_ID, "authorization_code", REDIRECT_URI);
        ExchangeResponse exchangeResponse;
        Response<ExchangeResponse> response;
        try {
            response = call.execute();
            exchangeResponse = response.body();
        } catch (IOException e) {
            return false;
        }
        if (exchangeResponse == null || exchangeResponse.accessToken.isEmpty()) return false;
        authTokens.put(username, exchangeResponse.accessToken);
        SharedPreferences sharedPreferences = context.getSharedPreferences(REFRESH_TOKEN_SP, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(username, exchangeResponse.refreshToken).apply();
        return true;
    }

    @Override
    public void invalidateToken(String username) {
        authTokens.remove(username);
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
