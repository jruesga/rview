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

import android.annotation.SuppressLint;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class OkHttpHelper {

    @SuppressLint("TrustAllX509TrustManager")
    private static final X509TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(
                X509Certificate[] x509Certificates, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] x509Certificates, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    };

    private static SSLSocketFactory sSSLSocketFactory;

    public static OkHttpClient.Builder getSafeClientBuilder() {
        return new OkHttpClient.Builder();
    }

    @SuppressLint("BadHostnameVerifier")
    public static OkHttpClient.Builder getUnsafeClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            if (sSSLSocketFactory == null) {
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new X509TrustManager[]{TRUST_ALL_CERTS}, null);
                sSSLSocketFactory = sslContext.getSocketFactory();
            }

            builder.sslSocketFactory(sSSLSocketFactory, TRUST_ALL_CERTS);
            builder.hostnameVerifier((hostname, session) -> hostname != null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Ignore
        }
        return builder;
    }

}
