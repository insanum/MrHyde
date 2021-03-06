package org.faudroids.mrhyde.github;


import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 * Retrofit interface for fetching the access token.
 */
public interface GitHubAuthApi {

	@POST("/login/oauth/access_token")
	@Headers("Accept: application/json")
	Observable<GitHubToken> getAccessToken(
			@Query("client_id") String clientId,
			@Query("client_secret") String clientSecret,
			@Query("code") String code);

}
