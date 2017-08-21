package com.fsck.k9.account;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.oauth.OAuth2AuthorizationCodeFlowTokenProvider;
import com.fsck.k9.mail.oauth.SpecificOAuth2TokenProvider;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * base class for OAuth 2.0 standard authorization code flow
 * Classes extended this should override two methods that return Retrofit's calls
 * For other OAuth 2.0 flows, please create another TokenProvider
 */
abstract class AndroidSpecificOAuth2TokenProvider extends SpecificOAuth2TokenProvider {
    protected Oauth2PromptRequestHandler promptRequestHandler;

    protected abstract Call<ExchangeResponse> getExchangeCodeCall(String code);
    protected abstract Call<RefreshResponse> getRefreshTokenCall(String code);

    void setPromptRequestHandler(Oauth2PromptRequestHandler promptRequestHandler) {
        this.promptRequestHandler = promptRequestHandler;
    }

    public OAuth2AuthorizationCodeFlowTokenProvider.Tokens exchangeCode(String username, String code) throws AuthenticationFailedException {
        Call<ExchangeResponse> call = getExchangeCodeCall(code);
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

    public String refreshToken(String username, String refreshToken) throws AuthenticationFailedException {
        Call<RefreshResponse> call = getRefreshTokenCall(refreshToken);
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

    protected static class ExchangeResponse {
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

    protected static class RefreshResponse {
        @SerializedName("access_token")
        String accessToken;
        @SerializedName("expires_in")
        int expiresIn;
        @SerializedName("token_type")
        String tokenType;
    }
}
