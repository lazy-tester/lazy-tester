package com.github.lazy.tester.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompilationUnitWrapper {

    private final CompilationUnit compilationUnit;

    private Map<String, MethodDeclaration> declaredMethods;
    private Map<String, FieldDeclaration> declaredClassFields;

    public CompilationUnitWrapper(Class<?> testeeClass) {
        this.compilationUnit = getCompilationUnit(testeeClass);
    }

    private CompilationUnit getCompilationUnit(Class<?> clazz) {
        var classPath = CodeGenerationUtils.mavenModuleRoot(clazz).resolve("src/main/java/" + clazz.getPackageName().replaceAll("\\.", "/"));
        return new SourceRoot(classPath).parse("", clazz.getSimpleName() + ".java");
    }

    public ArrayList<MethodCallExpr> getAllMethodCallExpressions(String methodName) {
        var methodCalls = new ArrayList<MethodCallExpr>();
        getDeclaredMethods().get(methodName).accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(final MethodCallExpr n, final Object arg) {
                methodCalls.add(n);
            }
        }, null);
        return methodCalls;
    }

    public Map<String, FieldDeclaration> getDeclaredClassFields() {
        if (declaredClassFields == null) {
            declaredClassFields = extractDeclaredClassFields();
        }
        return declaredClassFields;
    }

    public MethodDeclaration getDeclaredMethod(String methodName) {
        return getDeclaredMethods().get(methodName);
    }

    public Map<String, MethodDeclaration> getDeclaredMethods() {
        if (declaredMethods == null) {
            declaredMethods = extractMethods();
        }
        return declaredMethods;
    }

    private Map<String, FieldDeclaration> extractDeclaredClassFields() {
        var fields = new HashMap<String, FieldDeclaration>();
        compilationUnit.accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(final FieldDeclaration n, final Object arg) {
                fields.put(n.getVariables().get(0).getName().getId(), n);
            }
        }, null);
        return fields;
    }

    private Map<String, MethodDeclaration> extractMethods() {
        var methods = new HashMap<String, MethodDeclaration>();
        compilationUnit.accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(final MethodDeclaration n, final Object arg) {
                methods.put(n.getName().getId(), n);
            }
        }, null);
        return methods;
    }
}
