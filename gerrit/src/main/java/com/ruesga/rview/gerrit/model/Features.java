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

import com.google.gson.annotations.Since;

public enum Features {
    @Since(2.12)
    ACCOUNT_DETAILS,
    @Since(2.14)
    ACCOUNT_STATUS,
    @Since(2.14)
    ASSIGNEE,
    @Since(2.12)
    AVATARS,
    @Since(2.13)
    BLAME,
    @Since(2.14)
    CC,
    @Since(2.15)
    CHANGE_FLAGS,
    @Since(2.14)
    CHANGE_TAGS,
    @Since(2.13)
    MOVE,
    @Since(2.14)
    REVISION_DESCRIPTION,
    @Since(2.14)
    TAGGED_MESSAGES,
    @Since(2.15)
    UNRESOLVED_COMMENTS,
    @Since(2.13)
    VOTES
}
