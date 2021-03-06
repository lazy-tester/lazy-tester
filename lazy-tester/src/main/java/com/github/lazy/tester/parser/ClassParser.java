package com.github.lazy.tester.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.lazy.tester.model.MethodCall;
import com.github.lazy.tester.model.MethodParameter;
import com.github.lazy.tester.model.TestMethod;
import com.github.lazy.tester.model.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ClassParser {

    private final Class<?> testeeClass;
    private final CompilationUnitWrapper compilationUnit;

    public ClassParser(Class<?> testeeClass) {
        this.testeeClass = testeeClass;
        this.compilationUnit = new CompilationUnitWrapper(testeeClass);
    }

    public List<MethodCall> getMockCalls(String methodName) {
        return compilationUnit.getAllMethodCallExpressions(methodName).stream()
                .flatMap(this::getNestedMethodCalls)
                .filter(this::isCallerDeclaredField)
                .map(this::convertToMockCall)
                .collect(toList());
    }

    private Stream<MethodCallExpr> getNestedMethodCalls(MethodCallExpr methodCallExpr) {
        if (methodCallExpr.getScope().isPresent()) {
            return Stream.of(methodCallExpr);
        }
        return compilationUnit.getAllMethodCallExpressions(methodCallExpr.getName().getId()).stream()
                .flatMap(this::getNestedMethodCalls);
    }

    private MethodCall convertToMockCall(MethodCallExpr methodCallExpr) {
        var callerName = methodCallExpr.getScope().get().asNameExpr().getName().getId();
        var methodName = methodCallExpr.getName().getId();
        var returnType = getReturnVariableType(methodCallExpr);
        return MethodCall.builder()
                .mockName(callerName)
                .method(methodName)
                .returnType(returnType)
                .build();
    }

    private Type getReturnVariableType(MethodCallExpr methodCallExpr) {
        return methodCallExpr.getParentNode()
                .map(this::convertNodeToReturnType)
                .orElse(null);
    }

    private Type convertNodeToReturnType(Node node) {
        if (node instanceof VariableDeclarator) {
            var variableDeclarator = (VariableDeclarator) node;
            return Type.of(variableDeclarator.getName().getId(), variableDeclarator.getType().asString());
        } else if (node instanceof ReturnStmt) {
            return Type.of("undefined", "undefined");
        }
        return null;
    }

    private boolean isCallerDeclaredField(MethodCallExpr methodCallExpr) {
        var scopeExpression = methodCallExpr.getScope().get();
        if (!scopeExpression.isNameExpr()) {
            return false;
        }
        var callerId = scopeExpression.asNameExpr().getName().getId();
        return compilationUnit.getDeclaredClassFields().containsKey(callerId);
    }

    public List<String> getDeclaredPublicMethods() {
        return compilationUnit.getDeclaredMethods().values().stream()
                .filter(NodeWithPublicModifier::isPublic)
                .map(methodDeclaration -> methodDeclaration.getName().getId())
                .collect(toList());
    }

    public List<Field> getDeclaredFields() {
        return Arrays.asList(testeeClass.getDeclaredFields());
    }

    public TestMethod extractTestMethod(String methodName) {
        var mockCalls = getMockCalls(methodName);
        var returnType = getMethodReturnType(methodName);
        var parameters = getMethodParameters(methodName);

        return TestMethod.builder()
                .methodCalls(mockCalls)
                .name(methodName)
                .returnType(returnType)
                .parameters(parameters)
                .build();
    }

    private List<MethodParameter> getMethodParameters(String methodName) {
        var parameters = getReflectionMethod(methodName).get().getParameters();
        var methodParameters = new ArrayList<MethodParameter>();
        for (int i = 0; i < parameters.length; i++) {
            var parameterName = compilationUnit.getDeclaredMethod(methodName).getParameter(i).getName().getId();
            var methodParameter = MethodParameter.of(parameters[i].getType(), parameterName);
            methodParameters.add(methodParameter);
        }
        return methodParameters;
    }

    private Class<?> getMethodReturnType(String methodName) {
        return getReflectionMethod(methodName)
                .map(Method::getReturnType)
                .orElse(null);
    }

    private Optional<Method> getReflectionMethod(String methodName) {
        return Arrays.stream(testeeClass.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Objects.equals(methodName, method.getName()))
                .findFirst();
    }
}
