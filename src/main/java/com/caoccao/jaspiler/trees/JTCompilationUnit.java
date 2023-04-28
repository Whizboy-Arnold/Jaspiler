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

import com.caoccao.jaspiler.JaspilerContract;
import com.caoccao.jaspiler.JaspilerOptions;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.*;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The type Jt compilation unit.
 * It references com.sun.tools.javac.tree.JCTree.JCCompilationUnit.
 */
public final class JTCompilationUnit
        extends JTTree<CompilationUnitTree, JTCompilationUnit>
        implements CompilationUnitTree {
    private final DocCommentTree docCommentTree;
    private final DocSourcePositions docSourcePositions;
    private final DocTrees docTrees;
    private final List<JTImport> imports;
    private final JaspilerOptions options;
    private final SourcePositions sourcePositions;
    private final Trees trees;
    private JTModuleTree moduleTree;
    private String originalCode;
    private JTPackageDecl packageTree;

    public JTCompilationUnit(
            Trees trees,
            DocTrees docTrees,
            CompilationUnitTree originalTree,
            JaspilerOptions options) {
        super(Objects.requireNonNull(originalTree), null);
        docCommentTree = Objects.requireNonNull(docTrees).getDocCommentTree(getOriginalTree().getSourceFile());
        docSourcePositions = docTrees.getSourcePositions();
        this.docTrees = docTrees;
        imports = new ArrayList<>();
        this.options = options;
        originalCode = null;
        packageTree = null;
        sourcePositions = Objects.requireNonNull(trees).getSourcePositions();
        this.trees = trees;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitCompilationUnit(this, data);
    }

    @Override
    public JTCompilationUnit analyze() {
        super.analyze();
        packageTree = Optional.ofNullable(getOriginalTree().getPackage())
                .map(o -> new JTPackageDecl(o, this).analyze())
                .orElse(null);
        getOriginalTree().getImports().stream()
                .map(o -> new JTImport(o, this).analyze())
                .forEach(imports::add);
        // TODO Type
        moduleTree = Optional.ofNullable(getOriginalTree().getModule())
                .map(o -> new JTModuleTree(o, this).analyze())
                .orElse(null);
        return this;
    }

    @Override
    List<JTTree<?, ?>> getAllNodes() {
        var nodes = super.getAllNodes();
        nodes.add(packageTree);
        imports.forEach(t -> nodes.add(t.setParentTree(this)));
        // TODO Type
        nodes.add(moduleTree);
        return nodes;
    }

    @Override
    public JTCompilationUnit getCompilationUnit() {
        return this;
    }

    public DocCommentTree getDocCommentTree() {
        return docCommentTree;
    }

    public DocTrees getDocTrees() {
        return docTrees;
    }

    @Override
    public List<JTImport> getImports() {
        return imports;
    }

    @Override
    public Kind getKind() {
        return Kind.COMPILATION_UNIT;
    }

    @Override
    public LineMap getLineMap() {
        return getOriginalTree().getLineMap();
    }

    @Override
    public ModuleTree getModule() {
        return null;
    }

    public JTModuleTree getModuleTree() {
        return moduleTree;
    }

    public JaspilerOptions getOptions() {
        return options;
    }

    @Override
    public String getOriginalCode() {
        if (originalCode == null) {
            try {
                originalCode = getOriginalTree().getSourceFile().getCharContent(true).toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return originalCode;
    }

    public JTPosition getOriginalDocPosition(DocTree docTree) {
        return docTree == null
                ? JTPosition.Invalid
                : JTPosition.from(docSourcePositions, this, docCommentTree, docTree);
    }

    public JTPosition getOriginalPosition(Tree tree) {
        return tree == null
                ? JTPosition.Invalid
                : JTPosition.from(sourcePositions, getOriginalTree(), tree);
    }

    @Override
    public JTPackageDecl getPackage() {
        return packageTree;
    }

    @Override
    public List<? extends AnnotationTree> getPackageAnnotations() {
        return null;
    }

    @Override
    public ExpressionTree getPackageName() {
        return getPackage().getPackageName();
    }

    @Override
    public JavaFileObject getSourceFile() {
        return getOriginalTree().getSourceFile();
    }

    public Trees getTrees() {
        return trees;
    }

    @Override
    public List<? extends Tree> getTypeDecls() {
        return null;
    }

    public boolean save(Path outputPath) throws IOException {
        return save(outputPath, StandardCharsets.UTF_8);
    }

    public boolean save(Path outputPath, Charset charset) throws IOException {
        if (getAction() == JaspilerContract.Action.Ignore) {
            return false;
        }
        JavaFileObject javaFileObject = getSourceFile();
        File file = outputPath.resolve(javaFileObject.getName().replace('.', '/') + javaFileObject.getKind().extension).toFile();
        logger.debug("Writing to {}.", file.getAbsolutePath());
        File parentFile = file.getParentFile();
        if (parentFile.exists()) {
            if (parentFile.isDirectory()) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        String errorMessage = MessageFormat.format(
                                "Cannot write to {0} because it is a directory.",
                                file.getAbsolutePath());
                        throw new IOException(errorMessage);
                    }
                    if (!file.canWrite()) {
                        String errorMessage = MessageFormat.format(
                                "Cannot write to {0} because it is not writable.",
                                file.getAbsolutePath());
                        throw new IOException(errorMessage);
                    }
                } else if (!parentFile.canWrite()) {
                    String errorMessage = MessageFormat.format(
                            "Cannot write to {0} because it is not writable.",
                            parentFile.getAbsolutePath());
                    throw new IOException(errorMessage);
                }
            } else {
                String errorMessage = MessageFormat.format(
                        "Cannot write to {0} because {1} is not a directory.",
                        file.getAbsolutePath(),
                        parentFile.getAbsolutePath());
                throw new IOException(errorMessage);
            }
        } else if (!parentFile.mkdirs()) {
            String errorMessage = MessageFormat.format(
                    "Cannot create {0}.",
                    parentFile.getAbsolutePath());
            throw new IOException(errorMessage);
        }
        try (FileWriter fileWriter = new FileWriter(file, charset)) {
            return save(fileWriter);
        }
    }

    @Override
    public boolean save(Writer writer) throws IOException {
        return super.save(writer);
    }

    @Override
    public JTCompilationUnit setActionChange() {
        action = JaspilerContract.Action.Change;
        return this;
    }

    public JTCompilationUnit setModuleTree(JTModuleTree moduleTree) {
        this.moduleTree = Objects.requireNonNull(moduleTree).setParentTree(this);
        return setActionChange();
    }

    public JTCompilationUnit setPackageTree(JTPackageDecl packageTree) {
        this.packageTree = Objects.requireNonNull(packageTree).setParentTree(this);
        return setActionChange();
    }

    @Override
    public String toString() {
        if (isActionChange()) {
            var stringBuilder = new StringBuilder();
            if (getOptions().isPreserveCopyrights()
                    && getOriginalPosition().isValid()
                    && getOriginalPosition().startPosition() > 0) {
                stringBuilder.append(getOriginalCode(), 0, (int) getOriginalPosition().startPosition());
            }
            stringBuilder.append(super.toString());
            return stringBuilder.toString();
        }
        return getOriginalCode();
    }
}
