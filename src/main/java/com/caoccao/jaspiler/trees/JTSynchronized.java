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

import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.TreeVisitor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JTSynchronized
        extends JTStatement<SynchronizedTree, JTSynchronized>
        implements SynchronizedTree {
    private JTBlock block;
    private JTExpression<?, ?> expression;

    public JTSynchronized() {
        this(null, null);
        setActionChange();
    }

    JTSynchronized(SynchronizedTree synchronizedTree, JTTree<?, ?> parentTree) {
        super(synchronizedTree, parentTree);
        block = null;
        expression = null;
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitSynchronized(this, data);
    }

    @Override
    JTSynchronized analyze() {
        super.analyze();
        expression = JTTreeFactory.create(getOriginalTree().getExpression(), this);
        block = JTTreeFactory.create(getOriginalTree().getBlock(), this, JTBlock::new);
        return this;
    }

    @Override
    List<JTTree<?, ?>> getAllNodes() {
        var nodes = super.getAllNodes();
        Optional.ofNullable(expression).ifPresent(nodes::add);
        Optional.ofNullable(block).ifPresent(nodes::add);
        nodes.forEach(node -> node.setParentTree(this));
        return nodes;
    }

    @Override
    public JTBlock getBlock() {
        return block;
    }

    @Override
    public JTExpression<?, ?> getExpression() {
        return expression;
    }

    @Override
    public Kind getKind() {
        return Kind.SYNCHRONIZED;
    }

    public JTSynchronized setBlock(JTBlock block) {
        if (this.block == block) {
            return this;
        }
        this.block = Objects.requireNonNull(block).setParentTree(this);
        return setActionChange();
    }

    public JTSynchronized setExpression(JTExpression<?, ?> expression) {
        if (this.expression == expression) {
            return this;
        }
        this.expression = Objects.requireNonNull(expression).setParentTree(this);
        return setActionChange();
    }
}
