/// <reference types="../jaspiler/index.d.ts"/>

const assert = require('chai').assert;
const path = require('path');
const process = require('process');

const workingDirectory = process.cwd();
const pathMockPublicAnnotation = path.join(
  workingDirectory,
  '../../../src/test/java/com/caoccao/jaspiler/mock/MockPublicAnnotation.java');

function testBasicTransform() {
  const result = jaspiler.transformSync(pathMockPublicAnnotation);
  // Assert result, result.ast, result.code
  assert.isObject(result);
  assert.isObject(result.ast);
  const expectedLine = 'public @interface MockPublicAnnotation {';
  assert.include(result.code, expectedLine, 'The code string should be generated.');
  assert.include(result.ast.toString(), expectedLine, 'The ast.toString() should work.');
  assert.include('' + result.ast, expectedLine, 'The ast[Symbol.toPrimitive]() should work.');
  // Assert ast
  const ast = result.ast;
  const astImports = ast.imports;
  assert.isArray(astImports);
  assert.equal(2, astImports.length);
  const astModule = ast.module;
  assert.isNull(astModule);
  const astPackage = ast.package;
  assert.isObject(astPackage);
  assert.equal('package com.caoccao.jaspiler.mock;', astPackage.toString());
  const packageName = astPackage.packageName;
  assert.isObject(packageName);
  assert.equal('com.caoccao.jaspiler.mock', packageName.toString());
  const typeDecls = ast.typeDecls;
  assert.isArray(typeDecls);
  assert.equal(1, typeDecls.length);
  assert.include(typeDecls[0].toString(), expectedLine, 'The typeDecls[0].toString() should work.');
  assert.equal(pathMockPublicAnnotation, ast.sourceFile, 'The source file should match.');
}

// Package

function testIgnorePackage() {
  const result = jaspiler.transformSync(pathMockPublicAnnotation, {
    plugins: [{
      visitor: {
        Package(node) {
          assert.equal(0, node.annotations.length);
          assert.isTrue(node.isActionNoChange());
          assert.isFalse(node.isActionChange());
          assert.isFalse(node.isActionIgnore());
          assert.isTrue(node.setActionIgnore());
          assert.isFalse(node.isActionNoChange());
          assert.isFalse(node.isActionChange());
          assert.isTrue(node.isActionIgnore());
        },
      },
    }],
  });
  assert.notInclude(result.code, 'package com.caoccao.jaspiler.mock;');
}

function testReplacePackageName() {
  const result = jaspiler.transformSync(pathMockPublicAnnotation, {
    plugins: [{
      visitor: {
        Package(node) {
          assert.isTrue(node.isActionNoChange());
          node.packageName = jaspiler.createFieldAccess('abc', 'def', 'ghi');
          assert.isTrue(node.isActionChange());
        },
      },
    }],
  });
  assert.include(result.code, 'package abc.def.ghi;');
}

// Imports

function testImports() {
  const result = jaspiler.transformSync(pathMockPublicAnnotation, {
    plugins: [{
      visitor: {
        CompilationUnit(node) {
          assert.equal(pathMockPublicAnnotation, node.sourceFile, 'The source file should match.');
          const imports = node.imports;
          assert.equal(2, imports.length);
          imports.push(imports.shift());
          const newImport = jaspiler.newImport();
          newImport.qualifiedIdentifier = jaspiler.createFieldAccess('abc', 'def', 'ghi');
          newImport.staticImport = true;
          imports.push(newImport);
          node.imports = imports;
        },
      },
    }],
  });
  assert.include(
    result.code,
    'import java.lang.annotation.Inherited;\n'
    + 'import java.lang.annotation.Documented;\n'
    + 'import static abc.def.ghi;\n');
}

testBasicTransform();
// Package
testIgnorePackage();
testReplacePackageName();
// Imports
testImports();
