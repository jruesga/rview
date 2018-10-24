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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruesga.rview.R;
import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeKind;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.CommitInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.FileStatus;
import com.ruesga.rview.gerrit.model.GitPersonalInfo;
import com.ruesga.rview.gerrit.model.ProjectStatus;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.gerrit.model.ReviewerUpdateInfo;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SideType;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.ContinuousIntegrationInfo;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.model.UnresolvedComment;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.text.QuotedSpan;
import com.ruesga.rview.text.TagSpan;
import com.ruesga.rview.widget.RegExLinkifyTextView;
import com.ruesga.rview.widget.RegExLinkifyTextView.RegExLink;
import com.ruesga.rview.widget.StyleableTextView;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.databinding.BindingAdapter;

@Keep
@SuppressWarnings("unused")
public class Formatter {
    private static Account mAccount;
    private static final Map<String, PrettyTime> sPrettyTimeMap = new HashMap<>();
    private static String sDisplayFormat = Constants.ACCOUNT_DISPLAY_FORMAT_NAME;
    private static boolean sHighlightNotReviewed = true;
    private static boolean sDisplayAccountStatues = true;
    private static int sHighlightScoredMessages = Constants.HIGHLIGHT_SCORED_MESSAGE_MESSAGE;

    private static int sQuoteColor = -1;
    private static int sQuoteWidth = -1;
    private static int sQuoteMargin = -1;

    public static void refreshCachedPreferences(Context context) {
        mAccount = Preferences.getAccount(context);
        sDisplayFormat = Preferences.getAccountDisplayFormat(context, mAccount);
        sHighlightNotReviewed = Preferences.isAccountHighlightUnreviewed(context, mAccount);
        sDisplayAccountStatues = Preferences.isAccountDisplayStatuses(context, mAccount);
        sHighlightScoredMessages = Preferences.getAccountMessagesHighlightScored(context, mAccount);
    }

    private static PrettyTime getPrettyTime(Context context) {
        String localeTag = context.getString(R.string.bcp47_locale_tag);
        if (!sPrettyTimeMap.containsKey(localeTag)) {
            final String[] localeInfo = localeTag.split("-");
            final Locale locale = new Locale(localeInfo[0], localeInfo[1]);
            sPrettyTimeMap.put(localeTag, new PrettyTime(locale));
        }
        return sPrettyTimeMap.get(localeTag);
    }

    @BindingAdapter("prettyDateTime")
    public static void toPrettyDateTime(TextView view, Date date) {
        if (date == null) {
            view.setText(null);
            return;
        }
        view.setText(getPrettyTime(view.getContext()).format(date));
    }

    @SuppressWarnings("deprecation")
    @BindingAdapter("changeMessageAccount")
    public static void toChangeMessageAccount(TextView view, ChangeMessageInfo changeMessage) {
        if (changeMessage == null) {
            toAccountDisplayName(view, null);
            return;
        }

        if (ModelHelper.isOnBehalfOf(changeMessage)) {
            view.setText(Html.fromHtml(
                    view.getContext().getString(R.string.change_details_on_behalf_of,
                            toAccountDisplayName(changeMessage.realAuthor),
                            toAccountDisplayName(changeMessage.author))));
            view.setTypeface(TypefaceCache.getTypeface(view.getContext(), TypefaceCache.TF_REGULAR));
            return;
        }

        toAccountDisplayName(view, changeMessage.author);
    }

    @BindingAdapter("accountDisplayName")
    public static void toAccountDisplayName(TextView view, AccountInfo accountInfo) {
        if (accountInfo == null) {
            view.setText(null);
            return;
        }

        view.setText(toAccountDisplayName(accountInfo));
    }

    public static String toAccountDisplayName(AccountInfo accountInfo) {
        // Display Name
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
        if (TextUtils.isEmpty(accountDisplayName)) {
            accountDisplayName = accountInfo.name;
        }
        if (TextUtils.isEmpty(accountDisplayName)) {
            accountDisplayName = accountInfo.email;
        }
        if (TextUtils.isEmpty(accountDisplayName)) {
            accountDisplayName = String.valueOf(accountInfo.accountId);
        }

        // Status
        if (sDisplayAccountStatues && !TextUtils.isEmpty(accountInfo.status)) {
            accountDisplayName += " (" + accountInfo.status + ")";
        }

        return accountDisplayName;
    }

    @BindingAdapter({"draftAccountDisplayName", "isDraft"})
    public static void toDraftAccountDisplayName(
            TextView view, AccountInfo accountInfo, boolean isDraft) {

        if (isDraft) {
            Context ctx = view.getContext();
            view.setText(ctx.getString(R.string.menu_draft).toUpperCase(
                    AndroidHelper.getCurrentLocale(ctx)));
            Drawable dw = ContextCompat.getDrawable(ctx, R.drawable.bg_tag);
            if (dw != null) {
                dw = DrawableCompat.wrap(dw.mutate());
                DrawableCompat.setTint(dw, ContextCompat.getColor(ctx, R.color.unscored));
            }
            view.setBackground(dw);
            return;
        }

        view.setBackground(null);
        toAccountDisplayName(view, accountInfo);
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

        String message = EmojiHelper.createEmoji(
                info.message.substring(info.subject.length()).trim());
        view.setText(StringHelper.removeLineBreaks(message));
    }

    @SuppressWarnings("deprecation")
    @BindingAdapter("userFoldMessage")
    public static void userFoldMessage(TextView view, ChangeMessageInfo msg) {
        if (msg == null) {
            view.setText(null);
            return;
        }

        // Create reviewer updates message
        if (msg._reviewer_updates.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (ReviewerStatus status : ReviewerStatus.values()) {
                sb.append(toReviewUpdateMessage(
                        view.getContext(), msg._reviewer_updates, status, sb.length() > 0));
            }
            view.setText(Html.fromHtml(StringHelper.fold(sb.toString())));
            return;
        }

        // Fold the user message
        String userMessage = StringHelper.fold(StringHelper.removeAllAttachments(msg.message));
        if (userMessage == null) {
            view.setText(null);
            return;
        }
        final SpannableStringBuilder spannable = new SpannableStringBuilder(userMessage);
        highlightUserMessageReviewScores(
                view.getContext(), StringHelper.firstLine(msg.message), spannable);
        view.setText(spannable);
    }

    @BindingAdapter("userMessage")
    public static void toUserMessage(TextView view, String msg) {
        if (msg == null) {
            view.setText(null);
            return;
        }

        // Clean up paragraphs (to mimic "<p></p>" browsers behaviour) and create Emojis
        String message = EmojiHelper.createEmoji(StringHelper.cleanUpParagraphs(msg));

        // This process mimics the Gerrit formatting process done in class
        // ./gerrit-gwtexpui/src/main/java/com/google/gwtexpui/safehtml/client/SafeHtml.java

        // Split message into paragraphs
        String[] paragraphs = StringHelper.obtainParagraphs(message);
        StringBuilder sb = new StringBuilder();
        boolean formattedMessage = false;
        for (String p : paragraphs) {
            if (StringHelper.isQuote(p)) {
                sb.append(StringHelper.obtainQuote(StringHelper.prepareForQuote(p).trim()));
                formattedMessage = true;
            } else if (StringHelper.isList(p)) {
                sb.append(p);
                formattedMessage = true;
            } else if (StringHelper.isPreFormat(p)) {
                sb.append(StringHelper.obtainPreFormatMessage(p));
                formattedMessage = true;
            } else {
                sb.append(p);
            }
            if (sb.length() > 0) {
                while (sb.charAt(sb.length() - 1) == '\n') {
                    sb = sb.deleteCharAt(sb.length() - 1);
                }
            }
            sb.append("\n\n");
        }

        String userMessage = StringHelper.removeExtraLines(sb.toString());

        // Create the spannable
        final SpannableStringBuilder spannable;
        if (!formattedMessage) {
            spannable = new SpannableStringBuilder(userMessage);
        } else {
            spannable = new SpannableStringBuilder(
                    userMessage.replaceAll(StringHelper.NON_PRINTABLE_CHAR, "")
                            .replaceAll(StringHelper.NON_PRINTABLE_CHAR2, ""));
        }

        // Highlight user message's scores
        highlightUserMessageReviewScores(
                view.getContext(), StringHelper.firstLine(userMessage), spannable);

        // If the user message is not formatted, do not try to compute unnecessary stuff
        if (!formattedMessage) {
            view.setText(spannable);
            return;
        }

        if (sQuoteColor == -1) {
            sQuoteColor = ContextCompat.getColor(view.getContext(), R.color.quote);
            sQuoteWidth = (int) view.getContext().getResources().getDimension(R.dimen.quote_width);
            sQuoteMargin = (int) view.getContext().getResources().getDimension(R.dimen.quote_margin);
        }
        String[] lines = userMessage.split("\n");

        // Pre-Format
        int start = 0;
        int spans = 0;
        while ((start = userMessage.indexOf(StringHelper.NON_PRINTABLE_CHAR2, start)) != -1) {
            int end = userMessage.indexOf(StringHelper.NON_PRINTABLE_CHAR2, start + 1);
            if (end == -1) {
                //¿? This is supposed to be formatted by us. Skip it
                break;
            }

            // Find quote token occurrences
            int offset = StringHelper.countOccurrences(
                    StringHelper.NON_PRINTABLE_CHAR, userMessage, 0, start);
            start -= offset;
            end -= offset;

            // Ensure bounds
            int spanStart = start - spans;
            int spanEnd = Math.min(end - spans - 1, spannable.length());
            if (spanStart < 0 || spanEnd < 0) {
                //Something was wrong. Skip it
                break;
            }
            spannable.setSpan(new RelativeSizeSpan(0.8f), spanStart, spanEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new TypefaceSpan("monospace"), spanStart, spanEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end;
            spans++;
        }

        start = 0;
        for (String line : lines) {
            // Quotes
            int maxIndent = StringHelper.countOccurrences(StringHelper.NON_PRINTABLE_CHAR, line);
            for (int i = 0; i < maxIndent; i++) {
                QuotedSpan span = new QuotedSpan(sQuoteColor, sQuoteWidth, sQuoteMargin);
                spannable.setSpan(span, start,
                        Math.min(start + line.length() - maxIndent, spannable.length()),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // List
            if (StringHelper.isList(line)) {
                spannable.replace(start, start + 1, "\u2022");
                spannable.setSpan(new LeadingMarginSpan.Standard(sQuoteMargin),
                        start, Math.min(start + line.length(), spannable.length()),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            start += line.length() - maxIndent + 1;
        }

        view.setText(spannable);
    }

    @BindingAdapter("regexpLinkifyCommitsOnly")
    public static void toRegExLinkifyCommitsOnly(RegExLinkifyTextView view, Boolean only) {
        List<RegExLink> linksScanners = new ArrayList<>();

        if (mAccount != null) {
            linksScanners.addAll(
                    RegExLinkifyTextView.createRepositoryRegExpLinks(mAccount.mRepository));
        }

        if (only) {
            linksScanners.add(RegExLinkifyTextView.GERRIT_CHANGE_ID_REGEX);
            linksScanners.add(RegExLinkifyTextView.GERRIT_COMMIT_REGEX);
        }

        if (!linksScanners.isEmpty()) {
            view.addRegEx(linksScanners.toArray(new RegExLink[0]));
        }
    }

    @BindingAdapter("regexpLinkify")
    public static void toRegExLinkify(RegExLinkifyTextView view, ConfigInfo info) {
        List<RegExLink> linksScanners = new ArrayList<>();
        linksScanners.add(RegExLinkifyTextView.GERRIT_CHANGE_ID_REGEX);
        linksScanners.add(RegExLinkifyTextView.GERRIT_COMMIT_REGEX);
        if (mAccount != null) {
            linksScanners.addAll(
                    RegExLinkifyTextView.createRepositoryRegExpLinks(mAccount.mRepository));
        }

        if (info != null && info.commentLinks != null) {
            for (String key : info.commentLinks.keySet()) {
                switch (key) {
                    case "changeid":
                    case "commit":
                        break;
                    default:
                        String match = info.commentLinks.get(key).match;
                        String link = info.commentLinks.get(key).link;
                        String htmlLink = StringHelper.extractLinkFromHtml(
                                info.commentLinks.get(key).html);
                        if (TextUtils.isEmpty(match) && TextUtils.isEmpty(link)
                                && TextUtils.isEmpty(htmlLink)) {
                            continue;
                        }

                        // Use link if available, otherwise extract the link from the html
                        if (TextUtils.isEmpty(link)) {
                            link = htmlLink;
                        }

                        // Add the link scanner
                        linksScanners.add(new RegExLink(RegExLinkifyTextView.WEB_LINK_REGEX.mType,
                                match, link, true));
                        break;
                }
            }
        }

        view.addRegEx(linksScanners.toArray(new RegExLink[0]));
    }

    @BindingAdapter("committer")
    public static void toCommitter(TextView view, GitPersonalInfo info) {
        if (info == null) {
            view.setText(null);
            return;
        }

        String date = getPrettyTime(view.getContext()).format(info.date);
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

        if (submitType.equals(SubmitType.FAST_FORWARD_ONLY)) {
            view.setText(R.string.submit_type_fast_forward_only);
        } else if (submitType.equals(SubmitType.MERGE_IF_NECESSARY)) {
            view.setText(R.string.submit_type_merge_if_necessary);
        } else if (submitType.equals(SubmitType.MERGE_ALWAYS)) {
            view.setText(R.string.submit_type_merge_always);
        } else if (submitType.equals(SubmitType.CHERRY_PICK)) {
            view.setText(R.string.submit_type_cherry_pick);
        } else if (submitType.equals(SubmitType.REBASE_IF_NECESSARY)) {
            view.setText(R.string.submit_type_rebase_if_necessary);
        } else {
            view.setText(null);
        }
    }

    @BindingAdapter("fileTypeDrawable")
    public static void toFileTypeDrawable(ImageView view, FileInfo model) {
        if (model == null || model.status == null) {
            view.setImageDrawable(null);
            return;
        }

        view.setImageDrawable(ContextCompat.getDrawable(view.getContext(),
                ModelHelper.toFileStatusDrawable(model.status)));
    }

    @BindingAdapter("fileStatus")
    public static void toFileStatus(TextView view, ChangeDetailsFragment.FileItemModel item) {
        if (item == null) {
            view.setText(null);
            return;
        }

        String status = "";
        String txt = "";
        if (item.info != null) {
            if (item.info.status.equals(FileStatus.R)) {
                status = "[" + view.getContext().getString(R.string.file_status_renamed) + "] ";
            } else if (item.info.status.equals(FileStatus.C)) {
                status = "[" + view.getContext().getString(R.string.file_status_copied) + "] ";
            } else if (item.info.status.equals(FileStatus.W)) {
                status = "[" + view.getContext().getString(R.string.file_status_rewritten) + "] ";
            }
            txt = status;
            if (!TextUtils.isEmpty(item.info.oldPath)) {
                txt += item.info.oldPath + " \u2192 ";
            }
        }
        txt += item.file.startsWith("/") ? item.file.substring(1) : item.file;
        view.setText(txt);
    }

    @BindingAdapter("fileStatus")
    public static void toFileStatus(TextView view, FileStatus status) {
        if (status == null) {
            view.setText(null);
            return;
        }

        if (status.equals(FileStatus.R)) {
            view.setText(R.string.file_status_renamed);
        } else if (status.equals(FileStatus.C)) {
            view.setText(R.string.file_status_copied);
        } else if (status.equals(FileStatus.W)) {
            view.setText(R.string.file_status_rewritten);
        } else if (status.equals(FileStatus.A)) {
            view.setText(R.string.file_status_added);
        } else if (status.equals(FileStatus.D)) {
            view.setText(R.string.file_status_deleted);
        } else if (status.equals(FileStatus.M)) {
            view.setText(R.string.file_status_modified);
        }
    }

    @BindingAdapter("file")
    public static void toFile(TextView view, String path) {
        view.setText(path.startsWith("/") ? path.substring(1) : path);
    }

    @BindingAdapter("moreItems")
    public static void toMoreItems(TextView view, Integer more) {
        int q = more == null ? 0 : more;
        view.setText(view.getResources().getQuantityString(R.plurals.more_items, q, q));
    }

    @BindingAdapter({"changeStatus", "currentRevision"})
    public static void toChangeStatus(TextView view, ChangeInfo change, boolean currentRevision) {
        if (change == null) {
            view.setText(null);
            return;
        }

        if (!currentRevision) {
            view.setText(R.string.change_statuses_not_current);
        } else if (ChangeStatus.NEW.equals(change.status)) {
            if (change.submittable) {
                view.setText(R.string.change_statuses_ready_to_submit);
            } else if (!change.mergeable) {
                view.setText(R.string.change_statuses_not_mergeable);
            } else {
                String neededLabel = ModelHelper.checkNeedsLabel(change.labels);
                if (neededLabel != null) {
                    view.setText(view.getContext().getString(
                            R.string.change_statuses_needs_label, neededLabel));
                } else {
                    view.setText(R.string.menu_open);
                }
            }
        } else if (ChangeStatus.SUBMITTED.equals(change.status)) {
            view.setText(R.string.change_statuses_submitted);
        } else if (ChangeStatus.MERGED.equals(change.status)) {
            view.setText(R.string.menu_merged);
        } else if (ChangeStatus.ABANDONED.equals(change.status)) {
            view.setText(R.string.menu_abandoned);
        } else if (ChangeStatus.DRAFT.equals(change.status)) {
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
        if (added == null && deleted == null) {
            added = res.getQuantityString(R.plurals.files_added, 0, 0);
            deleted = res.getQuantityString(R.plurals.files_deleted, 0, 0);
        }

        String txt;
        if (added != null && deleted != null) {
            txt = res.getString(R.string.added_vs_deleted, added, deleted);
        } else if (added != null) {
            txt = added;
        } else {
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
        if (revision.commit == null) {
            view.setText(view.getContext().getString(R.string.options_base));
        } else {
            view.setText(revision.commit.commit.substring(0, 10));
        }
    }

    @BindingAdapter("reviewerKind")
    public static void toReviewerKind(ImageView view, Boolean isGroup) {
        view.setImageResource(isGroup != null && isGroup
                ? R.drawable.ic_group : R.drawable.ic_person);
    }

    @BindingAdapter("accountEmails")
    public static void toAccountEmails(TextView v, AccountDetailInfo account) {
        if (account == null || account.email == null) {
            v.setText(null);
            return;
        }

        StringBuilder emails = new StringBuilder(account.email);
        if (account.secondaryEmails != null) {
            for (String email : account.secondaryEmails) {
                emails.append("\n").append(email);
            }
        }
        v.setText(emails.toString());
    }

    @BindingAdapter("projectStatus")
    public static void toProjectStatus(TextView v, ProjectStatus status) {
        if (status == null) {
            v.setText(null);
            return;
        }

        String statusText = null;
        if (status.equals(ProjectStatus.ACTIVE)) {
            statusText = v.getContext().getString(R.string.project_details_status_active);
        } else if (status.equals(ProjectStatus.READ_ONLY)) {
            statusText = v.getContext().getString(R.string.project_details_status_read_only);
        } else if (status.equals(ProjectStatus.HIDDEN)) {
            statusText = v.getContext().getString(R.string.project_details_status_hidden);
        }
        v.setText(statusText);
    }

    @BindingAdapter("commentLine")
    public static void toCommentLine(TextView v, CommentInfo comment) {
        if (comment == null) {
            v.setText(null);
            return;
        }

        String link = "";
        if (comment.patchSet != comment.messagePatchSet) {
            link = v.getContext().getString(
                    R.string.change_details_comment_patchset, comment.patchSet);
        }
        if (comment.line == null) {
            link += v.getContext().getString(R.string.change_details_comment_file);
        } else if (SideType.PARENT.equals(comment.side)) {
            link += v.getContext().getString(
                    R.string.change_details_comment_base_line_number, comment.line);
        } else {
            link += v.getContext().getString(
                    R.string.change_details_comment_line_number, comment.line);
        }
        v.setText(link);
    }

    @BindingAdapter("accountStatus")
    public static void toAccountStatus(TextView view, String status) {
        view.setText(EmojiHelper.resolveEmojiStatus(view.getContext(), status));
    }

    @BindingAdapter("revisionKind")
    public static void toRevisionKind(TextView view, RevisionInfo revision) {
        if (revision == null) {
            view.setText(null);
            view.setVisibility(View.GONE);
            return;
        }

        String text = null;
        if (ChangeKind.REWORK.equals(revision.kind)) {
            text = view.getContext().getString(R.string.revision_kind_rework);
        } else if (ChangeKind.TRIVIAL_REBASE.equals(revision.kind)) {
            text = view.getContext().getString(R.string.revision_kind_trivial_rebase);
        } else if (ChangeKind.MERGE_FIRST_PARENT_UPDATE.equals(revision.kind)) {
            text = view.getContext().getString(R.string.revision_kind_merge_first_parent_update);
        }
        view.setText(text);
        view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("unresolvedComments")
    public static void toUnresolvedComments(TextView view, UnresolvedComment unresolvedComment) {
        if (unresolvedComment == null || unresolvedComment.mTotal == 0) {
            view.setVisibility(View.GONE);
            return;
        }

        final Context ctx = view.getContext();
        view.setText(unresolvedComment.mUnresolved == 0
                ? ctx.getString(R.string.unresolved_comments_zero, unresolvedComment.mTotal)
                : ctx.getString(R.string.unresolved_comments_more,
                        unresolvedComment.mTotal, unresolvedComment.mUnresolved));
        view.setVisibility(View.VISIBLE);
    }

    @BindingAdapter("emptyStateDrawable")
    public static void toEmptyStateDrawable(ImageView v, EmptyState state) {
        if (state == null || state.state == EmptyState.NORMAL_STATE) {
            v.setImageDrawable(null);
        } else if (state.state == EmptyState.NO_RESULTS_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_empty_results));
        } else if (state.state == EmptyState.ALL_DONE_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_duck));
        } else if (state.state == EmptyState.NOT_CONNECTIVITY_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_cloud_off));
        } else if (state.state == EmptyState.SERVER_CANNOT_BE_REACHED) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_server_network_off));
        } else if (state.state == EmptyState.ERROR_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_error_outline));
        } else if (state.state == EmptyState.NO_TRENDING_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_vitals));
        } else if (state.state == EmptyState.NO_FOLLOWING_STATE) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_follow_person));
        } else if (state.state == EmptyState.NO_SELECTION) {
            v.setImageDrawable(ContextCompat.getDrawable(
                    v.getContext(), R.drawable.ic_tap));
        }
    }

    @BindingAdapter("emptyStatedDescription")
    public static void toEmptyStateDescription(TextView v, EmptyState state) {
        if (state == null || state.state == EmptyState.NORMAL_STATE) {
            v.setText(null);
        } else if (state.state == EmptyState.NO_RESULTS_STATE) {
            v.setText(R.string.empty_states_no_results);
        } else if (state.state == EmptyState.ALL_DONE_STATE) {
            v.setText(R.string.empty_states_all_done);
        } else if (state.state == EmptyState.NOT_CONNECTIVITY_STATE) {
            v.setText(R.string.empty_states_not_connectivity);
        } else if (state.state == EmptyState.SERVER_CANNOT_BE_REACHED) {
            v.setText(R.string.empty_states_server_not_reached);
        } else if (state.state == EmptyState.ERROR_STATE) {
            v.setText(R.string.empty_states_error);
        } else if (state.state == EmptyState.NO_TRENDING_STATE) {
            v.setText(R.string.empty_states_no_trending);
        } else if (state.state == EmptyState.NO_FOLLOWING_STATE) {
            v.setText(R.string.empty_states_no_following);
        } else if (state.state == EmptyState.NO_SELECTION) {
            v.setText(R.string.empty_states_no_selection);
        }
    }

    public static boolean hasEmptyStateRetry(int state) {
        return state != EmptyState.NORMAL_STATE
                && state != EmptyState.NO_FOLLOWING_STATE
                && state != EmptyState.NO_SELECTION;
    }

    @BindingAdapter("humanReadableSize")
    public static void toHumanReadableSize(TextView v, Long size) {
        if (size == null) {
            v.setText(null);
            return;
        }
        v.setText(android.text.format.Formatter.formatFileSize(v.getContext(), size));
    }

    public static String toShortenCommit(String commitHash) {
        if (!TextUtils.isEmpty(commitHash) && commitHash.length() > 8) {
            return commitHash.substring(0, 8);
        }
        return commitHash;
    }

    @BindingAdapter("buildStatus")
    public static void toBuildStatus(ImageView v, ContinuousIntegrationInfo.BuildStatus status) {
        if (status == null) {
            v.setImageDrawable(null);
            return;
        }

        switch (status) {
            case SUCCESS:
                v.setImageResource(R.drawable.ic_status_success);
                break;
            case FAILURE:
                v.setImageResource(R.drawable.ic_status_failure);
                break;
            case SKIPPED:
                v.setImageResource(R.drawable.ic_status_skipped);
                break;
            case RUNNING:
            default:
                v.setImageResource(R.drawable.ic_status_running);
                break;
        }
    }

    // ./polygerrit-ui/app/elements/change-list/gr-change-list-item/gr-change-list-item.js
    @BindingAdapter("toCommitSize")
    public static void toCommitSize(TextView v, ChangeInfo change) {
        if (change == null) {
            v.setText(v.getContext().getString(R.string.commit_size_unknown));
            return;
        }

        int delta = change.insertions + change.deletions;
        final int resId;
        if (delta == 0) {
            v.setText(v.getContext().getString(R.string.commit_size_unknown));
            return;
        } else if (delta < 10) {
            resId = R.string.commit_size_xs;
        } else if (delta < 50) {
            resId = R.string.commit_size_s;
        } else if (delta < 250) {
            resId = R.string.commit_size_m;
        } else if (delta < 1000) {
            resId = R.string.commit_size_l;
        } else {
            resId = R.string.commit_size_xl;
        }
        v.setText(String.format(Locale.US, "%s  (+%d, -%d)",
                v.getContext().getString(resId),
                change.insertions,
                change.deletions));
    }

    @BindingAdapter("colorifyReviewedMessage")
    public static void colorifyReviewedMessage(View v, ChangeMessageInfo message) {
        if (message == null || message.message == null) {
            return;
        }
        if (!(sHighlightScoredMessages == Constants.HIGHLIGHT_SCORED_MESSAGE_MESSAGE
                || sHighlightScoredMessages == Constants.HIGHLIGHT_SCORED_MESSAGE_BOTH)) {
            return;
        }

        // Extract every review punctuation
        String firstLine = StringHelper.firstLine(message.message);
        int review = 0;
        boolean isPatchSetLine = StringHelper.PATCHSET_LINE_PATTERN.matcher(firstLine).matches();
        if (isPatchSetLine && !firstLine.contains(".")) {
            int start = firstLine.indexOf(": ") + 1;
            Matcher m = StringHelper.VOTE_PATTERN.matcher(firstLine);
            while (m.find()) {
                if (m.group(3) != null && m.start() == start) {
                    int value = StringHelper.parseNumberWithSign(m.group(3)).intValue();
                    if (Math.abs(value) > Math.abs(review)) {
                        review = value;
                    } else if (Math.abs(value) == Math.abs(review) && value < 0) {
                        review = value;
                    }
                    start = m.end();
                }
            }
        }

        int color = android.R.color.transparent;
        if (review < 0) {
            color = R.color.messageWithNegativeReview;
        }
        if (review > 0) {
            color = R.color.messageWithPositiveReview;
        }
        v.setBackgroundColor(ContextCompat.getColor(v.getContext(), color));
    }

    @BindingAdapter("resolveAttachmentIcon")
    public static void resolveAttachmentIcon(ImageView v, Attachment attachment) {
        if (attachment == null) {
            v.setImageDrawable(null);
            return;
        }

        String mimetype = attachment.mMimeType;
        if ("application/octet-stream".equals(attachment.mMimeType)) {
            attachment.mMimeType = StringHelper.getMimeType(new File(attachment.mName));
        }

        @DrawableRes int resource;
        @ColorRes int color;
        if (mimetype.equals("text/xml") || mimetype.equals("application/xml")
                || mimetype.contains("+xml")) {
            resource = R.drawable.ic_mime_xml;
            color = R.color.mime_type_default;
        } else if (mimetype.startsWith("image/")) {
            resource = R.drawable.ic_mime_image;
            color = R.color.mime_type_default;
        } else if (mimetype.startsWith("video/")) {
            resource = R.drawable.ic_mime_video;
            color = R.color.mime_type_default;
        } else if (mimetype.startsWith("audio/")) {
            resource = R.drawable.ic_mime_audio;
            color = R.color.mime_type_default;
        } else if (mimetype.startsWith("text/")) {
            resource = R.drawable.ic_mime_document;
            color = R.color.mime_type_default;
        } else if (mimetype.equals("application/internet-shortcut")
                || mimetype.equals("application/x-url")) {
            resource = R.drawable.ic_mime_url;
            color = R.color.mime_type_default;
        } else if (mimetype.contains("excel") || mimetype.contains("xls")
                || mimetype.contains("spreadsheet")) {
            resource = R.drawable.ic_mime_excel;
            color = R.color.mime_type_excel;
        } else if (mimetype.contains("word")) {
            resource = R.drawable.ic_mime_word;
            color = R.color.mime_type_word;
        } else if (mimetype.contains("powerpoint")) {
            resource = R.drawable.ic_mime_powerpoint;
            color = R.color.mime_type_powerpoint;
        } else if (mimetype.contains("pdf")) {
            resource = R.drawable.ic_mime_pdf;
            color = R.color.mime_type_pdf;
        } else {
            resource = R.drawable.ic_mime_file;
            color = R.color.mime_type_default;
        }

        Drawable dw = ContextCompat.getDrawable(v.getContext(), resource);
        v.setImageDrawable(BitmapUtils.tintDrawable(v.getResources(), dw,
                ContextCompat.getColor(v.getContext(), color)));
    }

    @BindingAdapter("bindToVersion")
    public static void bindToVersion(View v, double version) {
        v.setVisibility(ModelHelper.isEqualsOrGreaterVersionThan(mAccount, version)
                ? View.VISIBLE : View.GONE);
    }

    private static String toReviewUpdateMessage(
            Context ctx, List<ReviewerUpdateInfo> updates, ReviewerStatus state, boolean newLine) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (ReviewerUpdateInfo update : updates) {
            if (state.equals(update._extendedState)) {
                if (first) {
                    if (newLine) {
                        sb.append("<br/>");
                    }
                    sb.append(ctx.getResources().getStringArray(
                            R.array.reviewer_update_states)[state.ordinal()]);
                    sb.append(" ");
                } else {
                    sb.append(", ");
                }
                sb.append("<b>").append(toAccountDisplayName(update.reviewer)).append("</b>");
                first = false;
            }
        }
        return sb.toString();
    }

    private static void highlightUserMessageReviewScores(
            Context context, String userMessage, Spannable spannable) {
        if (!(sHighlightScoredMessages == Constants.HIGHLIGHT_SCORED_MESSAGE_SCORE
                || sHighlightScoredMessages == Constants.HIGHLIGHT_SCORED_MESSAGE_BOTH)) {
            return;
        }

        final int approved = ContextCompat.getColor(context, R.color.approved);
        final int rejected = ContextCompat.getColor(context, R.color.rejected);
        final int noscore = ContextCompat.getColor(context, R.color.noscore);

        boolean isPatchSetLine = StringHelper.PATCHSET_LINE_PATTERN.matcher(userMessage).matches();
        if (isPatchSetLine && !userMessage.contains(".")) {
            Matcher m = StringHelper.VOTE_PATTERN.matcher(userMessage);
            int start = userMessage.indexOf(": ") + 1;
            while (m.find()) {
                if (m.start() == start) {
                    int color = 0;
                    if (m.group(3) != null) {
                        // Vote added
                        color = StringHelper.parseNumberWithSign(m.group(3)).intValue() > 0
                                ? approved : rejected;
                    } else if (m.group(4) != null) {
                        // Vote removed
                        color = noscore;
                    }

                    final TagSpan span = new TagSpan(context, color, Color.WHITE);
                    spannable.setSpan(span, m.start(2), m.end(2), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                start = m.end();
            }
        }
    }
}
