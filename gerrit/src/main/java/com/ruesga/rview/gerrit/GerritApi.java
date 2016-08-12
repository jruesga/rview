/*
 * Copyright (C) 2016 Jorge Ruesga
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
package com.ruesga.rview.gerrit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountInput;
import com.ruesga.rview.gerrit.model.AccountNameInput;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.ConfigInput;
import com.ruesga.rview.gerrit.model.GcInput;
import com.ruesga.rview.gerrit.model.HeadInput;
import com.ruesga.rview.gerrit.model.ProjectAccessInfo;
import com.ruesga.rview.gerrit.model.ProjectDescriptionInput;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectInput;
import com.ruesga.rview.gerrit.model.ProjectParentInput;
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.gerrit.model.UsernameInput;

import java.util.List;
import java.util.Map;

import okhttp3.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Gerrit REST api
 */
public interface GerritApi {

    double VERSION = 2.12;

    // ===============================
    // Gerrit access endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html#list-access"
     */
    @GET("access/")
    Observable<Map<String, ProjectAccessInfo>> getAccessRights(
            @NonNull @Query("project") String[] names);



    // ===============================
    // Gerrit accounts endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    @GET("accounts/")
    Observable<List<AccountInfo>> getAccountsSuggestions(
            @NonNull @Query("q") String query,
            @Nullable @Query("n") Integer count);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    @GET("accounts/")
    Observable<List<AccountInfo>> getAccounts(
            @NonNull @Query("q") AccountQuery query,
            @Nullable @Query("n") Integer count,
            @Nullable @Query("S") Integer start,
            @Nullable @Query("o") AccountOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account"
     */
    @GET("accounts/{account-id}")
    Observable<AccountInfo> getAccount(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account"
     */
    @GET("accounts/self")
    Observable<AccountInfo> getSelfAccount();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#create-account"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{username}")
    Observable<AccountInfo> createAccount(
            @NonNull @Path("username") String username,
            @NonNull @Body AccountInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-detail"
     */
    @GET("accounts/{account-id}/detail")
    Observable<AccountDetailInfo> getAccountDetails(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-detail"
     */
    @GET("accounts/self/detail")
    Observable<AccountDetailInfo> getSelfAccountDetails();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-name"
     */
    @GET("accounts/{account-id}/name")
    Observable<String> getAccountName(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-name"
     */
    @GET("accounts/self/name")
    Observable<String> getSelfAccountName();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-account-name"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/name")
    Observable<String> setAccountName(
            @Path("account-id") int accountId,
            @NonNull @Body AccountNameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-account-name"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/self/name")
    Observable<String> setSelfAccountName(@NonNull @Body AccountNameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-name"
     */
    @DELETE("accounts/{account-id}/name")
    Observable<Void> deleteAccountName(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-name"
     */
    @DELETE("accounts/self/name")
    Observable<Void> deleteSelfAccountName();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-username"
     */
    @GET("accounts/{account-id}/username")
    Observable<String> getAccountUsername(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-username"
     */
    @GET("accounts/self/username")
    Observable<String> getSelfAccountUsername();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-username"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/username")
    Observable<String> setAccountUsername(
            @Path("account-id") int accountId,
            @NonNull @Body UsernameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-username"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/self/username")
    Observable<String> setSelfAccountUsername(@NonNull @Body UsernameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-active"
     */
    @GET("accounts/{account-id}/active")
    Observable<String> isAccountActive(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-active"
     */
    @PUT("accounts/{account-id}/active")
    Observable<Void> setAccountAsActive(@Path("account-id") int accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-active"
     */
    @DELETE("accounts/{account-id}/active")
    Observable<Void> setAccountAsInactive(@Path("account-id") int accountId);



    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-changes"
     */
    @GET("changes/")
    Observable<List<ChangeInfo>> getChanges(
            @NonNull @Query("q") ChangeQuery query,
            @Nullable @Query("n") Integer count,
            @Nullable @Query("S") Integer start,
            @Nullable @Query("o") ChangeOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-change"
     */
    @GET("changes/{change-id}")
    Observable<ChangeInfo> getChange(
            @NonNull @Path("change-id") String changeId,
            @Nullable @Query("o") ChangeOptions[] options);


    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-version"
     */
    @GET("config/server/version")
    Observable<ServerVersion> getServerVersion();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-info"
     */
    @GET("config/server/info")
    Observable<ServerInfo> getServerInfo();

    // ===============================
    // Gerrit groups endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html"
    // ===============================

    // ===============================
    // Gerrit plugins endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html"
    // ===============================

    // ===============================
    // Gerrit projects endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-projects"
     */
    @GET("projects/")
    Observable<Map<String, ProjectInfo>> getProjects(
            @Nullable @Query("d") Boolean showDescription,
            @Nullable @Query("t") Boolean showTree,
            @Nullable @Query("b") String branch,
            @Nullable @Query("type") ProjectType type,
            @Nullable @Query("has-acl-for") String group);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project"
     */
    @GET("projects/{project-name}")
    Observable<ProjectInfo> getProject(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-project"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}")
    Observable<ProjectInfo> createProject(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-description"
     */
    @GET("projects/{project-name}/description")
    Observable<String> getProjectDescription(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-description"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/description")
    Observable<String> setProjectDescription(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectDescriptionInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-project-description"
     */
    @DELETE("projects/{project-name}/description")
    Observable<Void> deleteProjectDescription(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-parent"
     */
    @GET("projects/{project-name}/parent")
    Observable<String> getProjectParent(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-parent"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/parent")
    Observable<String> setProjectParent(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectParentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-head"
     */
    @GET("projects/{project-name}/HEAD")
    Observable<String> getProjectHead(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-head"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/HEAD")
    Observable<String> setProjectHead(
            @NonNull @Path("project-name") String name,
            @NonNull @Body HeadInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-repository-statistics"
     */
    @GET("projects/{project-name}/statistics.git")
    Observable<RepositoryStatisticsInfo> getProjectStatistics(
            @NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-config"
     */
    @GET("projects/{project-name}/config")
    Observable<ConfigInfo> getProjectConfig(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-config"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/config")
    Observable<ConfigInfo> setProjectConfig(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ConfigInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#run-gc"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("projects/{project-name}/gc")
    Observable<Response> runProjectGc(
            @NonNull @Path("project-name") String name,
            @NonNull @Body GcInput input);
}
