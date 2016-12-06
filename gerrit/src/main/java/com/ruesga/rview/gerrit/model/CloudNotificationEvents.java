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
package com.ruesga.rview.gerrit.model;

/**
 * @link "https://github.com/jruesga/gerrit-cloud-notifications-plugin/blob/master/src/main/resources/Documentation/fcm.md#CloudNotificationEvents"
 */
public final class CloudNotificationEvents {
    public static final int CHANGE_ABANDONED_EVENT = 0x01;
    public static final int CHANGE_MERGED_EVENT = 0x02;
    public static final int CHANGE_RESTORED_EVENT = 0x04;
    public static final int CHANGE_REVERTED_EVENT = 0x08;
    public static final int COMMENT_ADDED_EVENT = 0x10;
    public static final int DRAFT_PUBLISHED_EVENT = 0x20;
    public static final int HASHTAG_CHANGED_EVENT = 0x40;
    public static final int REVIEWER_ADDED_EVENT = 0x80;
    public static final int REVIEWER_DELETED_EVENT = 0x100;
    public static final int PATCHSET_CREATED_EVENT = 0x200;
    public static final int TOPIC_CHANGED_EVENT = 0x400;
    public static final int ASSIGNEE_CHANGED_EVENT = 0x600;
}
