package com.github.lazy.tester;

import com.github.lazy.tester.model.MockCall;
import com.github.lazy.tester.model.TestMethod;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class ClassParser {

    private final CompilationUnit compilationUnit;

    private Map<String, MethodDeclaration> declaredMethods;
    private Map<String, FieldDeclaration> declaredClassFields;

    public ClassParser(Class<?> testeeClass) {
        this.compilationUnit = getCompilationUnit(testeeClass);
    }

    private CompilationUnit getCompilationUnit(Class<?> clazz) {
        var classPath = CodeGenerationUtils.mavenModuleRoot(clazz).resolve("src/main/java/" + clazz.getPackageName().replaceAll("\\.", "/"));
        return new SourceRoot(classPath).parse("", clazz.getSimpleName() + ".java");
    }

    public TestMethod convertToTestMethod(String methodName) {
        var mockCalls = getAllMethodCallExpressions(methodName).stream()
                .filter(methodCallExpr -> methodCallExpr.getScope().isPresent())
                .filter(this::isCallerDeclaredField)
                .map(this::convertToMockCall)
                .collect(toList());

        return new TestMethod(methodName, mockCalls);
    }

    private MockCall convertToMockCall(MethodCallExpr methodCallExpr) {
        var callerName = methodCallExpr.getScope().get().asNameExpr().getName().getId();
        var methodName = methodCallExpr.getName().getId();
        return new MockCall(callerName, methodName);
    }

    private boolean isCallerDeclaredField(MethodCallExpr methodCallExpr) {
        var scopeExpression = methodCallExpr.getScope().get();
        if (!scopeExpression.isNameExpr()) {
            return false;
        }
        var callerId = scopeExpression.asNameExpr().getName().getId();
        return getDeclaredClassFields().containsKey(callerId);
    }

    private ArrayList<MethodCallExpr> getAllMethodCallExpressions(String methodName) {
        var methodCalls = new ArrayList<MethodCallExpr>();
        getDeclaredMethods().get(methodName).accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(final MethodCallExpr n, final Object arg) {
                methodCalls.add(n);
            }
        }, null);
        return methodCalls;
    }

    public List<String> getDeclaredPublicMethods() {
        return getDeclaredMethods().values().stream()
                .filter(NodeWithPublicModifier::isPublic)
                .map(methodDeclaration -> methodDeclaration.getName().getId())
                .collect(toList());
    }

    private Map<String, FieldDeclaration> getDeclaredClassFields() {
        if (declaredClassFields == null) {
            declaredClassFields = extractDeclaredClassFields();
        }
        return declaredClassFields;
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

    private Map<String, MethodDeclaration> getDeclaredMethods() {
        if (declaredMethods == null) {
            declaredMethods = extractMethods();
        }
        return declaredMethods;
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
