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

import android.content.Context;
import android.text.TextUtils;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.Authorization;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AddReviewerResultInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.gerrit.model.LabelInfo;
import com.ruesga.rview.gerrit.model.ReviewerInfo;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelHelper {

    public static final String ACTION_CHERRY_PICK = "cherrypick";
    public static final String ACTION_REBASE = "rebase";
    public static final String ACTION_ABANDON = "abandon";
    public static final String ACTION_RESTORE = "restore";
    public static final String ACTION_REVERT = "revert";
    public static final String ACTION_PUBLISH_DRAFT = "publish";
    public static final String ACTION_FOLLOW_UP = "followup";
    public static final String ACTION_DELETE_CHANGE = "/";
    public static final String ACTION_SUBMIT = "submit";

    public static final String ACTION_CREATE_DRAFT = "create_draft";
    public static final String ACTION_UPDATE_DRAFT = "update_draft";
    public static final String ACTION_DELETE_DRAFT = "delete_draft";

    private static final Map<String, List<String>> sAvatarUrlCache = new HashMap<>();
    private static final Map<String, Boolean> sTemporaryTrustAllCertificates = new HashMap<>();

    public static GerritApi getGerritApi(Context context) {
        Account account = Preferences.getAccount(context);
        if (account == null) {
            return null;
        }
        boolean trustAllCerts = account.mRepository.mTrustAllCertificates;
        if (isTemporaryTrustAllCertificatesAccessGranted(account)) {
            trustAllCerts = true;
        }
        Authorization authorization = new Authorization(
                account.mAccount.username, account.mToken, trustAllCerts);
        return GerritServiceFactory.getInstance(
                context.getApplicationContext(), account.mRepository.mUrl, authorization);
    }

    public static boolean isTemporaryTrustAllCertificatesAccessGranted(Account account) {
        final String accountKey = account.getAccountHash();
        return sTemporaryTrustAllCertificates.containsKey(account.getAccountHash()) &&
                sTemporaryTrustAllCertificates.get(accountKey);
    }

    public static boolean hasTemporaryTrustAllCertificatesAccessRequested(Account account) {
        return sTemporaryTrustAllCertificates.containsKey(account.getAccountHash());
    }

    public static void setTemporaryTrustAllCertificatesAccessGrant(
            Account account, boolean granted) {
        sTemporaryTrustAllCertificates.put(account.getAccountHash(), granted);
    }

    public static List<String> getAvatarUrl(Context context, AccountInfo account) {
        String accountKey = account.accountId + "/" + getAccountDisplayName(account);

        List<String> urls = sAvatarUrlCache.get(accountKey);
        if (urls != null && !urls.isEmpty()) {
            return urls;
        }

        urls = new ArrayList<>();

        // Gerrit avatars
        int maxSize = (int) context.getResources().getDimension(R.dimen.max_avatar_size);
        if (account.avatars != null && account.avatars.length > 0) {
            GerritApi api =  ModelHelper.getGerritApi(context);
            if (api != null && api.supportsFeature(Features.AVATARS)) {
                String url = api.getAvatarUri(String.valueOf(account.accountId), maxSize).toString();
                urls.add(url);
            } else {
                int count = account.avatars.length - 1;
                boolean hasAvatarUrl = false;
                for (int i = count; i >= 0; i--) {
                    if (account.avatars[i].height < maxSize) {
                        urls.add(account.avatars[i].url);
                        hasAvatarUrl = true;
                        break;
                    }
                }
                if (hasAvatarUrl) {
                    urls.add(account.avatars[0].url);
                }
            }
        }

        // Github identicons
        if (account.username != null) {
            urls.add("https://github.com/identicons/" + account.username + ".png");
        }

        // Gravatar icons
        if (account.email != null) {
            urls.add("https://www.gravatar.com/avatar/"
                    + computeGravatarHash(account.email) + ".png?s=" + maxSize + "&d=404");
        }

        sAvatarUrlCache.put(accountKey, urls);
        return urls;
    }

    public static String getAccountDisplayName(AccountInfo account) {
        if (!TextUtils.isEmpty(account.name)) {
            return account.name;
        }
        if (!TextUtils.isEmpty(account.username)) {
            return account.username;
        }
        if (!TextUtils.isEmpty(account.email)) {
            return account.email;
        }
        return String.valueOf(account.accountId);
    }

    public static String getSafeAccountOwner(AccountInfo account) {
        return TextUtils.isEmpty(account.username)
                ? String.valueOf(account.accountId) : account.username;
    }

    public static String formatAccountWithEmail(AccountInfo account) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(account.name)) {
            sb.append(account.name);
        } else if (!TextUtils.isEmpty(account.username)) {
            sb.append(account.username);
        }
        if (!TextUtils.isEmpty(account.email)) {
            if (sb.length() == 0) {
                sb.append(account.email);
            } else {
                sb.append(" <").append(account.email).append(">");
            }
        }
        return sb.toString().trim();
    }

    public static AccountInfo[] removeAccount(AccountInfo account, AccountInfo[] accounts) {
        if (accounts == null) {
            return null;
        }
        List<AccountInfo> newAccounts = new ArrayList<>();
        for (AccountInfo a : accounts) {
            if (a.accountId != account.accountId) {
                newAccounts.add(a);
            }
        }
        return newAccounts.toArray(new AccountInfo[newAccounts.size()]);
    }

    public static ApprovalInfo[] removeApproval(AccountInfo account, ApprovalInfo[] approvals) {
        if (approvals == null) {
            return null;
        }
        List<ApprovalInfo> newApprovals = new ArrayList<>();
        for (ApprovalInfo a : approvals) {
            if (a.owner != null && a.owner.accountId != account.accountId) {
                newApprovals.add(a);
            }
        }
        return newApprovals.toArray(new ApprovalInfo[newApprovals.size()]);
    }

    public static AccountInfo[] addReviewers(ReviewerInfo[] reviewers, AccountInfo[] accounts) {
        List<AccountInfo> newAccounts = new ArrayList<>();
        if (accounts != null) {
            Collections.addAll(newAccounts, accounts);
        }
        for (ReviewerInfo reviewer : reviewers) {
            boolean exists = false;
            for (AccountInfo a : newAccounts) {
                if (a.accountId == reviewer.accountId) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                newAccounts.add(reviewer);
            }
        }
        return newAccounts.toArray(new AccountInfo[newAccounts.size()]);
    }

    public static ApprovalInfo[] updateApprovals(
            ReviewerInfo[] reviewers, String label, ApprovalInfo[] approvals) {
        List<ApprovalInfo> newApprovals = new ArrayList<>();
        if (approvals != null) {
            Collections.addAll(newApprovals, approvals);
        }
        for (ReviewerInfo reviewer : reviewers) {
            boolean exists = false;
            for (ApprovalInfo a : newApprovals) {
                if (a.owner.accountId == reviewer.accountId) {
                    if (reviewer.approvals != null) {
                        a.value = reviewer.approvals.get(label);
                    } else {
                        a.value = null;
                    }
                    a.date = new Date();
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                ApprovalInfo approvalInfo = new ApprovalInfo();
                approvalInfo.owner = reviewer;
                approvalInfo.date = new Date();
                approvalInfo.value = reviewer.approvals.get(label);
                newApprovals.add(approvalInfo);
            }
        }
        return newApprovals.toArray(new ApprovalInfo[newApprovals.size()]);
    }

    @SuppressWarnings("ConstantConditions")
    public static void updateRemovableReviewers(
            Context context, ChangeInfo change, AddReviewerResultInfo result) {
        List<AccountInfo> newRemovableAccounts = new ArrayList<>();
        Collections.addAll(newRemovableAccounts, change.removableReviewers);

        Account account = Preferences.getAccount(context);
        for (ReviewerInfo reviewer : result.reviewers) {
            boolean removable = false;

            // Owner of the change
            if (change.owner.accountId == account.mAccount.accountId) {
                removable = true;
            }

            // Reviewer is me
            if (reviewer.accountId == account.mAccount.accountId) {
                removable = true;
            }

            if (removable) {
                newRemovableAccounts.add(reviewer);
            }
        }

        change.removableReviewers = newRemovableAccounts.toArray(
                new AccountInfo[newRemovableAccounts.size()]);
    }

    public static boolean isReviewer(AccountInfo account, ChangeInfo change) {
        if (change.reviewers != null) {
            for (ReviewerStatus status : change.reviewers.keySet()) {
                AccountInfo[] accounts = change.reviewers.get(status);
                if (accounts != null) {
                    for (AccountInfo a : accounts) {
                        if (a.accountId == account.accountId) {
                            return true;
                        }
                    }
                }
            }
        }

        if (change.labels != null) {
            for (String label : change.labels.keySet()) {
                LabelInfo labelInfo = change.labels.get(label);
                if (labelInfo != null && labelInfo.all != null) {
                    for (ApprovalInfo approval : labelInfo.all) {
                        if (approval.owner.accountId == account.accountId) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static String checkNeedsLabel(Map<String, LabelInfo> labels) {
        for (String label : sortLabels(labels)) {
            LabelInfo labelInfo = labels.get(label);

            if (labelInfo.optional || labelInfo.values == null) {
                continue;
            }

            int approvalValue = getApprovalValue(labelInfo);
            boolean hasApproval = false;
            if (labelInfo.all != null) {
                for (ApprovalInfo approval : labelInfo.all) {
                    if (approval.value != null && approval.value >= approvalValue) {
                        hasApproval = true;
                        break;
                    }
                }
            }
            if (!hasApproval) {
                return label;
            }
        }

        // Has all needed labels approved
        return null;
    }

    private static int getApprovalValue(LabelInfo labelInfo) {
        int max = 0;
        for (Integer value : labelInfo.values.keySet()) {
            max = Math.max(max, value);
        }
        return max;
    }

    public static List<String> sortLabels(Map<String, LabelInfo> changeLabels) {
        List<String> labels = new ArrayList<>();
        if (changeLabels != null) {
            labels.addAll(changeLabels.keySet());
        }
        Collections.sort(labels);
        return labels;
    }

    public static List<String> sortPermittedLabels(Map<String, Integer[]> permittedLabels) {
        List<String> labels = new ArrayList<>();
        if (permittedLabels != null) {
            labels.addAll(permittedLabels.keySet());
        }
        Collections.sort(labels);
        return labels;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    private static String computeGravatarHash(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(email.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            // Ignore
        } catch (UnsupportedEncodingException e) {
            // Ignore
        }
        return null;
    }

    private static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte v : array) {
            sb.append(Integer.toHexString((v & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
