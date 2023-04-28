/*
 * Copyright (c) 2023-2023. caoccao.com Sam Cao
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

package com.caoccao.jaspiler;

import com.caoccao.jaspiler.contexts.JaspilerDocContext;
import com.caoccao.jaspiler.contexts.JaspilerTransformContext;
import com.caoccao.jaspiler.mock.MockIgnorePublicClass;
import com.caoccao.jaspiler.trees.*;
import com.caoccao.jaspiler.utils.BaseLoggingObject;
import com.caoccao.jaspiler.utils.MockUtils;
import com.caoccao.jaspiler.visiters.JaspilerDocScanner;
import com.caoccao.jaspiler.visiters.JaspilerTransformScanner;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.TreePathScanner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJaspilerCompiler extends BaseLoggingObject {
    @Test
    public void testTransform() throws IOException {
        class TestDocScanner extends DocTreeScanner<TestDocScanner, JaspilerDocContext> {
            @Override
            public TestDocScanner scan(DocTree node, JaspilerDocContext jaspilerDocContext) {
                var position = jaspilerDocContext.getCompilationUnitTree().getOriginalDocPosition(node);
                return super.scan(node, jaspilerDocContext);
            }
        }
        class TestTransformScanner extends TreePathScanner<TestTransformScanner, JaspilerTransformContext> {
            @Override
            public TestTransformScanner visitCompilationUnit(CompilationUnitTree node, JaspilerTransformContext jaspilerTransformContext) {
                var jtCompilationUnit = (JTCompilationUnit) node;
                JTImport jtImport = new JTImport();
                var jtFieldAccess1 = new JTFieldAccess();
                jtImport.setQualifiedIdentifier(jtFieldAccess1);
                jtFieldAccess1.setIdentifier(new JTName("i5"));
                var jtFieldAccess2 = new JTFieldAccess();
                jtFieldAccess1.setExpression(jtFieldAccess2);
                jtFieldAccess2.setIdentifier(new JTName("i4"));
                jtCompilationUnit.getImports().add(jtImport);
                return super.visitCompilationUnit(node, jaspilerTransformContext);
            }

            @Override
            public TestTransformScanner visitImport(ImportTree node, JaspilerTransformContext jaspilerTransformContext) {
                if (node.toString().contains("import java.util.*;")) {
                    var jtImport = (JTImport) node;
                    jtImport.setStaticImport(true);
                    var jtFieldAccess1 = new JTFieldAccess();
                    jtImport.setQualifiedIdentifier(jtFieldAccess1);
                    jtFieldAccess1.setIdentifier(new JTName("i3"));
                    var jtFieldAccess2 = new JTFieldAccess();
                    jtFieldAccess1.setExpression(jtFieldAccess2);
                    jtFieldAccess2.setIdentifier(new JTName("i2"));
                    var jtFieldAccess3 = new JTFieldAccess();
                    jtFieldAccess2.setExpression(jtFieldAccess3);
                    jtFieldAccess3.setIdentifier(new JTName("i1"));
                }
                return super.visitImport(node, jaspilerTransformContext);
            }

            @Override
            public TestTransformScanner visitPackage(PackageTree node, JaspilerTransformContext jaspilerTransformContext) {
                var packageTree = (JTPackageDecl) node;
                var jtFieldAccess1 = new JTFieldAccess();
                packageTree.setPackageName(jtFieldAccess1);
                jtFieldAccess1.setIdentifier(new JTName("a2"));
                var jtFieldAccess2 = new JTFieldAccess();
                jtFieldAccess1.setExpression(jtFieldAccess2);
                jtFieldAccess2.setIdentifier(new JTName("a1"));
                return super.visitPackage(node, jaspilerTransformContext);
            }
        }
        Path sourceFilePath = MockUtils.getSourcePath(MockIgnorePublicClass.class);
        var compiler = new JaspilerCompiler();
        compiler.addJavaFileObjects(sourceFilePath);
        try (StringWriter writer = new StringWriter()) {
            compiler.transform(
                    new JaspilerTransformScanner(),
                    new JaspilerDocScanner(),
                    writer,
                    JaspilerOptions.Default);
            String code = writer.toString();
            var texts = List.of(
                    "* Copyright (c)",
                    "package/* test */com./*1*/caoccao/*2*/.jaspiler.mock;",
                    "import com.caoccao/*1*/./*2*/jaspiler.JaspilerContract;");
            texts.forEach(text -> assertTrue(code.contains(text)));
        }
        try (StringWriter writer = new StringWriter()) {
            compiler.transform(
                    new TestTransformScanner(),
                    new TestDocScanner(),
                    writer,
                    JaspilerOptions.Default);
            String code = writer.toString();
            logger.debug(code);
            var texts = List.of(
                    "* Copyright (c)",
                    "package a1.a2;",
                    "import com.caoccao/*1*/./*2*/jaspiler.JaspilerContract;",
                    "import static i1.i2.i3;",
                    "import i4.i5;");
            texts.forEach(text -> assertTrue(code.contains(text)));
        }
    }
}
