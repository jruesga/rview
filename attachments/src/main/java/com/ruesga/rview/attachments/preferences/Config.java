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
package com.ruesga.rview.attachments.preferences;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

public class Config {
    private static final X500Principal DEBUG_DN =
            new X500Principal("CN=Android Debug,O=Android,C=US");

    @TargetApi(Build.VERSION_CODES.P)
    @SuppressLint({"PackageManagerGetSignatures", "Deprecated"})
    public static boolean isApkDebugSigned(Context ctx) {
        try {
            final Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                        ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                        ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (Signature signature : signatures) {
                ByteArrayInputStream stream =
                        new ByteArrayInputStream(signature.toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                boolean debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
                if (debuggable) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
}
