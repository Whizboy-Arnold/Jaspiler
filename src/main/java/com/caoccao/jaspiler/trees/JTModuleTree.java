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

import com.sun.source.tree.*;

import java.util.List;

public final class JTModuleTree
        extends JTTree<ModuleTree, JTModuleTree>
        implements ModuleTree {
    public JTModuleTree() {
        this(null, null);
        setActionChange();
    }

    JTModuleTree(ModuleTree moduleTree, JTTree<?, ?> parentTree) {
        super(moduleTree, parentTree);
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitModule(this, data);
    }

    @Override
    JTModuleTree analyze() {
        super.analyze();
        // TODO
        return this;
    }

    @Override
    public List<? extends AnnotationTree> getAnnotations() {
        return null;
    }

    @Override
    public List<? extends DirectiveTree> getDirectives() {
        return null;
    }

    @Override
    public Kind getKind() {
        return Kind.MODULE;
    }

    @Override
    public ModuleKind getModuleType() {
        return null;
    }

    @Override
    public ExpressionTree getName() {
        return null;
    }
}
