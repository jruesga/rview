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

import com.google.gson.GsonBuilder;
import com.ruesga.rview.gerrit.adapters.GerritApprovalInfoAdapter;
import com.ruesga.rview.gerrit.adapters.GerritServerVersionAdapter;
import com.ruesga.rview.gerrit.adapters.GerritUtcDateAdapter;
import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.ConfigInput;
import com.ruesga.rview.gerrit.model.GcInput;
import com.ruesga.rview.gerrit.model.HeadInput;
import com.ruesga.rview.gerrit.model.ProjectDescriptionInput;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectInput;
import com.ruesga.rview.gerrit.model.ProjectParentInput;
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

public class GerritApiClient implements GerritApi {

    private final static Map<String, GerritApiClient> sInstances = new HashMap<>();

    private final GerritApi mService;

    private GerritApiClient(String endpoint) {
        // OkHttp client
        OkHttpClient client = OkHttpHelper.getUnsafeClientBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createHeadersInterceptor())
                .build();

        // Gson adapter
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setVersion(VERSION)
                .generateNonExecutableJson()
                .setLenient();
        registerCustomAdapters(gsonBuilder);
        GsonConverterFactory gsonFactory = GsonConverterFactory.create(gsonBuilder.create());

        // RxJava adapter
        RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.create();

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(gsonFactory)
                .addCallAdapterFactory(rxAdapter)
                .build();

        // Build the api
        mService = retrofit.create(GerritApi.class);
    }

    public static GerritApiClient getInstance(String endpoint) {
        // Sanitize endpoint
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }

        if (!sInstances.containsKey(endpoint)) {
            sInstances.put(endpoint, new GerritApiClient(endpoint));
        }
        return sInstances.get(endpoint);
    }

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    private Interceptor createHeadersInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()/*
                        .header("Accept", "application/json")*/;
                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        };
    }

    private void registerCustomAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(Date.class, new GerritUtcDateAdapter());
        builder.registerTypeAdapter(ServerVersion.class, new GerritServerVersionAdapter());
        builder.registerTypeAdapter(ApprovalInfo.class, new GerritApprovalInfoAdapter());
    }


    //-- Accounts

    @Override
    public Observable<List<AccountInfo>> getAccountsSuggestions(
            @NonNull String query, @Nullable Integer count) {
        return mService.getAccountsSuggestions(query, count);
    }

    @Override
    public Observable<List<AccountInfo>> getAccounts(
            @NonNull AccountQuery query, @Nullable Integer count,
            @Nullable Integer start, @Nullable AccountOptions[] options) {
        return mService.getAccounts(query, count, start, options);
    }


    //-- Changes

    @Override
    public Observable<List<ChangeInfo>> getChanges(
            @NonNull ChangeQuery query, @Nullable Integer count,
            @Nullable Integer start, @Nullable ChangeOptions[] options) {
        return mService.getChanges(query, count, start, options);
    }

    @Override
    public Observable<ChangeInfo> getChange(
            @NonNull String changeId, @Nullable ChangeOptions[] options) {
        return mService.getChange(changeId, options);
    }


    //-- Config

    @Override
    public Observable<ServerVersion> getServerVersion() {
        return mService.getServerVersion();
    }

    @Override
    public Observable<ServerInfo> getServerInfo() {
        return mService.getServerInfo();
    }


    //-- Projects


    @Override
    public Observable<Map<String, ProjectInfo>> getProjects(@Nullable Boolean showDescription,
            @Nullable Boolean showTree, @Nullable String branch,
            @Nullable ProjectType type, @Nullable String group) {
        return mService.getProjects(showDescription, showTree, branch, type, group);
    }

    @Override
    public Observable<ProjectInfo> getProject(@NonNull String projectName) {
        return mService.getProject(projectName);
    }

    @Override
    public Observable<ProjectInfo> createProject(@NonNull ProjectInput input) {
        return mService.createProject(input);
    }

    @Override
    public Observable<String> getProjectDescription(@NonNull String projectName) {
        return mService.getProjectDescription(projectName);
    }

    @Override
    public Observable<String> setProjectDescription(
            @NonNull String projectName, @NonNull ProjectDescriptionInput input) {
        return mService.setProjectDescription(projectName, input);
    }

    @Override
    public Observable<Void> deleteProjectDescription(@NonNull String projectName) {
        return mService.deleteProjectDescription(projectName);
    }

    @Override
    public Observable<String> getProjectParent(@NonNull String projectName) {
        return mService.getProjectParent(projectName);
    }

    @Override
    public Observable<String> setProjectParent(
            @NonNull String projectName, @NonNull ProjectParentInput input) {
        return mService.setProjectParent(projectName, input);
    }

    @Override
    public Observable<String> getProjectHead(@NonNull String projectName) {
        return mService.getProjectHead(projectName);
    }

    @Override
    public Observable<String> setProjectHead(
            @NonNull String projectName, @NonNull HeadInput input) {
        return mService.setProjectHead(projectName, input);
    }

    @Override
    public Observable<RepositoryStatisticsInfo> getProjectStatistics(
            @NonNull String projectName) {
        return mService.getProjectStatistics(projectName);
    }

    @Override
    public Observable<ConfigInfo> getProjectConfig(@NonNull String projectName) {
        return mService.getProjectConfig(projectName);
    }

    @Override
    public Observable<ConfigInfo> setProjectConfig(
            @NonNull String projectName, @NonNull ConfigInput input) {
        return mService.setProjectConfig(projectName, input);
    }

    @Override
    public Observable<Response> runProjectGc(
            @NonNull String projectName, @NonNull GcInput input) {
        return mService.runProjectGc(projectName, input);
    }
}
