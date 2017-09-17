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
package com.ruesga.rview.misc;

import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.TestUtils;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.model.ContinuousIntegrationInfo;
import com.ruesga.rview.model.Repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({android.util.Log.class, android.text.TextUtils.class})
public class ContinuousIntegrationHelperTest {

    private static final int DEFAULT_PATCHSET_NUMBER = 1;

    private final List<Repository> repositories = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        TestUtils.mockCommonAndroidClasses();

        try {
            Type type = new TypeToken<ArrayList<Repository>>() {}.getType();
            repositories.addAll(TestUtils.loadJson(type, new File(TestUtils.getRootDirectory(),
                    "app/src/main/res/raw/repositories.json")));
        } catch (IOException ex) {
            throw new FileNotFoundException(new File("app/src/main/res/raw/repositories.json").getAbsolutePath());
        }
    }

    @Test
    public void testExtractContinuousIntegrationInfo() throws Exception {
        List<ContinuousIntegrationInfo> cis;

        // AOSP (1)
        cis = extractContinuousIntegrationInfo("AOSP", 1);
        assertEquals(7, cis.size());
        assertEquals("aosp-master/aosp_arm64-eng", cis.get(0).mName);
        assertEquals("https://android-build.googleplex.com/builds/pending/P4720458/aosp_arm64-eng/latest",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // AOSP (2)
        cis = extractContinuousIntegrationInfo("AOSP", 2);
        assertEquals(12, cis.size());
        assertEquals("git_master/marlin-userdebug", cis.get(6).mName);
        assertEquals("https://android-build.googleplex.com/builds/pending/P4783922/kernel_test/latest",
                cis.get(6).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(6).mStatus);

        // OpenStack
        cis = extractContinuousIntegrationInfo("OpenStack", null);
        assertEquals(36, cis.size());
        assertEquals("dsvm-tempest-zadara-driver", cis.get(1).mName);
        assertEquals("http://openstack-ci-logs.zadarastorage.com/85/499285/1/check/dsvm-tempest-zadara-driver/a1691cd",
                cis.get(1).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(1).mStatus);

        // Chromium
        cis = extractContinuousIntegrationInfo("Chromium", null);
        assertEquals(0, cis.size());

        // Gerrit
        cis = extractContinuousIntegrationInfo("Gerrit", null);
        assertEquals(3, cis.size());
        assertEquals("bazel/notedb", cis.get(0).mName);
        assertEquals("https://gerrit-ci.gerritforge.com/job/Gerrit-verifier-bazel/28933/console",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(0).mStatus);

        // Linaro
        cis = extractContinuousIntegrationInfo("Linaro", null);
        assertEquals(4, cis.size());
        assertEquals("android-lcr-reference-hikey-o", cis.get(0).mName);
        assertEquals("https://ci.linaro.org/job/android-lcr-reference-hikey-o/8/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // LineageOs
        cis = extractContinuousIntegrationInfo("LineageOS", null);
        assertEquals(2, cis.size());
        assertEquals("wiki", cis.get(0).mName);
        assertEquals("https://jenkins.lineageos.org/job/infra/job/wiki/246/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(0).mStatus);

        // Intel HPDD
        cis = extractContinuousIntegrationInfo("Intel HPDD", null);
        assertEquals(5, cis.size());
        assertEquals("CentOS 6.7 x86_64 (BUILD)", cis.get(0).mName);
        assertEquals("http://build.lustre.org/builders/CentOS%206.7%20x86_64%20%28BUILD%29/builds/10790",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // ONAP
        cis = extractContinuousIntegrationInfo("Onap", null);
        assertEquals(2, cis.size());
        assertEquals("vfc-nfvo-catalog-master-catalog-verify-python", cis.get(0).mName);
        assertEquals("https://jenkins.onap.org/job/vfc-nfvo-catalog-master-catalog-verify-python/21/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // Zephyr
        cis = extractContinuousIntegrationInfo("Zephyr", null);
        assertEquals(1, cis.size());
        assertEquals("zephyr-verify", cis.get(0).mName);
        assertEquals("https://jenkins.zephyrproject.org/job/zephyr-verify/27024/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);
    }

    private List<ContinuousIntegrationInfo> extractContinuousIntegrationInfo(
            final String id, @Nullable final Integer number) throws Exception {
        final Repository repository = findRepositoryById(id);

        Type type = new TypeToken<List<ChangeMessageInfo>>(){}.getType();
        String testName = id.toLowerCase();
        if (number != null) {
            testName += "-" + number;
        }
        List<ChangeMessageInfo> messages =
                TestUtils.loadJson(type, "/com/ruesga/rview/misc/ci-" + testName + ".txt");

        List<ContinuousIntegrationInfo> cis =
                ContinuousIntegrationHelper.extractContinuousIntegrationInfo(
                        DEFAULT_PATCHSET_NUMBER, messages.toArray(
                                new ChangeMessageInfo[messages.size()]), repository);
        Collections.sort(cis);
        return cis;
    }

    private Repository findRepositoryById(String id) throws IOException {
        for (Repository repository : repositories) {
            if (repository.mName.equals(id)) {
                return repository;
            }
        }
        throw new IOException("Repository not found");
    }
}