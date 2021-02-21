package com.github.lazy.tester.builder;

import com.github.lazy.tester.model.MethodCall;
import com.github.lazy.tester.model.TestMethod;
import com.sun.codemodel.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
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
    private final JCodeModel codeModel;
    private final JDefinedClass definedClass;

    private JFieldVar testee;

    public TestClassBuilder(Class<?> testeeClass) {
        codeModel = new JCodeModel();
        definedClass = buildDefinedClass(testeeClass.getSimpleName(), testeeClass.getPackageName());
    }

    public void addMockField(Class<?> mockClass) {
        var mock = definedClass.field(JMod.NONE, codeModel.ref(mockClass), StringUtils.uncapitalize(mockClass.getSimpleName()));
        mock.annotate(Mock.class);
    }

    public void addTestMethod(TestMethod testMethod) {
        var name = "should" + StringUtils.capitalize(testMethod.getName());
        var method = definedClass.method(JMod.NONE, codeModel.VOID, name);
        method.annotate(codeModel.ref(Test.class));
        method._throws(Exception.class);
        var body = method.body();

        var typedMethodCalls = testMethod.getMethodCalls().stream()
                .filter(methodCall -> Objects.nonNull(methodCall.getReturnType()))
                .collect(toList());
        addTypedMethodMocks(body, typedMethodCalls);

        body.directStatement("//then");
        body.add(testee.invoke(testMethod.getName()));

        var voidMethodCalls = testMethod.getMethodCalls().stream()
                .filter(methodCall -> Objects.isNull(methodCall.getReturnType()))
                .collect(toList());
        addVoidMethodVerifies(body, voidMethodCalls);
    }

    private void addTypedMethodMocks(JBlock body, java.util.List<com.github.lazy.tester.model.MethodCall> typedMethodCalls) {
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
        testee = definedClass.field(JMod.NONE, testeeClass, StringUtils.uncapitalize(testeeClass.getSimpleName()));
        testee.annotate(InjectMocks.class);
    }

    public String build() {
        return convertToString();
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
}
