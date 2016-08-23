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
package com.ruesga.rview.gerrit;

public interface PlatformAbstractionLayer {
    /**
     * Return if the app is running in debug mode
     */
    boolean isDebugBuild();

    /**
     * Log a message from gerrit
     */
    void log(String message);

    /**
     * Encode data to base64
     */
    byte[] encodeBase64(byte[] data);

    /**
     * Decode data from base64
     */
    byte[] decodeBase64(byte[] data);

    /**
     * Determines if there are network connectivity
     */
    boolean hasConnectivity();
}
