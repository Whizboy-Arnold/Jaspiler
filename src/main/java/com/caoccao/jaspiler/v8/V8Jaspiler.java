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

package com.caoccao.jaspiler.v8;

import com.caoccao.jaspiler.JaspilerCompiler;
import com.caoccao.jaspiler.exceptions.JaspilerArgumentException;
import com.caoccao.jaspiler.exceptions.JaspilerCheckedException;
import com.caoccao.jaspiler.exceptions.JaspilerParseException;
import com.caoccao.jaspiler.options.JaspilerTransformOptions;
import com.caoccao.jaspiler.trees.JTTreeFactory;
import com.caoccao.jaspiler.utils.BaseLoggingObject;
import com.caoccao.jaspiler.utils.V8Register;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interfaces.IJavetClosable;
import com.caoccao.javet.interfaces.IJavetUniFunction;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.V8Scope;
import com.caoccao.javet.interop.proxy.IJavetDirectProxyHandler;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.primitive.V8ValueString;
import com.caoccao.javet.values.reference.V8ValueObject;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class V8Jaspiler
        extends BaseLoggingObject
        implements IJavetDirectProxyHandler<JaspilerCheckedException>, IJavetClosable {
    public static final String NAME = "jaspiler";
    protected static final String FUNCTION_CREATE_FIELD_ACCESS = "createFieldAccess";
    protected static final String FUNCTION_TRANSFORM_SYNC = "transformSync";
    protected static final String PROPERTIES_AST = "ast";
    protected static final String PROPERTIES_CODE = "code";
    protected JaspilerCompiler jaspilerCompiler;
    protected Map<String, IJavetUniFunction<String, ? extends V8Value, JaspilerCheckedException>> stringGetterMap;
    protected V8Runtime v8Runtime;

    public V8Jaspiler(V8Runtime v8Runtime) {
        super();
        jaspilerCompiler = new JaspilerCompiler();
        stringGetterMap = null;
        this.v8Runtime = v8Runtime;
    }

    @Override
    public void close() {
        jaspilerCompiler = null;
    }

    public V8Value createFieldAccess(V8Value... v8Values) throws JavetException {
        if (ArrayUtils.isEmpty(v8Values)) {
            return v8Runtime.createV8ValueUndefined();
        }
        String[] strings = new String[v8Values.length];
        for (int i = 0; i < v8Values.length; ++i) {
            if (v8Values[i] instanceof V8ValueString v8ValueString) {
                strings[i] = v8ValueString.getValue();
            }
        }
        return v8Runtime.toV8Value(JTTreeFactory.createFieldAccess(strings));
    }

    @Override
    public V8Runtime getV8Runtime() {
        return v8Runtime;
    }

    @Override
    public boolean isClosed() {
        return jaspilerCompiler == null;
    }

    @Override
    public Map<String, IJavetUniFunction<String, ? extends V8Value, JaspilerCheckedException>> proxyGetStringGetterMap() {
        if (stringGetterMap == null) {
            stringGetterMap = new HashMap<>();
            V8Register.putStringGetter(v8Runtime, stringGetterMap, FUNCTION_CREATE_FIELD_ACCESS, this::createFieldAccess);
            V8Register.putStringGetter(v8Runtime, stringGetterMap, FUNCTION_TRANSFORM_SYNC, this::transformSync);
        }
        return stringGetterMap;
    }

    public V8Value transformSync(V8Value... v8Values) throws JavetException, JaspilerCheckedException {
        File file = validateFile(validateString(FUNCTION_TRANSFORM_SYNC, v8Values, 0));
        try (var v8JaspilerOptions = new V8JaspilerOptions();
             var jaspilerTransformScanner = new V8JaspilerTransformScanner(v8JaspilerOptions);
             var jaspilerDocScanner = new V8JaspilerDocScanner();
             var stringWriter = new StringWriter()) {
            if (v8Values.length > 1) {
                v8JaspilerOptions.deserialize(validateObject(FUNCTION_TRANSFORM_SYNC, v8Values, 1));
            }
            jaspilerCompiler.clearJavaFileObject();
            jaspilerCompiler.addJavaFileObjects(file);
            jaspilerCompiler.transform(
                    jaspilerTransformScanner,
                    jaspilerDocScanner,
                    JaspilerTransformOptions.Default);
            var compilationUnitTree = jaspilerCompiler.getTransformContexts().get(0).getCompilationUnitTree();
            try (V8Scope v8Scope = v8Runtime.getV8Scope()) {
                var v8ValueObjectResult = v8Scope.createV8ValueObject();
                if (compilationUnitTree.save(stringWriter)) {
                    v8ValueObjectResult.set(
                            PROPERTIES_AST, compilationUnitTree,
                            PROPERTIES_CODE, stringWriter.toString());
                } else {
                    v8ValueObjectResult.set(
                            PROPERTIES_AST, null,
                            PROPERTIES_CODE, null);
                }
                v8Scope.setEscapable();
                return v8ValueObjectResult;
            }
        } catch (IOException e) {
            throw new JaspilerParseException(e.getMessage(), e);
        }
    }

    protected File validateFile(String filePath) throws JaspilerArgumentException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new JaspilerArgumentException(
                    MessageFormat.format("[{0}] is not found.", file.getAbsolutePath()));
        }
        if (!file.isFile()) {
            throw new JaspilerArgumentException(
                    MessageFormat.format("[{0}] is not a file.", file.getAbsolutePath()));
        }
        if (!file.canRead()) {
            throw new JaspilerArgumentException(
                    MessageFormat.format("File [{0}] cannot be read.", file.getAbsolutePath()));
        }
        return file;
    }

    protected void validateLength(
            String functionName, V8Value[] v8Values, int index)
            throws JaspilerArgumentException {
        if (v8Values == null || v8Values.length < index || index < 0) {
            throw new JaspilerArgumentException(
                    MessageFormat.format("Argument count mismatches in {0}.", functionName));
        }
    }

    protected V8ValueObject validateObject(
            String functionName, V8Value[] v8Values, int index)
            throws JaspilerArgumentException {
        validateLength(functionName, v8Values, index);
        V8Value v8Value = v8Values[index];
        if (v8Value instanceof V8ValueObject v8ValueObject) {
            return v8ValueObject;
        }
        throw new JaspilerArgumentException(
                MessageFormat.format("Argument type mismatches in {0}. Object is expected.", functionName));
    }

    protected String validateString(
            String functionName, V8Value[] v8Values, int index)
            throws JaspilerArgumentException {
        validateLength(functionName, v8Values, index);
        V8Value v8Value = v8Values[index];
        if (v8Value instanceof V8ValueString v8ValueString) {
            return v8ValueString.getValue();
        }
        throw new JaspilerArgumentException(
                MessageFormat.format("Argument type mismatches in {0}. String is expected.", functionName));
    }
}