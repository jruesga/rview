/*
 * Copyright (C) 2017 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.rview.attachments.gdrive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ruesga.rview.attachments.gdrive.model.FileMetadata;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataPageInfo;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadata;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadataPageInfo;
import com.ruesga.rview.attachments.gdrive.oauth.AccessToken;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GDriveRestApi {

    String OAUTH_REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob";
    String OAUTH_GRANT_TYPE_AUTH_TOKEN = "authorization_code";
    String OAUTH_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    // OAuth2 EntryPoints

    @FormUrlEncoded
    @POST("oauth2/v4/token")
    Observable<AccessToken> requestAccessToken(
            @NonNull @Field("code") String code,
            @NonNull @Field("client_id") String clientId,
            @NonNull @Field("client_secret") String clientSecret,
            @NonNull @Field("redirect_uri") String redirectUri,
            @NonNull @Field("grant_type") String grantType);

    @FormUrlEncoded
    @POST("oauth2/v4/token")
    Observable<AccessToken> refreshAccessToken(
            @NonNull @Field("refresh_token") String refreshToken,
            @NonNull @Field("client_id") String clientId,
            @NonNull @Field("client_secret") String clientSecret,
            @NonNull @Field("grant_type") String grantType);

    @GET("oauth2/v4/tokeninfo")
    Observable<AccessToken> requestTokenInfo(
            @NonNull @Query("access_token") String accessToken);



    // Drive EntryPoints

    @GET("drive/v3/files")
    Observable<FileMetadataPageInfo> listFiles(
            @Nullable @Query("pageSize") Integer pageSize,
            @Nullable @Query("pageToken") String pageToken,
            @Nullable @Query("q") String query);

    @POST("drive/v3/files")
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    Observable<FileMetadata> createFileMetadata(
            @NonNull @Body FileMetadataInput input);

    @PATCH("upload/drive/v3/files/{file-id}")
    Observable<FileMetadata> uploadFileContent(
            @NonNull @Path("file-id") String fileId,
            @NonNull @Body RequestBody input);

    @GET("drive/v3/files/{file-id}/permissions")
    Observable<PermissionMetadataPageInfo> listFilePermissions(
            @NonNull @Path("file-id") String fileId,
            @Nullable @Query("pageSize") Integer pageSize,
            @Nullable @Query("pageToken") String pageToken);

    @POST("drive/v3/files/{file-id}/permissions")
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    Observable<PermissionMetadata> createFilePermission(
            @NonNull @Path("file-id") String fileId,
            @NonNull @Body PermissionMetadataInput input);
}
