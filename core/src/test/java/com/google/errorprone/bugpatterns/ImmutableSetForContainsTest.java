/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ImmutableSetForContains}. */
@RunWith(JUnit4.class)
public final class ImmutableSetForContainsTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ImmutableSetForContains.class, getClass());

  @Test
  public void immutableListOf_onlyContainsReplaces() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList<String> MY_LIST_1 =",
            "           ImmutableList.<String>builder().add(\"hello\").build();",
            "  private static final ImmutableList<String> MY_LIST_2 = ImmutableList.of(\"hello\");",
            "  private static final ImmutableList<String> MY_LIST_3 =",
            "           new ArrayList<String>().stream().collect(toImmutableList());",
            "  private void myFunc() {",
            "    boolean myBool1 = MY_LIST_1.contains(\"he\");",
            "    boolean myBool2 = MY_LIST_2.containsAll(new ArrayList<String>());",
            "    boolean myBool3 = MY_LIST_3.isEmpty();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import static com.google.common.collect.ImmutableSet.toImmutableSet;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableSet<String> MY_LIST_1 =",
            "           ImmutableSet.<String>builder().add(\"hello\").build();",
            "  private static final ImmutableSet<String> MY_LIST_2 = ImmutableSet.of(\"hello\");",
            "  private static final ImmutableSet<String> MY_LIST_3 =",
            "           new ArrayList<String>().stream().collect(toImmutableSet());",
            "  private void myFunc() {",
            "    boolean myBool1 = MY_LIST_1.contains(\"he\");",
            "    boolean myBool2 = MY_LIST_2.containsAll(new ArrayList<String>());",
            "    boolean myBool3 = MY_LIST_3.isEmpty();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableList_initUsingStaticFunc_replacesWithCopyOf() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList<String> MY_LIST = initMyList();",
            "  private static ImmutableList<String> initMyList() {",
            "    return ImmutableList.of();",
            "  }",
            "  private void myFunc() {",
            "    boolean myBool = MY_LIST.contains(\"he\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableSet<String> MY_LIST = ",
            "                ImmutableSet.copyOf(initMyList());",
            "  private static ImmutableList<String> initMyList() {",
            "    return ImmutableList.of();",
            "  }",
            "  private void myFunc() {",
            "    boolean myBool = MY_LIST.contains(\"he\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableList_rawType_replacesWithImmutableSet() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList MY_LIST = ImmutableList.of(\"hello\");",
            "  private void myFunc() {",
            "    boolean myBool = MY_LIST.contains(\"he\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableSet MY_LIST = ImmutableSet.of(\"hello\");",
            "  private void myFunc() {",
            "    boolean myBool = MY_LIST.contains(\"he\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldAnnotatedWithBind_noMatch() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import java.util.ArrayList;",
            "class Test {",
            "  @Bind",
            "  private static final ImmutableList MY_LIST = ImmutableList.of(\"hello\");",
            "  private void myFunc() {",
            "    boolean myBool = MY_LIST.contains(\"he\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void immutableVarPassedToAFunc_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  private static final ImmutableList<String> MY_LIST = ImmutableList.of(\"hello\");",
            "  private void myFunc() {",
            "    consumer(MY_LIST);",
            "  }",
            "  private void consumer(ImmutableList<String> arg) {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void immutableList_uniqueElements_iterating_replacesWithSet() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;",
            "import com.google.common.collect.ImmutableList;",
            "import com.sun.source.tree.Tree.Kind;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList<String> STR_LIST = ImmutableList.of(\"hello\");",
            "  private static final ImmutableList<Kind> ENUM_LIST =",
            "                                  ImmutableList.of(Kind.AND, METHOD_INVOCATION);",
            "  private void myFunc() {",
            "    STR_LIST.stream().forEach(System.out::println);",
            "    STR_LIST.forEach(System.out::println);",
            "    ENUM_LIST.stream().forEach(System.out::println);",
            "    ENUM_LIST.forEach(System.out::println);",
            "    for (String myStr : STR_LIST) { System.out.println(myStr); }",
            "    for (Kind myKind : ENUM_LIST) { System.out.println(myKind); }",
            "    for (Long lvar : ImmutableList.<Long>of(2L)) { System.out.println(lvar); }",
            "    ImmutableList<Long> longList = ImmutableList.of(1L);",
            "    for (Long lvar : longList) { System.out.println(lvar); }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import com.sun.source.tree.Tree.Kind;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableSet<String> STR_LIST = ImmutableSet.of(\"hello\");",
            "  private static final ImmutableSet<Kind> ENUM_LIST =",
            "                                  ImmutableSet.of(Kind.AND, METHOD_INVOCATION);",
            "  private void myFunc() {",
            "    STR_LIST.stream().forEach(System.out::println);",
            "    STR_LIST.forEach(System.out::println);",
            "    ENUM_LIST.stream().forEach(System.out::println);",
            "    ENUM_LIST.forEach(System.out::println);",
            "    for (String myStr : STR_LIST) { System.out.println(myStr); }",
            "    for (Kind myKind : ENUM_LIST) { System.out.println(myKind); }",
            "    for (Long lvar : ImmutableList.<Long>of(2L)) { System.out.println(lvar); }",
            "    ImmutableList<Long> longList = ImmutableList.of(1L);",
            "    for (Long lvar : longList) { System.out.println(lvar); }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableList_duplicateElements_iterating_doesNotReplace() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList<String> STR_LIST_1 = ",
            "        ImmutableList.of(\"hello\", \"hello\");",
            "  private static final ImmutableList<String> STR_LIST_2 = ",
            "        ImmutableList.of(\"hello\", strGenFunc());",
            "  private void myFunc() {",
            "    STR_LIST_1.stream().forEach(System.out::println);",
            "    STR_LIST_2.stream().forEach(System.out::println);",
            "  }",
            "  private static String strGenFunc() { return \"\"; }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void immutableList_distinctElementsInBuilder_iterating_doesNotReplace() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private static final ImmutableList<String> STR_LIST = ",
            "        ImmutableList.<String>builder().add(\"hello\").build();",
            "  private void myFunc() {",
            "    STR_LIST.stream().forEach(System.out::println);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
