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

import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.StatusType;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ProjectAccessInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;

import net.iharder.Base64;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GerritApiClientTest {

    private static final String ENDPOINT = "https://gerrit-review.googlesource.com/";

    private static final PlatformAbstractionLayer TEST_PLATFORM = new PlatformAbstractionLayer() {
        @Override
        public boolean isDebugBuild() {
            return true;
        }

        @Override
        public void log(String message) {
            System.out.println(message);
        }

        @Override
        public byte[] encodeBase64(byte[] data) {
            return Base64.encodeBytesToBytes(data);
        }

        @Override
        public byte[] decodeBase64(byte[] data) {
            try {
                return Base64.decode(data);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean hasConnectivity() {
            return true;
        }
    };

    private static GerritApiClient getGerritClient(String endPoint) {
        return new GerritApiClient(endPoint, null, TEST_PLATFORM);
    }

    @Test
    public void testClientConnectivity() {
        String[] projectNames = {"gerrit", "git-repo"};
        final GerritApiClient client = getGerritClient(ENDPOINT);
        Map<String, ProjectAccessInfo> accesses =
                client.getAccessRights(projectNames).blockingFirst();
        assertNotNull(accesses);
        assertEquals(accesses.size(), projectNames.length);
        for (String projectName : projectNames) {
            assertTrue(accesses.containsKey(projectName));
            assertNotNull(accesses.get(projectName));
            assertNotNull(accesses.get(projectName).inheritsFrom.id = "Public-Projects");
        }
    }

    @Test
    public void testCacheServerVersion() {
        final int count = 5;
        final GerritApiClient client = getGerritClient(ENDPOINT);
        ChangeQuery query = new ChangeQuery().status(StatusType.OPEN);
        List<ChangeInfo> changes = client.getChanges(query, count, 0, null).blockingFirst();
        assertNotNull(changes);
        assertEquals(changes.size(), count);

        // And version should also be cached
        assertNotNull(client.mServerVersion);
        assertTrue(client.mServerVersion.getVersion() >= GerritApi.API_VERSION);
    }

    @Test
    public void testListOpenChanges() {
        final int count = 5;
        final GerritApiClient client = getGerritClient(ENDPOINT);
        ChangeQuery query = new ChangeQuery().status(StatusType.OPEN);
        List<ChangeInfo> changes = client.getChanges(query, count, 0, null).blockingFirst();
        assertNotNull(changes);
        assertEquals(changes.size(), count);
    }

    @Test
    public void testGetServerVersion() {
        final GerritApiClient client = getGerritClient(ENDPOINT);
        ServerVersion version = client.getServerVersion().blockingFirst();
        assertNotNull(version);
        assertTrue(version.getVersion() >= GerritApi.API_VERSION);
    }

    @Test
    public void testGetServerInfo() {
        final GerritApiClient client = getGerritClient(ENDPOINT);
        ServerInfo info = client.getServerInfo().blockingFirst();
        assertNotNull(info);
        assertEquals(info.gerrit.docUrl, ENDPOINT + "Documentation/");
    }

}
