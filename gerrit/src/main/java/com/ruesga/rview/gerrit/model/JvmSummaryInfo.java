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

import com.google.gson.annotations.SerializedName;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#jvm-summary-info"
 */
public class JvmSummaryInfo {
    @SerializedName("vm_vendor") public String vmVendor;
    @SerializedName("vm_name") public String vmName;
    @SerializedName("vm_version") public String vmVersion;
    @SerializedName("os_name") public String osName;
    @SerializedName("os_version") public String osVersion;
    @SerializedName("os_arch") public String osArch;
    @SerializedName("user") public String user;
    @SerializedName("host") public String host;
    @SerializedName("current_working_directory") public String currentWorkingDirectory;
    @SerializedName("site") public String site;
}
