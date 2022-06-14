/*
 * Copyright 2022 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.hubspot;

import com.google.errorprone.ErrorProneAnalyzer;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Context;

public class HubSpotErrorProneAnalyzer implements TaskListener {
  private final Context context;
  private final ErrorProneOptions options;
  private final TaskListener standardAnalyzer;


  public static HubSpotErrorProneAnalyzer create(ScannerSupplier scannerSupplier, ErrorProneOptions options, Context context) {
    // Note: This analyzer doesn't make any attempt to handle refaster refactorings. We fall back on
    // standard analyzer impl if refaster is requested

    TaskListener standardAnalyzer = ErrorProneAnalyzer.createByScanningForPlugins(scannerSupplier, options, context);

    if (HubSpotUtils.isErrorHandlingEnabled(options)) {
      standardAnalyzer = ErrorReportingAnalyzer.wrap(standardAnalyzer);
    }

    return new HubSpotErrorProneAnalyzer(context, options, standardAnalyzer);
  }

  private HubSpotErrorProneAnalyzer(Context context, ErrorProneOptions options, TaskListener standardAnalyzer) {
    this.context = context;
    this.options = options;
    this.standardAnalyzer = standardAnalyzer;
  }

  @Override
  public void started(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleStartup();
    }

    standardAnalyzer.started(taskEvent);
  }

  @Override
  public void finished(TaskEvent taskEvent) {
    standardAnalyzer.finished(taskEvent);

    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleShutdown();
    }
  }
}
