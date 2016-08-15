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
import com.ruesga.rview.gerrit.model.AbandonInput;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountInput;
import com.ruesga.rview.gerrit.model.AccountNameInput;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.AddGpgKeyInput;
import com.ruesga.rview.gerrit.model.AddReviewerResultInfo;
import com.ruesga.rview.gerrit.model.AddReviewerStatus;
import com.ruesga.rview.gerrit.model.Capability;
import com.ruesga.rview.gerrit.model.CapabilityInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeInput;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.ConfigInput;
import com.ruesga.rview.gerrit.model.ContributorAgreementInfo;
import com.ruesga.rview.gerrit.model.ContributorAgreementInput;
import com.ruesga.rview.gerrit.model.DateFormat;
import com.ruesga.rview.gerrit.model.DeleteGpgKeyInput;
import com.ruesga.rview.gerrit.model.DeleteProjectWatchInput;
import com.ruesga.rview.gerrit.model.DeleteVoteInput;
import com.ruesga.rview.gerrit.model.DiffPreferencesInfo;
import com.ruesga.rview.gerrit.model.DiffPreferencesInput;
import com.ruesga.rview.gerrit.model.EditPreferencesInfo;
import com.ruesga.rview.gerrit.model.EditPreferencesInput;
import com.ruesga.rview.gerrit.model.EmailInfo;
import com.ruesga.rview.gerrit.model.EmailInput;
import com.ruesga.rview.gerrit.model.FixInput;
import com.ruesga.rview.gerrit.model.GcInput;
import com.ruesga.rview.gerrit.model.GpgKeyInfo;
import com.ruesga.rview.gerrit.model.GroupInfo;
import com.ruesga.rview.gerrit.model.HeadInput;
import com.ruesga.rview.gerrit.model.HttpPasswordInput;
import com.ruesga.rview.gerrit.model.IncludeInInfo;
import com.ruesga.rview.gerrit.model.MoveInput;
import com.ruesga.rview.gerrit.model.OAuthTokenInfo;
import com.ruesga.rview.gerrit.model.PreferencesInfo;
import com.ruesga.rview.gerrit.model.PreferencesInput;
import com.ruesga.rview.gerrit.model.ProjectAccessInfo;
import com.ruesga.rview.gerrit.model.ProjectDescriptionInput;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectInput;
import com.ruesga.rview.gerrit.model.ProjectParentInput;
import com.ruesga.rview.gerrit.model.ProjectWatchInfo;
import com.ruesga.rview.gerrit.model.ProjectWatchInput;
import com.ruesga.rview.gerrit.model.RebaseInput;
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.RestoreInput;
import com.ruesga.rview.gerrit.model.RevertInput;
import com.ruesga.rview.gerrit.model.ReviewerInfo;
import com.ruesga.rview.gerrit.model.ReviewerInput;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.gerrit.model.SshKeyInfo;
import com.ruesga.rview.gerrit.model.StarInput;
import com.ruesga.rview.gerrit.model.SubmitInput;
import com.ruesga.rview.gerrit.model.SubmitStatus;
import com.ruesga.rview.gerrit.model.SubmittedTogetherInfo;
import com.ruesga.rview.gerrit.model.SubmittedTogetherOptions;
import com.ruesga.rview.gerrit.model.SuggestedReviewerInfo;
import com.ruesga.rview.gerrit.model.TopicInput;
import com.ruesga.rview.gerrit.model.UseStatus;
import com.ruesga.rview.gerrit.model.UsernameInput;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GerritApiClientTest {

    private static final String ENDPOINT = "https://gerrit-review.googlesource.com/";
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
        AccountInfo account = client.getAccount(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(account);
        assertEquals(account.accountId, accountId);
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
        AccountDetailInfo accountDetails =
                client.getAccountDetails(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(accountDetails);
        assertEquals(accountDetails.accountId, accountId);
    }

    @Test
    public void testGetAccountName() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String name = client.getAccountName(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(name);
    }

    @Test
    public void testSetAccountName() {
        final AccountNameInput input = new AccountNameInput();
        input.name = "Test";
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String name = client.setAccountName(String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(name);
        assertEquals(name, input.name);
    }

    @Test
    public void testDeleteAccountName() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteAccountName(String.valueOf(accountId)).toBlocking().first();
    }

    @Test
    public void testGetAccountUsername() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String username = client.getAccountUsername(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(username);
    }

    @Test
    public void testSetAccountUsername() {
        final UsernameInput input = new UsernameInput();
        input.username = "test";
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String username = client.setAccountUsername(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(username);
        assertEquals(username, input.username);
    }

    @Test
    public void testIsAccountActive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String active = client.isAccountActive(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(active);
        assertEquals(active, "ok");
    }

    @Test
    public void testSetAccountAsActive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.setAccountAsActive(String.valueOf(accountId)).toBlocking().first();
        String active = client.isAccountActive(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(active);
        assertEquals(active, "ok");
    }

    @Test
    public void testSetAccountAsInactive() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.setAccountAsActive(String.valueOf(accountId)).toBlocking().first();
        String active = client.isAccountActive(String.valueOf(accountId)).toBlocking().first();
        assertTrue(active == null || active.length() == 0);
    }

    @Test
    public void testGetHttpPassword() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String name = client.getHttpPassword(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(name);
    }

    @Test
    public void testSetHttpPassword() {
        final HttpPasswordInput input = new HttpPasswordInput();
        input.generate = true;
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String password = client.setHttpPassword(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(password);
    }

    @Test
    public void testDeleteHttpPassword() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteHttpPassword(String.valueOf(accountId)).toBlocking().first();
    }

    @Test
    public void testGetOAuthToken() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        OAuthTokenInfo token = client.getOAuthToken(String.valueOf(accountId)).toBlocking().first();
        assertNotNull(token);
        assertNotNull(token.accessToken);
    }

    @Test
    public void testGetAccountEmails() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<EmailInfo> emails = client.getAccountEmails(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(emails);
        assertTrue(emails.size() > 0);
    }

    @Test
    public void testGetAccountEmail() {
        final String email = "test@test.com";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        EmailInfo info = client.getAccountEmail(
                GerritApi.SELF_ACCOUNT, email).toBlocking().first();
        assertNotNull(info);
        assertNotNull(info.email);
        assertTrue(info.email.equals(email));
    }

    @Test
    public void testCreateAccountEmail() {
        final EmailInput input = new EmailInput();
        input.preferred = true;
        final String email = "test@test.com";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        EmailInfo info = client.createAccountEmail(
                GerritApi.SELF_ACCOUNT, email, input).toBlocking().first();
        assertNotNull(info);
        assertNotNull(info.email);
        assertTrue(info.email.equals(email));
        assertEquals(info.preferred, true);
    }

    @Test
    public void testDeleteAccountEmail() {
        final String email = "test@test.com";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteAccountEmail(GerritApi.SELF_ACCOUNT, email).toBlocking().first();
    }

    @Test
    public void testSetAccountPreferredEmail() {
        final String email = "test@test.com";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.setAccountPreferredEmail(GerritApi.SELF_ACCOUNT, email).toBlocking().first();
    }

    @Test
    public void testGetAccountSshKeys() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<SshKeyInfo> sshKeys = client.getAccountSshKeys(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(sshKeys);
        assertTrue(sshKeys.size() > 0);
    }

    @Test
    public void testGetAccountSshKey() {
        final int sshKey = 1;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        SshKeyInfo info = client.getAccountSshKey(
                GerritApi.SELF_ACCOUNT, sshKey).toBlocking().first();
        assertNotNull(info);
        assertTrue(info.id == sshKey);
    }

    @Test
    public void testAddAccountSshKey() {
        final String encodedKey = "AAAAB3NzaC1yc2EAAAABIwAAAQEA0TYImydZAw==";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        SshKeyInfo info = client.addAccountSshKey(
                GerritApi.SELF_ACCOUNT, encodedKey).toBlocking().first();
        assertNotNull(info);
        assertNotNull(info.encodedKey);
        assertEquals(info.encodedKey, encodedKey);
    }

    @Test
    public void testDeleteAccountSshKey() {
        final int sshKey = 1;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteAccountSshKey(GerritApi.SELF_ACCOUNT, sshKey).toBlocking().first();
    }

    @Test
    public void testGetAccountGpgKeys() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<GpgKeyInfo> gpgKeys = client.getAccountGpgKeys(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(gpgKeys);
        assertTrue(gpgKeys.size() > 0);
    }

    @Test
    public void testGetAccountGpgKey() {
        final String gpgKey = "AFC8A49B";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        GpgKeyInfo info = client.getAccountGpgKey(
                GerritApi.SELF_ACCOUNT, gpgKey).toBlocking().first();
        assertNotNull(info);
        assertNotNull(info.id);
        assertTrue(info.id.equals(gpgKey));
    }

    @Test
    public void testAddAccountGpgKeys() {
        final AddGpgKeyInput input = new AddGpgKeyInput();
        input.gpgKeys = new String[]{"-----BEGIN PGP PUBLIC KEY BLOCK-----nmQENBFXNcBCACv4paCiy.."};
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        Map<String, GpgKeyInfo> keys = client.addAccountGpgKeys(
                GerritApi.SELF_ACCOUNT, input).toBlocking().first();
        assertNotNull(keys);
        assertEquals(keys.size(), 1);
    }

    @Test
    public void testDeleteAccountGpgKeys() {
        final DeleteGpgKeyInput input = new DeleteGpgKeyInput();
        input.gpgKeyIds = new String[]{"AFC8A49B"};
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        Map<String, GpgKeyInfo> keys = client.deleteAccountGpgKeys(
                GerritApi.SELF_ACCOUNT, input).toBlocking().first();
        assertNotNull(keys);
        assertEquals(keys.size(), 1);
        assertTrue(keys.containsKey(input.gpgKeyIds[0]));
    }

    @Test
    public void testGetAccountCapabilities() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        CapabilityInfo capabilities =
                client.getAccountCapabilities(String.valueOf(accountId), null).toBlocking().first();
        assertNotNull(capabilities);
    }

    @Test
    public void testHasAccountCapability() {
        final int accountId = 1000;
        final Capability capability = Capability.createGroup;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String hasCapability = client.hasAccountCapability(
                String.valueOf(accountId), capability).toBlocking().first();
        assertNotNull(hasCapability);
        assertEquals(hasCapability, "ok");
    }

    @Test
    public void testGetAccountGroups() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<GroupInfo> groups = client.getAccountGroups(
                String.valueOf(accountId)).toBlocking().first();
        assertNotNull(groups);
        assertTrue(groups.size() > 0);
    }

    @Test
    public void testGetAccountAvatar() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Response response = client.getAccountAvatar(
                String.valueOf(accountId)).toBlocking().first();
        assertNotNull(response);
        String location = response.header("Location");
        assertNotNull(location);
    }

    @Test
    public void testGetAccountAvatarChangeUrl() {
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String url = client.getAccountAvatarChangeUrl(
                String.valueOf(accountId)).toBlocking().first();
        assertNotNull(url);
    }

    @Test
    public void testGetAccountPreferences() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        PreferencesInfo preferences = client.getAccountPreferences(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(preferences);
    }

    @Test
    public void testSetAccountPreferences() {
        final PreferencesInput input = new PreferencesInput();
        input.dateFormat = DateFormat.EURO;
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        PreferencesInfo preferences = client.setAccountPreferences(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(preferences);
        assertNotNull(preferences.dateFormat);
        assertEquals(preferences.dateFormat, input.dateFormat);
    }

    @Test
    public void testGetAccountDiffPreferences() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        DiffPreferencesInfo preferences = client.getAccountDiffPreferences(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(preferences);
    }

    @Test
    public void testSetAccountDiffPreferences() {
        final DiffPreferencesInput input = new DiffPreferencesInput();
        input.expandAllComments = true;
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        DiffPreferencesInfo preferences = client.setAccountDiffPreferences(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(preferences);
        assertTrue(preferences.expandAllComments == input.expandAllComments);
    }

    @Test
    public void testGetAccountEditPreferences() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        EditPreferencesInfo preferences = client.getAccountEditPreferences(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(preferences);
    }

    @Test
    public void testSetAccountEditPreferences() {
        final EditPreferencesInput input = new EditPreferencesInput();
        input.autoCloseBrackets = true;
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        EditPreferencesInfo preferences = client.setAccountEditPreferences(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(preferences);
        assertTrue(preferences.autoCloseBrackets == input.autoCloseBrackets);
    }

    @Test
    public void testGetAccountWatchedProjects() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ProjectWatchInfo> projects = client.getAccountWatchedProjects(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);
    }

    @Test
    public void testSetAccountWatchedProjects() {
        final ProjectWatchInput[] input = new ProjectWatchInput[1];
        input[0] = new ProjectWatchInput();
        input[0].project = "test";
        input[0].notifyAllComments = true;
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        List<ProjectWatchInfo> projects = client.addOrUpdateAccountWatchedProjects(
                String.valueOf(accountId), input).toBlocking().first();
        assertNotNull(projects);
        assertEquals(projects.size(), input.length);
        assertEquals(projects.get(0).notifyAllComments, true);
    }

    @Test
    public void testDeleteAccountWatchedProjects() {
        final DeleteProjectWatchInput[] input = new DeleteProjectWatchInput[1];
        input[0] = new DeleteProjectWatchInput();
        input[0].project = "test";
        final int accountId = 1000;
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteAccountWatchedProjects(String.valueOf(accountId), input).toBlocking().first();
    }

    @Test
    public void testGetDefaultStarredChanges() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ChangeInfo> projects = client.getDefaultStarredChanges(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);
    }

    @Test
    public void testPutDefaultStarOnChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.putDefaultStarOnChange(GerritApi.SELF_ACCOUNT, changeId).toBlocking().first();
    }

    @Test
    public void testRemoveDefaultStarFromChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.removeDefaultStarFromChange(GerritApi.SELF_ACCOUNT, changeId).toBlocking().first();
    }

    @Test
    public void testGetStarredChanges() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ChangeInfo> projects = client.getStarredChanges(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);
    }

    @Test
    public void testGetStarLabelsFromChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<String> labels = client.getStarLabelsFromChange(
                GerritApi.SELF_ACCOUNT, changeId).toBlocking().first();
        assertNotNull(labels);
        assertTrue(labels.size() > 0);
    }

    @Test
    public void testUpdateStarLabelsFromChange() {
        final StarInput input = new StarInput();
        input.add = new String[]{"blue", "red"};
        input.remove = new String[]{"yellow"};
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        List<String> labels = client.updateStarLabelsFromChange(
                GerritApi.SELF_ACCOUNT, changeId, input).toBlocking().first();
        assertNotNull(labels);
        assertTrue(labels.size() > 0);
        assertTrue(labels.contains("blue"));
        assertTrue(labels.contains("red"));
        assertFalse(labels.contains("yellow"));
    }

    @Test
    public void testGetContributorAgreements() {
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ContributorAgreementInfo> agreements = client.getContributorAgreements(
                GerritApi.SELF_ACCOUNT).toBlocking().first();
        assertNotNull(agreements);
        assertTrue(agreements.size() > 0);
    }

    @Test
    public void testSignContributorAgreement() {
        final ContributorAgreementInput input = new ContributorAgreementInput();
        input.name = "Individual";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String agreement = client.signContributorAgreement(
                GerritApi.SELF_ACCOUNT, input).toBlocking().first();
        assertNotNull(agreement);
        assertEquals(agreement, input.name);
    }



    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

    @Test
    public void testCreateChange() {
        final ChangeInput input = new ChangeInput();
        input.project = "test";
        input.branch = "master";
        input.subject = "Test commit";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.createChange(input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.project, input.project);
        assertEquals(change.branch, input.branch);
        assertEquals(change.subject, input.subject);
    }

    @Test
    public void testGetChanges() {
        ChangeQuery query = new ChangeQuery()
                .status(StatusType.MERGED)
                .and(new ChangeQuery().owner("John Doe"));
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
    public void testGetChange() {
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
    public void testGetChangeDetail() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        ChangeInfo change = client.getChangeDetail(changeId, null).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.id, changeId);
    }

    @Test
    public void testGetChangeTopic() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        String topic = client.getChangeTopic(changeId).toBlocking().first();
        assertNotNull(topic);
    }

    @Test
    public void testSetChangeTopic() {
        final TopicInput input = new TopicInput();
        input.topic = "mytopic";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        String topic = client.setChangeTopic(changeId, input).toBlocking().first();
        assertNotNull(topic);
        assertEquals(topic, input.topic);
    }

    @Test
    public void testDeleteChangeTopic() {
        final TopicInput input = new TopicInput();
        input.topic = "mytopic";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteChangeTopic(changeId).toBlocking().first();
    }

    @Test
    public void testAbandonChange() {
        final AbandonInput input = new AbandonInput();
        input.message = "not useful anymore";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.abandonChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.status, ChangeStatus.ABANDONED);
    }

    @Test
    public void testRestoreChange() {
        final RestoreInput input = new RestoreInput();
        input.message = "restored";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.restoreChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.status, ChangeStatus.NEW);
    }

    @Test
    public void testRebaseChange() {
        final RebaseInput input = new RebaseInput();
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.rebaseChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.status, ChangeStatus.NEW);
    }

    @Test
    public void testMoveChange() {
        final MoveInput input = new MoveInput();
        input.destinationBranch = "release-2.0";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.moveChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.branch, input.destinationBranch);
    }

    @Test
    public void testRevertChange() {
        final RevertInput input = new RevertInput();
        input.message = "needed by prev commit";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.revertChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.status, ChangeStatus.NEW);
    }

    @Test
    public void testSubmitChange() {
        final SubmitInput input = new SubmitInput();
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.submitChange(changeId, input).toBlocking().first();
        assertNotNull(change);
        assertEquals(change.status, ChangeStatus.MERGED);
    }

    @Test
    public void testGetChangesSubmittedTogether() {
        final SubmittedTogetherOptions[] options = {SubmittedTogetherOptions.NON_VISIBLE_CHANGES};
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        SubmittedTogetherInfo changes = client.getChangesSubmittedTogether(
                changeId, options).toBlocking().first();
        assertNotNull(changes);
    }

    @Test
    public void testPublishDraftChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.publishDraftChange(changeId).toBlocking().first();
    }

    @Test
    public void testDeleteDraftChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteDraftChange(changeId).toBlocking().first();
    }

    @Test
    public void testGetChangeIncludedIn() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        IncludeInInfo includedIn = client.getChangeIncludedIn(changeId).toBlocking().first();
        assertNotNull(includedIn);
    }

    @Test
    public void testIndexChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.indexChange(changeId).toBlocking().first();
    }

    @Test
    public void testGetChangeComments() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Map<String, List<CommentInfo>> comments =
                client.getChangeComments(changeId).toBlocking().first();
        assertNotNull(comments);
        assertTrue(comments.size() > 0);
    }

    @Test
    public void testGetChangeDraftComments() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Map<String, List<CommentInfo>> comments =
                client.getChangeDraftComments(changeId).toBlocking().first();
        assertNotNull(comments);
        assertTrue(comments.size() > 0);
    }

    @Test
    public void testCheckChange() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.checkChange(changeId).toBlocking().first();
        assertNotNull(change);
    }

    @Test
    public void testFixChange() {
        final FixInput input = new FixInput();
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        ChangeInfo change = client.fixChange(changeId, input).toBlocking().first();
        assertNotNull(change);
    }




    @Test
    public void testGetChangeReviewers() {
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ReviewerInfo> reviewers = client.getChangeReviewers(changeId).toBlocking().first();
        assertNotNull(reviewers);
        assertTrue(reviewers.size() > 0);
    }

    @Test
    public void testGetChangeSuggestedReviewers() {
        final String query = "han";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<SuggestedReviewerInfo> reviewers =
                client.getChangeSuggestedReviewers(changeId, query, null).toBlocking().first();
        assertNotNull(reviewers);
        assertTrue(reviewers.size() > 0);
    }

    @Test
    public void testGetChangeReviewer() {
        final String accountId = "1024147";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        List<ReviewerInfo> reviewers = client.getChangeReviewer(
                changeId, accountId).toBlocking().first();
        assertNotNull(reviewers);
        assertNotNull(reviewers.size() == 1);
        assertEquals(String.valueOf(reviewers.get(0).accountId), accountId);
    }

    @Test
    public void testAddChangeReviewer() {
        final ReviewerInput input = new ReviewerInput();
        input.reviewerId = "1024147";
        input.state = AddReviewerStatus.REVIEWER;
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        AddReviewerResultInfo result = client.addChangeReviewer(
                changeId, input).toBlocking().first();
        assertNotNull(result);
        assertNull(result.error);
    }

    @Test
    public void testDeleteChangeReviewer() {
        final String accountId = "1024147";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteChangeReviewer(changeId, accountId).toBlocking().first();
    }

    @Test
    public void testGetChangeReviewerVotes() {
        final String accountId = "1024147";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(ENDPOINT);
        Map<String, Integer> votes = client.getChangeReviewerVotes(
                changeId, accountId).toBlocking().first();
        assertNotNull(votes);
        assertNotNull(votes.size() == 1);
    }

    @Test
    public void testDeleteChangeReviewerVote() {
        final DeleteVoteInput input = new DeleteVoteInput();
        final String accountId = "1024147";
        final String labelId = "Code-Review";
        final String changeId = "zoekt~master~Ia8e07dc3e73501d07c7726a6946b4088dad7e376";
        final GerritApiClient client = GerritApiClient.getInstance(TEST_ENDPOINT);
        client.deleteChangeReviewerVote(changeId, accountId, labelId, input).toBlocking().first();
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
