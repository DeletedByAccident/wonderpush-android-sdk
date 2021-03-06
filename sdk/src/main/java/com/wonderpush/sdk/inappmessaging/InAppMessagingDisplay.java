// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.wonderpush.sdk.inappmessaging;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

/**
 * The interface that a IAM display class must implement. Note that the developer is responsible
 * for calling the logging-related methods on InAppMessaging to track user-related metrics.
 */
@Keep
public interface InAppMessagingDisplay {
  /**
   * Called when an in-app message should be displayed. It's the responsibility of the implementation to display the in-app message
   * and use the callbacks parameter to report views and clicks.
   * @param inAppMessage - The in-app message.
   * @param callbacks - The object used to report views, clicks and dismisses.
   * @param delay - A delay in milliseconds. The implementation should wait this amount of time before triggering the display.
   * @return false when the message should be handled by the default, buit-in InAppMessagingDisplay instance. This instance will take care of reporting impressions and clicks to the display delegate. true if the message was handled
   */
  @Keep
  boolean displayMessage(
          @NonNull InAppMessage inAppMessage,
          @NonNull InAppMessagingDisplayCallbacks callbacks,
          long delay);
}
