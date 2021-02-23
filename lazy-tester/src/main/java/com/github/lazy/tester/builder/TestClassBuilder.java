package com.github.lazy.tester.builder;

import com.github.javaparser.JavaParser;
import com.github.lazy.tester.model.MethodCall;
import com.github.lazy.tester.model.TestMethod;
import com.sun.codemodel.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class TestClassBuilder {

    private static final JavaParser JAVA_PARSER = new JavaParser();
    private static final String EXPECTED_RESULT_VARIABLE_NAME = "expectedResult";

    private final JCodeModel codeModel = new JCodeModel();
    private final JDefinedClass definedClass;
    private final EasyRandom generator;

    private JFieldVar testee;

    public TestClassBuilder(Class<?> testeeClass) {
        definedClass = buildDefinedClass(testeeClass.getSimpleName(), testeeClass.getPackageName());
        generator = createRandomDataGenerator();
    }

    public void addMockField(Class<?> mockClass) {
        var mock = definedClass.field(JMod.NONE, codeModel.ref(mockClass), getSimpleVariableName(mockClass));
        mock.annotate(Mock.class);
    }

    public void addTestMethod(TestMethod testMethod) {
        var body = createTestMethod(testMethod);

        addGiven(testMethod, body);
        addWhen(testMethod, body);
        addThenAndAssert(testMethod, body);
        addVerify(testMethod, body);
    }

    private JBlock createTestMethod(TestMethod testMethod) {
        var name = "should" + StringUtils.capitalize(testMethod.getName());
        var method = definedClass.method(JMod.NONE, codeModel.VOID, name);
        method.annotate(codeModel.ref(Test.class));
        method._throws(Exception.class);
        return method.body();
    }

    private void addVerify(TestMethod testMethod, JBlock body) {
        var voidMethodCalls = testMethod.getMethodCalls().stream()
                .filter(methodCall -> Objects.isNull(methodCall.getReturnType()))
                .collect(toList());
        addVoidMethodVerifies(body, voidMethodCalls);
    }

    private void addWhen(TestMethod testMethod, JBlock body) {
        var typedMethodCalls = testMethod.getMethodCalls().stream()
                .filter(methodCall -> Objects.nonNull(methodCall.getReturnType()))
                .collect(toList());
        addTypedMethodMocks(body, typedMethodCalls);
    }

    private void addThenAndAssert(TestMethod testMethod, JBlock body) {
        body.directStatement("//then");
        var testeeMethodInvocation = testee.invoke(testMethod.getName());
        testMethod.getParameters().forEach(parameter ->
                testeeMethodInvocation.arg(JExpr.ref(parameter.getVariableName())));
        if (isReturnTypeVoid(testMethod)) {
            var type = codeModel._ref(testMethod.getReturnType());
            var variableName = "result";
            body.decl(type, variableName, testeeMethodInvocation);
            addVariableAssert(body, variableName);
        } else {
            body.add(testeeMethodInvocation);
        }
    }

    private boolean isReturnTypeVoid(TestMethod testMethod) {
        return Objects.nonNull(testMethod.getReturnType()) && testMethod.getReturnType() != Void.TYPE;
    }

    private void addGiven(TestMethod testMethod, JBlock body) {
        if (testMethod.getParameters().isEmpty()) {
            return;
        }
        body.directStatement("//given");
        testMethod.getParameters().forEach(methodParameter -> {
            addVariableDeclaration(body, methodParameter.getVariableName(), methodParameter.getType());
        });

        if (!isReturnTypeVoid(testMethod)) {
            addVariableDeclaration(body, EXPECTED_RESULT_VARIABLE_NAME, testMethod.getReturnType());
        }
    }

    private void addVariableDeclaration(JBlock body, String variableName, Class<?> parameterType) {
        var type = codeModel._ref(parameterType);
        if (type.isPrimitive()) {
            var value = generator.nextObject(parameterType);
            body.decl(type, variableName, JExpr.ref(value.toString()));
        } else {
            body.decl(type, variableName, JExpr._new(type));
        }
    }

    private void addVariableAssert(JBlock body, String variableName) {
        body.directStatement("//assert");
        var assertCall = codeModel.ref(Assertions.class)
                .staticInvoke("assertEquals")
                .arg(JExpr.ref(EXPECTED_RESULT_VARIABLE_NAME))
                .arg(JExpr.ref(variableName));
        body.add(assertCall);
    }

    private String getSimpleVariableName(Class<?> returnType) {
        return StringUtils.uncapitalize(returnType.getSimpleName());
    }

    private void addTypedMethodMocks(JBlock body, java.util.List<MethodCall> typedMethodCalls) {
        if (typedMethodCalls.isEmpty()) {
            return;
        }
        body.directStatement("//when");
        typedMethodCalls.forEach(mockCall -> addTypedMethodMock(body, mockCall));
    }

    private void addVoidMethodVerifies(JBlock body, List<MethodCall> voidMethodCalls) {
        if (voidMethodCalls.isEmpty()) {
            return;
        }
        body.directStatement("//verify");
        voidMethodCalls.forEach(mockCall -> addVoidMethodVerify(body, mockCall));
    }

    private void addVoidMethodVerify(JBlock body, MethodCall mockCall) {
        var arg = codeModel.ref(Mockito.class)
                .staticInvoke("verify").arg(JExpr.ref(mockCall.getMockName())).invoke(mockCall.getMethod());
        body.add(arg);
    }

    private void addTypedMethodMock(JBlock body, MethodCall mockCall) {
        var arg = codeModel.ref(Mockito.class)
                .staticInvoke("mock").arg(JExpr.ref(mockCall.getMockName()).invoke(mockCall.getMethod()))
                .invoke("thenReturn").arg("some value to return");
        body.add(arg);
    }

    public void addTesteeField(Class<?> testeeClass) {
        testee = definedClass.field(JMod.NONE, testeeClass, getSimpleVariableName(testeeClass));
        testee.annotate(InjectMocks.class);
    }

    public String build() {
        var builtClass = convertToString();
        //beautify
        return JAVA_PARSER.parse(builtClass).getResult().get().toString();
    }

    @SneakyThrows
    private JDefinedClass buildDefinedClass(String name, String packageName) {
        var jp = codeModel._package(packageName);
        var cls = jp._class(JMod.NONE, StringUtils.capitalize(name) + "Test");
        var extendsWithAnnotation = cls.annotate(ExtendWith.class);
        extendsWithAnnotation.param("value", codeModel.ref(MockitoExtension.class));
        return cls;
    }

    @SneakyThrows
    private String convertToString() {
        try (var os = new ByteArrayOutputStream()) {
            codeModel.build(new StringCodeWriter(os));
            return os.toString();
        }
    }

    private EasyRandom createRandomDataGenerator() {
        final EasyRandom generator;
        EasyRandomParameters parameters = new EasyRandomParameters();
        generator = new EasyRandom(parameters);
        return generator;
    }
}
