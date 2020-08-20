/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils.TypeCompatibilityReport;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
    name = "EqualsIncompatibleType",
    summary = "An equality test between objects with incompatible types always returns false",
    severity = WARNING)
public class EqualsIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> STATIC_EQUALS_MATCHER =
      staticEqualsInvocation();

  private static final Matcher<ExpressionTree> INSTANCE_EQUALS_MATCHER = instanceEqualsInvocation();

  private static final Matcher<Tree> ASSERT_FALSE_MATCHER =
      toType(
          MethodInvocationTree.class,
          anyOf(
              instanceMethod().anyClass().named("assertFalse"),
              staticMethod().anyClass().named("assertFalse")));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, final VisitorState state) {
    if (!STATIC_EQUALS_MATCHER.matches(invocationTree, state)
        && !INSTANCE_EQUALS_MATCHER.matches(invocationTree, state)) {
      return Description.NO_MATCH;
    }

    // This is the type of the object on which the java.lang.Object.equals() method
    // is called, either directly or indirectly via a static utility method. In the latter,
    // it is the type of the first argument to the static method.
    Type receiverType;
    // This is the type of the argument to the java.lang.Object.equals() method.
    // In case a static utility method is used, it is the type of the second argument
    // to this method.
    Type argumentType;

    if (STATIC_EQUALS_MATCHER.matches(invocationTree, state)) {
      receiverType = ASTHelpers.getType(invocationTree.getArguments().get(0));
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(1));
    } else {
      receiverType = ASTHelpers.getReceiverType(invocationTree);
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(0));
    }

    TypeCompatibilityReport compatibilityReport =
        TypeCompatibilityUtils.compatibilityOfTypes(receiverType, argumentType, state);
    if (compatibilityReport.compatible()) {
      return Description.NO_MATCH;
    }

    // Ignore callsites wrapped inside assertFalse:
    // assertFalse(objOfReceiverType.equals(objOfArgumentType))
    if (ASSERT_FALSE_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return Description.NO_MATCH;
    }

    // When we reach this point, we know that the two following facts hold:
    // (1) The types of the receiver and the argument to the eventual invocation of
    //     java.lang.Object.equals() are incompatible.
    // (2) No common superclass (other than java.lang.Object) or interface of the receiver and the
    //     argument defines an override of java.lang.Object.equals().
    // This equality test almost certainly evaluates to false, which is very unlikely to be the
    // programmer's intent. Hence, this is reported as an error. There is no sensible fix to suggest
    // in this situation.
    return buildDescription(invocationTree)
        .setMessage(
            getMessage(
                invocationTree,
                receiverType,
                argumentType,
                compatibilityReport.lhs(),
                compatibilityReport.rhs(),
                state))
        .build();
  }

  private static String getMessage(
      MethodInvocationTree invocationTree,
      Type receiverType,
      Type argumentType,
      Type conflictingReceiverType,
      Type conflictingArgumentType,
      VisitorState state) {
    TypeStringPair typeStringPair = new TypeStringPair(receiverType, argumentType);
    String baseMessage =
        "Calling "
            + ASTHelpers.getSymbol(invocationTree).getSimpleName()
            + " on incompatible types "
            + typeStringPair.getReceiverTypeString()
            + " and "
            + typeStringPair.getArgumentTypeString();

    // If receiver/argument are incompatible due to a conflict in the generic type, message that out
    if (!state.getTypes().isSameType(receiverType, conflictingReceiverType)) {
      TypeStringPair conflictingTypes =
          new TypeStringPair(conflictingReceiverType, conflictingArgumentType);
      baseMessage +=
          ". They are incompatible because "
              + conflictingTypes.getReceiverTypeString()
              + " and "
              + conflictingTypes.getArgumentTypeString()
              + " are incompatible.";
    }
    return baseMessage;
  }

  private static class TypeStringPair {
    private String receiverTypeString;
    private String argumentTypeString;

    private TypeStringPair(Type receiverType, Type argumentType) {
      receiverTypeString = Signatures.prettyType(receiverType);
      argumentTypeString = Signatures.prettyType(argumentType);
      if (argumentTypeString.equals(receiverTypeString)) {
        receiverTypeString = receiverType.toString();
        argumentTypeString = argumentType.toString();
      }
    }

    private String getReceiverTypeString() {
      return receiverTypeString;
    }

    private String getArgumentTypeString() {
      return argumentTypeString;
    }
  }

}
