/*
 * Copyright (c) 2023. caoccao.com Sam Cao
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

package com.caoccao.jaspiler.trees;

import com.caoccao.jaspiler.BaseTestSuite;
import com.caoccao.jaspiler.contexts.JaspilerTransformContext;
import com.caoccao.jaspiler.mock.MockAllInOnePublicClass;
import com.caoccao.jaspiler.visiters.JaspilerTransformScanner;
import com.sun.source.tree.MethodTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJTMethodDecl extends BaseTestSuite {
    @Test
    public void testUpdateName() throws Exception {
        String[] newMethodNames = new String[]{"ANewMethodName", "mockNames", "mockValue"};
        class TestTransformScanner extends JaspilerTransformScanner<TestTransformScanner> {
            @Override
            public TestTransformScanner visitMethod(MethodTree node, JaspilerTransformContext jaspilerTransformContext) {
                var jtMethodDecl = (JTMethodDecl) node;
                if ("Test".equals(jtMethodDecl.getName().getValue())) {
                    jtMethodDecl.setName(new JTName(newMethodNames[0]));
                } else if ("names".equals(jtMethodDecl.getName().getValue())) {
                    jtMethodDecl.setName(new JTName(newMethodNames[1]));
                } else if ("value".equals(jtMethodDecl.getName().getValue())) {
                    jtMethodDecl.setName(new JTName(newMethodNames[2]));
                }
                return super.visitMethod(node, jaspilerTransformContext);
            }
        }
        String code = transform(new TestTransformScanner(), MockAllInOnePublicClass.class);
        List<String> texts = List.of(
                "public final <T> void " +
                        newMethodNames[0] + "(T x, @Deprecated int y) " +
                        "throws IOException, NoClassDefFoundError {",
                "String[] mockNames() default {\"A\", \"B\"};",
                "String mockValue() default \"value\";");
        texts.forEach(text -> assertTrue(code.contains(text)));
    }
}