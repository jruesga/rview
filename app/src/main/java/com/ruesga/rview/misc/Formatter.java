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
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.gerrit.model.CommitInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.FileStatus;
import com.ruesga.rview.gerrit.model.GitPersonalInfo;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.text.QuotedSpan;
import com.ruesga.rview.widget.RegExLinkifyTextView;
import com.ruesga.rview.widget.RegExLinkifyTextView.RegExLink;
import com.ruesga.rview.widget.StyleableTextView;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

@ProguardIgnored
@SuppressWarnings("unused")
public class Formatter {
    private static final Map<Locale, PrettyTime> sPrettyTimeMap = new HashMap<>();
    private static String sDisplayFormat = Constants.ACCOUNT_DISPLAY_FORMAT_NAME;
    private static boolean sHighlightNotReviewed = true;

    private static int sQuoteColor = -1;
    private static int sQuoteWidth = -1;
    private static int sQuoteMargin = -1;

    public static void refreshCachedPreferences(Context context) {
        Account account = Preferences.getAccount(context);
        sDisplayFormat = Preferences.getAccountDisplayFormat(context, account);
        sHighlightNotReviewed = Preferences.isAccountHighlightUnreviewed(context, account);
    }

    @BindingAdapter("prettyDateTime")
    public static void toPrettyDateTime(TextView view, Date date) {
        if (date == null) {
            view.setText(null);
            return;
        }

        Locale locale = AndroidHelper.getCurrentLocale(view.getContext());
        if (!sPrettyTimeMap.containsKey(locale)) {
            sPrettyTimeMap.put(locale, new PrettyTime(locale));
        }
        view.setText(sPrettyTimeMap.get(locale).format(date));
    }

    @BindingAdapter("accountDisplayName")
    public static void toAccountDisplayName(TextView view, AccountInfo accountInfo) {
        if (accountInfo == null) {
            view.setText(null);
            return;
        }

        String accountDisplayName = null;
        switch (sDisplayFormat) {
            case Constants.ACCOUNT_DISPLAY_FORMAT_NAME:
                accountDisplayName = accountInfo.name;
                break;
            case Constants.ACCOUNT_DISPLAY_FORMAT_EMAIL:
                accountDisplayName = accountInfo.email;
                break;
            case Constants.ACCOUNT_DISPLAY_FORMAT_USERNAME:
                accountDisplayName = accountInfo.username;
                break;
        }
        if (TextUtils.isEmpty(accountDisplayName)) {
            accountDisplayName = accountInfo.username;
        }
        view.setText(accountDisplayName);
    }

    @BindingAdapter("highlightUnreviewed")
    public static void toHighlightedUnreviewed(StyleableTextView view, boolean reviewed) {
        view.setTypeface(TypefaceCache.getTypeface(view.getContext(),
                sHighlightNotReviewed && !reviewed
                        ? TypefaceCache.TF_BOLD_CONDENSED : TypefaceCache.TF_CONDENSED));
    }

    @BindingAdapter("compressedText")
    public static void toCompressedText(TextView view, String value) {
        if (value == null) {
            view.setText(null);
            return;
        }
        view.setText(value.replaceAll("\n", " ").trim());
    }

    @BindingAdapter("commitMessage")
    public static void toCommitMessage(TextView view, CommitInfo info) {
        if (info == null || info.message == null) {
            view.setText(null);
            return;
        }

        view.setText(StringHelper.removeLineBreaks(
                info.message.substring(info.subject.length()).trim()));
    }

    @BindingAdapter("userMessage")
    public static void toUserMessage(TextView view, String msg) {
        if (msg == null) {
            view.setText(null);
            return;
        }

        String preparedQuote = StringHelper.prepareForQuote(StringHelper.removeLineBreaks(msg));
        if (!preparedQuote.contains(StringHelper.NON_PRINTABLE_CHAR)) {
            // there is not quoted messages here, just a simple message
            view.setText(preparedQuote);
            return;
        }

        if (sQuoteColor == -1) {
            sQuoteColor = ContextCompat.getColor(view.getContext(), R.color.quote);
            sQuoteWidth = (int) view.getContext().getResources().getDimension(R.dimen.quote_width);
            sQuoteMargin = (int) view.getContext().getResources().getDimension(R.dimen.quote_margin);
        }

        String[] lines = preparedQuote.split("\n");
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(
                preparedQuote.replaceAll(StringHelper.NON_PRINTABLE_CHAR, ""));
        int start = 0;
        for (String line : lines) {
            int maxIndent = StringHelper.countOccurrences(StringHelper.NON_PRINTABLE_CHAR, line);
            for (int i = 0; i < maxIndent; i++) {
                QuotedSpan span = new QuotedSpan(
                        sQuoteColor, sQuoteWidth, sQuoteMargin, i, maxIndent);
                spannable.setSpan(span, start, start + line.length() - maxIndent,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            start += line.length() - maxIndent + 1;
        }

        view.setText(spannable);
    }

    @BindingAdapter("regexpLinkifyCommitsOnly")
    public static void toRegExLinkifyCommitsOnly(RegExLinkifyTextView view, Boolean only) {
        if (only) {
            view.addRegEx(
                    RegExLinkifyTextView.GERRIT_CHANGE_ID_REGEX,
                    RegExLinkifyTextView.GERRIT_COMMIT_REGEX);
        }
    }

    @BindingAdapter("regexpLinkify")
    public static void toRegExLinkify(RegExLinkifyTextView view, ConfigInfo info) {
        if (info == null || info.commentLinks == null || info.commentLinks.isEmpty()) {
            return;
        }

        List<RegExLink> linksScanners = new ArrayList<>();
        for (String key : info.commentLinks.keySet()) {
            switch (key) {
                case "changeid":
                    linksScanners.add(RegExLinkifyTextView.GERRIT_CHANGE_ID_REGEX);
                    break;
                case "commit":
                    linksScanners.add(RegExLinkifyTextView.GERRIT_COMMIT_REGEX);
                    break;
                default:
                    String link = info.commentLinks.get(key).link;
                    if (TextUtils.isEmpty(link) &&
                            !TextUtils.isEmpty(info.commentLinks.get(key).html)) {
                        Matcher matcher = RegExLinkifyTextView.WEB_LINK_REGEX.mPattern.matcher(
                                info.commentLinks.get(key).html);
                        if (matcher.find()) {
                            link = matcher.group();
                            linksScanners.add(new RegExLink(
                                    info.commentLinks.get(key).match, link));
                        }
                    }
                    break;
            }
        }
        view.addRegEx(linksScanners.toArray(new RegExLink[linksScanners.size()]));
    }

    @BindingAdapter("committer")
    public static void toCommitter(TextView view, GitPersonalInfo info) {
        if (info == null) {
            view.setText(null);
            return;
        }

        Locale locale = AndroidHelper.getCurrentLocale(view.getContext());
        if (!sPrettyTimeMap.containsKey(locale)) {
            sPrettyTimeMap.put(locale, new PrettyTime(locale));
        }
        String date = sPrettyTimeMap.get(locale).format(info.date);
        String txt = view.getContext().getString(
                R.string.committer_format, info.name, info.email, date);
        view.setText(txt);
    }

    @BindingAdapter("score")
    public static void toScore(TextView view, Integer score) {
        if (score == null) {
            view.setText(null);
            return;
        }

        String txt = (score > 0 ? "+" : "") + String.valueOf(score);
        view.setText(txt);
    }

    @BindingAdapter("submitType")
    public static void toSubmitType(TextView view, SubmitType submitType) {
        if (submitType == null) {
            view.setText(null);
            return;
        }

        // FIXME: Map to string resources
        view.setText(submitType.toString().replace("_", " "));
    }

    @BindingAdapter("fileStatus")
    public static void toFileStatus(TextView view, ChangeDetailsFragment.FileItemModel item) {
        if (item == null) {
            view.setText(null);
            return;
        }

        String status = "";
        if (item.info.status.equals(FileStatus.R)) {
            status = "[" + view.getContext().getString(R.string.file_status_renamed) + "] ";
        } else if (item.info.status.equals(FileStatus.C)) {
            status = "[" + view.getContext().getString(R.string.file_status_copied) + "] ";
        } else if (item.info.status.equals(FileStatus.W)) {
            status = "[" + view.getContext().getString(R.string.file_status_rewritten) + "] ";
        }
        String txt = status;
        if (!TextUtils.isEmpty(item.info.oldPath)) {
            txt += item.info.oldPath + " \u2192 ";
        }
        txt += item.file;
        view.setText(txt);
    }

    @BindingAdapter({"changeStatus", "currentRevision"})
    public static void toChangeStatus(TextView view, ChangeInfo change, boolean currentRevision) {
        if (change == null) {
            view.setText(null);
            return;
        }

        if (!currentRevision) {
            view.setText(R.string.change_statuses_not_current);
        } else if (change.status.equals(ChangeStatus.NEW)) {
            // TODO Convert labels to status (Needs Code-Review, Ready to Submit, ...)
            view.setText(R.string.menu_open);
        } else if (change.status.equals(ChangeStatus.MERGED)) {
            view.setText(R.string.menu_merged);
        } else if (change.status.equals(ChangeStatus.ABANDONED)) {
            view.setText(R.string.menu_abandoned);
        } else if (change.status.equals(ChangeStatus.DRAFT)) {
            view.setText(R.string.menu_draft);
        } else {
            view.setText(null);
        }
    }

    @BindingAdapter("addedVsDeleted")
    public static void toAddedVsRemoved(TextView view, FileInfo info) {
        if (info == null) {
            view.setText(null);
            return;
        }

        final Resources res = view.getResources();
        String added = null;
        if (info.linesInserted != null && info.linesInserted > 0) {
            added = res.getQuantityString(
                    R.plurals.files_added, info.linesInserted, info.linesInserted);
        }
        String deleted = null;
        if (info.linesDeleted != null && info.linesDeleted > 0) {
            deleted = res.getQuantityString(
                    R.plurals.files_deleted, info.linesDeleted, info.linesDeleted);
        }

        String txt = null;
        if (added != null && deleted != null) {
            txt = res.getString(R.string.added_vs_deleted, added, deleted);
        } else if (added != null) {
            txt = added;
        } else if (deleted != null) {
            txt = deleted;
        }
        view.setText(txt);
    }

    @BindingAdapter("commitWebLinksTag")
    public static void toCommitWebLinksTag(View view, CommitInfo commit) {
        if (commit != null && commit.webLinks != null && commit.webLinks.length > 0) {
            view.setTag(commit.webLinks[0].url);
        } else {
            view.setTag(null);
        }
    }

    @BindingAdapter("revisionNumber")
    public static void toRevisionNumber(TextView view, RevisionInfo revision) {
        if (revision == null) {
            view.setText(null);
            return;
        }
        view.setText(String.format(Locale.US, "#%02d", revision.number));
    }

    @BindingAdapter("revisionCommit")
    public static void toRevisionCommit(TextView view, RevisionInfo revision) {
        if (revision == null) {
            view.setText(null);
            return;
        }
        view.setText(revision.commit.commit.substring(0, 10));
    }

    @BindingAdapter("reviewerKind")
    public static void toReviewerKind(ImageView view, Boolean isGroup) {
        view.setImageResource(isGroup != null && isGroup
                ? R.drawable.ic_group : R.drawable.ic_person);
    }
}
