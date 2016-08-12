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
package com.android.rview.gerrit;

import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritApiClient;
import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.StatusType;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ProjectDescriptionInput;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectInput;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GerritApiClientTest {

    private static final String ENDPOINT = "https://gerrit-review.googlesource.com/";
    private static final String TEST_ENDPOINT = "http://localhost/";

    @Test
    public void getAccountsSuggestions() {
        String query = "john";
        int count = 3;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<AccountInfo> accounts = client.getAccountsSuggestions(
                query, count).toBlocking().first();
        assertNotNull(accounts);
        assertEquals(accounts.size(), 3);
    }

    @Test
    public void testGetAccounts() {
        AccountQuery query = new AccountQuery();
        query.username("john");
        query.email("john@foo.com");
        final int count = 3;
        final int start = 1;
        final AccountOptions[] options = {AccountOptions.DETAILS, AccountOptions.ALL_EMAILS};
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<AccountInfo> accounts = client.getAccounts(
                query, count, start, options).toBlocking().first();
        assertNotNull(accounts);
        assertEquals(accounts.size(), count);
    }

    @Test
    public void getChanges() {
        ChangeQuery query = new ChangeQuery()
                .status(StatusType.MERGED)
                .and(new ChangeQuery().owner("Han-Wen Nienhuys"));
        final int count = 3;
        final int start = 0;
        final ChangeOptions[] options = {
                ChangeOptions.DETAILED_ACCOUNTS,
                ChangeOptions.DETAILED_LABELS};
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ChangeInfo> changes = client.getChanges(query, count, start, options).toBlocking().first();
        assertNotNull(changes);
        assertEquals(changes.size(), count);
    }

    @Test
    public void getChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final ChangeOptions[] options = {
                ChangeOptions.DETAILED_ACCOUNTS,
                ChangeOptions.DETAILED_LABELS};
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ChangeInfo change = client.getChange(changeId, options).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.id, changeId);
    }

    @Test
    public void testGetServerVersion() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ServerVersion version = client.getServerVersion().toBlocking().first();
        assertNotNull(version);
        assertTrue(version.getVersion() >= GerritApi.VERSION);
    }

    @Test
    public void testGetServerInfo() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ServerInfo info = client.getServerInfo().toBlocking().first();
        assertNotNull(info);
        assertTrue(info.gerrit.docUrl.equals(ENDPOINT + "Documentation/"));
    }

    @Test
    public void testGetProjects() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Map<String, ProjectInfo> projects = client.getProjects(true, true, null, null, null)
                .toBlocking().first();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);
    }

    @Test
    public void testGetProject() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ProjectInfo project = client.getProject("git-repo").toBlocking().first();
        assertNotNull(project);
        assertTrue(project.id.equals("git-repo"));
    }

    @Test
    public void testCreateProject() {
        final ProjectInput input = new ProjectInput();
        input.name = "test";
        input.description = "test - description";
        input.createEmptyCommit = true;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ProjectInfo project = client.createProject(input).toBlocking().first();
        assertNotNull(project);
        assertNotNull(project.id);
        assertTrue(project.name.equals(input.name));
    }

    @Test
    public void testGetProjectDescription() {
        final String description = "repo - The Multiple Git Repository Tool";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String projectDescription = client.getProjectDescription("git-repo").toBlocking().first();
        assertNotNull(projectDescription);
        assertTrue(projectDescription.equals(description));
    }

    @Test
    public void testSetProjectDescription() {
        final ProjectDescriptionInput input = new ProjectDescriptionInput();
        input.description = "test - description - " + System.currentTimeMillis();
        input.commitMessage = "test commit message";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String projectDescription = client.setProjectDescription("test", input).toBlocking().first();
        assertNotNull(projectDescription);
        assertTrue(projectDescription.equals(input.description));
    }

    @Test
    public void testDeleteProjectDescription() {
        final String project = "test";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteProjectDescription(project).toBlocking().first();
        String projectDescription = client.getProjectDescription(project).toBlocking().first();
        assertTrue(projectDescription == null || projectDescription.length() == 0);
    }
}
