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
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.gerrit.model.SubmitStatus;
import com.ruesga.rview.gerrit.model.UseStatus;
import com.ruesga.rview.gerrit.model.UsernameInput;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GerritApiClientTest {

    //private static final String ENDPOINT = "https://gerrit-review.googlesource.com/";
    private static final String ENDPOINT = "http://review.cyanogenmod.org/";
    private static final String TEST_ENDPOINT = "http://localhost/";

    // ===============================
    // Gerrit access endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html"
    // ===============================

    @Test
    public void testGetAccessRights() {
        String[] projectNames = {"gerrit", "git-repo"};
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Map<String, ProjectAccessInfo> accesses =
                client.getAccessRights(projectNames).toBlocking().first();
        assertNotNull(accesses);
        assertEquals(accesses.size(), projectNames.length);
        for (String projectName : projectNames) {
            assertTrue(accesses.containsKey(projectName));
            assertNotNull(accesses.get(projectName));
            assertNotNull(accesses.get(projectName).inheritsFrom.id = "Public-Projects");
        }
    }

    // ===============================
    // Gerrit accounts endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html"
    // ===============================

    @Test
    public void testGetAccountsSuggestions() {
        String query = "john";
        final int count = 3;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<AccountInfo> accounts = client.getAccountsSuggestions(
                query, count).toBlocking().first();
        assertNotNull(accounts);
        assertEquals(accounts.size(), count);
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
    public void testGetAccount() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        AccountInfo account = client.getAccount(accountId).toBlocking().first();
        assertNotNull(account);
        assertEquals(account.accountId, accountId);
    }

    @Test
    public void testGetSelfAccount() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        AccountInfo account = client.getSelfAccount().toBlocking().first();
        assertNotNull(account);
    }

    @Test
    public void testCreateAccount() {
        final String username = "test";
        AccountInput input = new AccountInput();
        input.name = "Test";
        input.email = "test@test.com";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        AccountInfo account = client.createAccount(username, input).toBlocking().first();
        assertNotNull(account);
        assertEquals(account.name, username);
    }

    @Test
    public void testGetAccountDetails() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        AccountDetailInfo accountDetails = client.getAccountDetails(accountId).toBlocking().first();
        assertNotNull(accountDetails);
        assertEquals(accountDetails.accountId, accountId);
    }

    @Test
    public void testGetSelfAccountDetails() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        AccountDetailInfo accountDetails = client.getSelfAccountDetails().toBlocking().first();
        assertNotNull(accountDetails);
    }

    @Test
    public void testGetAccountName() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String name = client.getAccountName(accountId).toBlocking().first();
        assertNotNull(name);
    }

    @Test
    public void testGetSelfAccountName() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String name = client.getSelfAccountName().toBlocking().first();
        assertNotNull(name);
    }

    @Test
    public void testSetAccountName() {
        final AccountNameInput input = new AccountNameInput();
        input.name = "Test";
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String name = client.setAccountName(accountId, input).toBlocking().first();
        assertNotNull(name);
        assertEquals(name, input.name);
    }

    @Test
    public void testSetSelfAccountName() {
        final AccountNameInput input = new AccountNameInput();
        input.name = "Test";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String name = client.setSelfAccountName(input).toBlocking().first();
        assertNotNull(name);
        assertEquals(name, input.name);
    }

    @Test
    public void testDeleteAccountName() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteAccountName(accountId).toBlocking().first();
    }

    @Test
    public void testDeleteSelfAccountName() {
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteSelfAccountName().toBlocking().first();
    }

    @Test
    public void testGetAccountUsername() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String username = client.getAccountUsername(accountId).toBlocking().first();
        assertNotNull(username);
    }

    @Test
    public void testGetSelfAccountUsername() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String username = client.getSelfAccountUsername().toBlocking().first();
        assertNotNull(username);
    }

    @Test
    public void testSetAccountUsername() {
        final UsernameInput input = new UsernameInput();
        input.username = "test";
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String username = client.setAccountUsername(accountId, input).toBlocking().first();
        assertNotNull(username);
        assertEquals(username, input.username);
    }

    @Test
    public void testSetSelfAccountUsername() {
        final UsernameInput input = new UsernameInput();
        input.username = "test";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String username = client.setSelfAccountUsername(input).toBlocking().first();
        assertNotNull(username);
        assertEquals(username, input.username);
    }

    @Test
    public void testIsAccountActive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String active = client.isAccountActive(accountId).toBlocking().first();
        assertNotNull(active);
        assertEquals(active, "ok");
    }

    @Test
    public void testSetAccountAsActive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        client.setAccountAsActive(accountId).toBlocking().first();
        String active = client.isAccountActive(accountId).toBlocking().first();
        assertNotNull(active);
        assertEquals(active, "ok");
    }

    @Test
    public void testSetAccountAsInactive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        client.setAccountAsActive(accountId).toBlocking().first();
        String active = client.isAccountActive(accountId).toBlocking().first();
        assertTrue(active == null || active.length() == 0);
    }


    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

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


    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

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
        final String name = "test";
        final ProjectInput input = new ProjectInput();
        input.description = "test - description";
        input.createEmptyCommit = true;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ProjectInfo project = client.createProject(name, input).toBlocking().first();
        assertNotNull(project);
        assertNotNull(project.id);
        assertTrue(project.name.equals(name));
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

    @Test
    public void testGetProjectParent() {
        final String parent = "Public-Projects";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String projectParent = client.getProjectParent("git-repo").toBlocking().first();
        assertNotNull(projectParent);
        assertTrue(projectParent.equals(parent));
    }

    @Test
    public void testSetProjectParent() {
        final ProjectParentInput input = new ProjectParentInput();
        input.parent = "Public-Projects";
        input.commitMessage = "test commit message";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String projectParent = client.setProjectParent("test", input).toBlocking().first();
        assertNotNull(projectParent);
        assertTrue(projectParent.equals(input.parent));
    }

    @Test
    public void testGetProjectHead() {
        final String head = "refs/heads/master";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String projectHead = client.getProjectHead("git-repo").toBlocking().first();
        assertNotNull(projectHead);
        assertTrue(projectHead.equals(head));

    }

    @Test
    public void testSetProjectHead() {
        final HeadInput input = new HeadInput();
        input.ref = "refs/heads/master";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String projectHead = client.setProjectHead("test", input).toBlocking().first();
        assertNotNull(projectHead);
        assertTrue(projectHead.equals(input.ref));
    }

    @Test
    public void testGetProjectStatistics() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        RepositoryStatisticsInfo statistics =
                client.getProjectStatistics("git-repo").toBlocking().first();
        assertNotNull(statistics);
        assertTrue(statistics.numberOfLooseObjects > 0);
    }

    @Test
    public void testGetProjectConfig() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ConfigInfo config = client.getProjectConfig("git-repo").toBlocking().first();
        assertNotNull(config);
        assertNotNull(config.useContentMerge.value = true);
        assertNotNull(config.useSignedOffBy.value = false);
        assertNotNull(config.submitType.equals(SubmitStatus.MERGE_IF_NECESSARY));
    }

    @Test
    public void testSetProjectConfig() {
        final ConfigInput input = new ConfigInput();
        input.requireChangeId = UseStatus.TRUE;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ConfigInfo info = client.setProjectConfig("test", input).toBlocking().first();
        assertNotNull(info);
        assertNotNull(info.requireChangeId);
        assertTrue(info.requireChangeId.value = true);
    }

    @Test
    public void testRunProjectGc() {
        final GcInput input = new GcInput();
        input.async = true;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        Response response = client.runProjectGc("test", input).toBlocking().first();
        assertNotNull(response);
        String location = response.header("Location");
        assertNotNull(location);
    }
}
