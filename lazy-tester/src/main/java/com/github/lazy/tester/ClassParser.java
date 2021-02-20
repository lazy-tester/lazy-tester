package com.github.lazy.tester;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import com.github.lazy.tester.model.MethodCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    public List<MethodCall> getMockCalls(String methodName) {
        return getAllMethodCallExpressions(methodName).stream()
                .flatMap(this::getNestedMethodCalls)
                .filter(this::isCallerDeclaredField)
                .map(this::convertToMockCall)
                .collect(toList());
    }

    private Stream<MethodCallExpr> getNestedMethodCalls(MethodCallExpr methodCallExpr) {
        if (methodCallExpr.getScope().isPresent()) {
            return Stream.of(methodCallExpr);
        }
        return getAllMethodCallExpressions(methodCallExpr.getName().getId()).stream()
                .filter(nestedMethod -> nestedMethod.getScope().isPresent())
                .flatMap(this::getNestedMethodCalls);
    }

    private MethodCall convertToMockCall(MethodCallExpr methodCallExpr) {
        var callerName = methodCallExpr.getScope().get().asNameExpr().getName().getId();
        var methodName = methodCallExpr.getName().getId();
        return MethodCall.builder()
                .mockName(callerName)
                .method(methodName)
                .build();
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
