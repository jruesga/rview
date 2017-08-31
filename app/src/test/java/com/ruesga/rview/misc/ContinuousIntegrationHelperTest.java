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
package com.ruesga.rview.misc;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.model.ContinuousIntegrationInfo;
import com.ruesga.rview.model.Repository;

import org.junit.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ContinuousIntegrationHelperTest {

    private static final int DEFAULT_PATCHSET_NUMBER = 1;

    @Test
    public void testExtractContinuousIntegrationInfo() {
        List<ContinuousIntegrationInfo> cis;

        // AOSP
        cis = extractContinuousIntegrationInfo("AOSP", "https://android-review.googlesource.com/",
                "^(Deckard Autoverifier|Treehugger Robot)$");
        assertEquals(7, cis.size());
        assertEquals("aosp_arm64-eng", cis.get(0).mName);
        assertEquals("https://android-build.googleplex.com/builds/pending/P4720458/" +
                        "aosp_arm64-eng/latest", cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // OpenStack
        cis = extractContinuousIntegrationInfo("OpenStack", "https://review.openstack.org/",
                "^(.* CI|Jenkins|Zuul|Elastic Recheck)$");
        assertEquals(36, cis.size());
        assertEquals("dsvm-tempest-zadara-driver", cis.get(1).mName);
        assertEquals("http://openstack-ci-logs.zadarastorage.com/85/499285/1/check/dsvm-tempest-zadara-driver/a1691cd",
                cis.get(1).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(1).mStatus);

        // Chromium
        cis = extractContinuousIntegrationInfo("Chromium",
                "https://chromium-review.googlesource.com/", "^(.* Bot)$");
        assertEquals(0, cis.size());

        // Gerrit
        cis = extractContinuousIntegrationInfo("Gerrit",
                "https://gerrit-review.googlesource.com/", "^(GerritForge CI|Diffy Cuckoo.*)$");
        assertEquals(3, cis.size());
        assertEquals("bazel/notedb", cis.get(0).mName);
        assertEquals("https://gerrit-ci.gerritforge.com/job/Gerrit-verifier-bazel/28933/console",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(0).mStatus);

        // Linaro
        cis = extractContinuousIntegrationInfo("Linaro",
                "https://android-review.linaro.org/", "^(lava-bot)$");
        assertEquals(4, cis.size());
        assertEquals("android-lcr-reference-hikey-o", cis.get(0).mName);
        assertEquals("https://ci.linaro.org/job/android-lcr-reference-hikey-o/8/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // LineageOs
        cis = extractContinuousIntegrationInfo("LineageOS",
                "https://review.lineageos.org/", "^(Jenkins)$");
        assertEquals(2, cis.size());
        assertEquals("wiki", cis.get(0).mName);
        assertEquals("https://jenkins.lineageos.org/job/infra/job/wiki/246/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.FAILURE, cis.get(0).mStatus);

        // Intel HPDD
        cis = extractContinuousIntegrationInfo("Intel HPDD",
                "https://review.whamcloud.com/", "^(Jenkins|HPDD Checkpatch|Misc Code Checks Robot.*|.* Buildbot|Autotest|Maloo)$");
        assertEquals(5, cis.size());
        assertEquals("CentOS 6.7 x86_64 (BUILD)", cis.get(0).mName);
        assertEquals("http://build.lustre.org/builders/CentOS%206.7%20x86_64%20%28BUILD%29/builds/10790",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // ONAP
        cis = extractContinuousIntegrationInfo("Onap",
                "https://gerrit.onap.org/r/", "^(ONAP Jobbuilder)$");
        assertEquals(2, cis.size());
        assertEquals("vfc-nfvo-catalog-master-catalog-verify-python", cis.get(0).mName);
        assertEquals("https://jenkins.onap.org/job/vfc-nfvo-catalog-master-catalog-verify-python/21/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);

        // Zephyr
        cis = extractContinuousIntegrationInfo("Zephyr",
                "https://gerrit.zephyrproject.org/r/", "^(Zephyr JobBuilder)$");
        assertEquals(1, cis.size());
        assertEquals("zephyr-verify", cis.get(0).mName);
        assertEquals("https://jenkins.zephyrproject.org/job/zephyr-verify/27024/",
                cis.get(0).mUrl);
        assertEquals(ContinuousIntegrationInfo.BuildStatus.SUCCESS, cis.get(0).mStatus);
    }

    private List<ContinuousIntegrationInfo> extractContinuousIntegrationInfo(
            final String id, final String url, final String ciAccounts) {
        final Repository repository = new Repository(id, url, false);
        repository.mCiAccounts = ciAccounts;

        Type type = new TypeToken<List<ChangeMessageInfo>>(){}.getType();
        List<ChangeMessageInfo> messages = SerializationManager.getInstance().fromJson(
                new InputStreamReader(
                        ContinuousIntegrationHelperTest.class.getResourceAsStream(
                                "/com/ruesga/rview/misc/ci-" + id.toLowerCase() + ".txt")), type);

        List<ContinuousIntegrationInfo> cis =
                ContinuousIntegrationHelper.extractContinuousIntegrationInfo(
                        DEFAULT_PATCHSET_NUMBER, messages.toArray(
                                new ChangeMessageInfo[messages.size()]), repository);
        Collections.sort(cis);
        return cis;
    }
}
