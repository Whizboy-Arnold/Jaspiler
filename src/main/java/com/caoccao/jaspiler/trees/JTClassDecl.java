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

import com.caoccao.jaspiler.enums.JavaKeyword;
import com.caoccao.jaspiler.exceptions.JaspilerCheckedException;
import com.caoccao.jaspiler.exceptions.JaspilerNotSupportedException;
import com.caoccao.jaspiler.styles.IStyleWriter;
import com.caoccao.jaspiler.utils.ForEachUtils;
import com.caoccao.jaspiler.utils.V8Register;
import com.caoccao.javet.interfaces.IJavetBiFunction;
import com.caoccao.javet.interfaces.IJavetUniFunction;
import com.caoccao.javet.values.V8Value;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.TreeVisitor;

import java.util.*;

public final class JTClassDecl
        extends JTStatement<ClassTree, JTClassDecl>
        implements ClassTree {
    private static final String PROPERTY_EXTENDS_CLAUSE = "extendsClause";
    private static final String PROPERTY_IMPLEMENTS_CLAUSES = "implementsClauses";
    private static final String PROPERTY_MEMBERS = "members";
    private static final String PROPERTY_MODIFIERS = "modifiers";
    private static final String PROPERTY_PERMITS_CLAUSES = "permitsClauses";
    private static final String PROPERTY_SIMPLE_NAME = "simpleName";
    private static final String PROPERTY_TYPE_PARAMETERS = "typeParameters";
    private final List<JTExpression<?, ?>> implementsClauses;
    private final List<JTTree<?, ?>> members;
    private final List<JTExpression<?, ?>> permitsClauses;
    private final List<JTTypeParameter> typeParameters;
    private JTExpression<?, ?> extendsClause;
    private Kind kind;
    private JTModifiers modifiers;
    private JTName simpleName;

    public JTClassDecl() {
        this(null, null);
        setActionChange();
    }

    JTClassDecl(ClassTree classTree, JTTree<?, ?> parentTree) {
        super(classTree, parentTree);
        extendsClause = null;
        implementsClauses = new ArrayList<>();
        members = new ArrayList<>();
        modifiers = null;
        permitsClauses = new ArrayList<>();
        simpleName = null;
        typeParameters = new ArrayList<>();
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
    }

    @Override
    JTClassDecl analyze() {
        super.analyze();
        modifiers = JTTreeFactory.create(
                getOriginalTree().getModifiers(), this, JTModifiers::new);
        JTTreeFactory.createAndAdd(
                getOriginalTree().getTypeParameters(), this, JTTypeParameter::new, typeParameters::add);
        extendsClause = JTTreeFactory.create(getOriginalTree().getExtendsClause(), this);
        JTTreeFactory.createAndAdd(
                getOriginalTree().getImplementsClause(), this, (JTExpression<?, ?> o) -> implementsClauses.add(o));
        JTTreeFactory.createAndAdd(
                getOriginalTree().getPermitsClause(), this, (JTExpression<?, ?> o) -> permitsClauses.add(o));
        JTTreeFactory.createAndAdd(
                getOriginalTree().getMembers(), this, members::add);
        simpleName = JTTreeFactory.createName(getOriginalTree().getSimpleName());
        kind = getOriginalTree().getKind();
        if (Optional.ofNullable(modifiers).filter(IJTAnnotatable::containsIgnore).isPresent()) {
            setActionIgnore();
        }
        return this;
    }

    @Override
    List<JTTree<?, ?>> getAllNodes() {
        var nodes = super.getAllNodes();
        Optional.ofNullable(modifiers).ifPresent(nodes::add);
        typeParameters.stream().filter(Objects::nonNull).forEach(nodes::add);
        Optional.ofNullable(extendsClause).ifPresent(nodes::add);
        implementsClauses.stream().filter(Objects::nonNull).forEach(nodes::add);
        permitsClauses.stream().filter(Objects::nonNull).forEach(nodes::add);
        members.stream().filter(Objects::nonNull).forEach(nodes::add);
        nodes.forEach(node -> node.setParentTree(this));
        return nodes;
    }

    @Override
    public JTExpression<?, ?> getExtendsClause() {
        return extendsClause;
    }

    @Override
    public List<JTExpression<?, ?>> getImplementsClause() {
        return implementsClauses;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public List<JTTree<?, ?>> getMembers() {
        return members;
    }

    @Override
    public JTModifiers getModifiers() {
        return modifiers;
    }

    @Override
    public List<JTExpression<?, ?>> getPermitsClause() {
        return permitsClauses;
    }

    @Override
    public JTName getSimpleName() {
        return simpleName;
    }

    @Override
    public List<JTTypeParameter> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public Map<String, IJavetUniFunction<String, ? extends V8Value, JaspilerCheckedException>> proxyGetStringGetterMap() {
        if (stringGetterMap == null) {
            super.proxyGetStringGetterMap();
            V8Register.putStringGetter(stringGetterMap, PROPERTY_EXTENDS_CLAUSE, propertyName -> v8Runtime.toV8Value(getExtendsClause()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_IMPLEMENTS_CLAUSES, propertyName -> v8Runtime.toV8Value(getImplementsClause()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_MEMBERS, propertyName -> v8Runtime.toV8Value(getMembers()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_MODIFIERS, propertyName -> v8Runtime.toV8Value(getModifiers()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_PERMITS_CLAUSES, propertyName -> v8Runtime.toV8Value(getPermitsClause()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_SIMPLE_NAME, propertyName -> v8Runtime.toV8Value(getSimpleName()));
            V8Register.putStringGetter(stringGetterMap, PROPERTY_TYPE_PARAMETERS, propertyName -> v8Runtime.toV8Value(getTypeParameters()));
        }
        return stringGetterMap;
    }

    @Override
    public Map<String, IJavetBiFunction<String, V8Value, Boolean, JaspilerCheckedException>> proxyGetStringSetterMap() {
        if (stringSetterMap == null) {
            super.proxyGetStringSetterMap();
            V8Register.putStringSetter(stringSetterMap, PROPERTY_EXTENDS_CLAUSE,
                    (propertyName, propertyValue) -> replaceExpression(this::setExtendsClause, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_IMPLEMENTS_CLAUSES,
                    (propertyName, propertyValue) -> replaceExpressions(implementsClauses, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_MEMBERS,
                    (propertyName, propertyValue) -> replaceTrees(members, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_MODIFIERS,
                    (propertyName, propertyValue) -> replaceModifiers(this::setModifiers, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_PERMITS_CLAUSES,
                    (propertyName, propertyValue) -> replaceExpressions(permitsClauses, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_SIMPLE_NAME,
                    (propertyName, propertyValue) -> replaceName(this::setSimpleName, propertyValue));
            V8Register.putStringSetter(stringSetterMap, PROPERTY_TYPE_PARAMETERS,
                    (propertyName, propertyValue) -> replaceTypeParameters(typeParameters, propertyValue));
        }
        return stringSetterMap;
    }

    @Override
    public boolean save(IStyleWriter<?> writer) {
        if (isActionChange()) {
            int indent = getIndent();
            int childIndent = indent + getCompilationUnit().getOptions().getIndentSize();
            Optional.ofNullable(modifiers).ifPresent(writer::append);
            ForEachUtils.forEach(
                    typeParameters.stream().filter(Objects::nonNull).filter(tree -> !tree.isActionIgnore()).toList(),
                    writer::append,
                    tree -> writer.appendComma().appendSpace(),
                    trees -> writer.appendSpaceIfNeeded().appendLeftArrow(),
                    trees -> writer.appendSpaceIfNeeded().appendRightArrow());
            switch (kind) {
                case ANNOTATION_TYPE -> {
                    if (modifiers.isActionChange()) {
                        writer.appendSpaceIfNeeded().appendAt();
                    }
                    writer.appendKeyword(JavaKeyword.INTERFACE);
                }
                case CLASS -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.CLASS);
                case ENUM -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.ENUM);
                case INTERFACE -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.INTERFACE);
                case RECORD -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.RECORD);
            }
            writer.appendSpaceIfNeeded().append(simpleName);
            Optional.ofNullable(extendsClause)
                    .ifPresent(tree -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.EXTENDS).appendSpace().append(tree));
            ForEachUtils.forEach(
                    implementsClauses.stream().filter(Objects::nonNull).filter(tree -> !tree.isActionIgnore()).toList(),
                    writer::append,
                    tree -> writer.appendComma().appendSpace(),
                    trees -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.IMPLEMENTS).appendSpace());
            ForEachUtils.forEach(
                    permitsClauses.stream().filter(Objects::nonNull).filter(tree -> !tree.isActionIgnore()).toList(),
                    writer::append,
                    tree -> writer.appendComma().appendSpace(),
                    trees -> writer.appendSpaceIfNeeded().appendKeyword(JavaKeyword.PERMITS).appendSpace());
            writer.appendSpaceIfNeeded().appendLeftCurlyBracket().appendLineSeparator();
            ForEachUtils.forEach(
                    members.stream().filter(Objects::nonNull).filter(tree -> !tree.isActionIgnore()).toList(),
                    tree -> writer.appendSpace(childIndent).append(tree).appendLineSeparator(),
                    tree -> writer.appendLineSeparator());
            writer.appendSpace(indent).appendRightCurlyBracket().appendLineSeparator();
            return true;
        }
        return super.save(writer);
    }

    public JTClassDecl setExtendsClause(JTExpression<?, ?> extendsClause) {
        if (this.extendsClause == extendsClause) {
            return this;
        }
        this.extendsClause = Objects.requireNonNull(extendsClause).setParentTree(this);
        return setActionChange();
    }

    public JTClassDecl setKind(Kind kind) {
        if (this.kind == kind) {
            return this;
        }
        switch (Objects.requireNonNull(kind)) {
            case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE, RECORD -> this.kind = kind;
            default -> throw new JaspilerNotSupportedException(kind.name() + " is not supported.");
        }
        return setActionChange();
    }

    public JTClassDecl setModifiers(JTModifiers modifiers) {
        if (this.modifiers == modifiers) {
            return this;
        }
        this.modifiers = Objects.requireNonNull(modifiers).setParentTree(this);
        return setActionChange();
    }

    public JTClassDecl setSimpleName(JTName simpleName) {
        if (this.simpleName == simpleName) {
            return this;
        }
        this.simpleName = Objects.requireNonNull(simpleName);
        return setActionChange();
    }
}
