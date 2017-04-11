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
package com.ruesga.rview.model;

import android.support.annotation.Keep;
import android.view.View;

@Keep
public class EmptyState {

    @Keep
    public static abstract class EventHandlers {
        public abstract void onRetry(View v);
    }

    public static final int NORMAL_STATE = 0;
    public static final int NO_RESULTS_STATE = 1;
    public static final int ALL_DONE_STATE = 2;
    public static final int NOT_CONNECTIVITY_STATE = 3;
    public static final int SERVER_CANNOT_BE_REACHED = 4;
    public static final int ERROR_STATE = 5;

    public int state = NORMAL_STATE;
}