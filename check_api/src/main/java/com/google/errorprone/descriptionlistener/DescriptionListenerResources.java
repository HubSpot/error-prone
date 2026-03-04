package com.google.errorprone.descriptionlistener;

import com.google.auto.value.AutoValue;
import com.google.errorprone.fixes.ErrorProneEndPosTable;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

@AutoValue
public abstract class DescriptionListenerResources {
  public abstract Log getLog();

  public abstract ErrorProneEndPosTable getEndPositions();
  public abstract JCCompilationUnit getCompilation();
  public abstract Context getContext();
  public abstract boolean getUseErrors();

  public static DescriptionListenerResources create(Log log, ErrorProneEndPosTable endPositions,
      JCCompilationUnit compilation, Context context, boolean useErrors) {
    return new AutoValue_DescriptionListenerResources(log, endPositions, compilation, context,
        useErrors);
  }
}
