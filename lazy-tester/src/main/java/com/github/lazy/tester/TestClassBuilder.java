package com.github.lazy.tester;

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
import java.io.OutputStream;

public class TestClassBuilder {
    private final JCodeModel codeModel;
    private final JDefinedClass definedClass;

    private JFieldVar testee;

    public TestClassBuilder(Class<?> testeeClass) {
        codeModel = new JCodeModel();
        definedClass = buildDefinedClass(testeeClass.getSimpleName(), testeeClass.getPackageName());
    }

    public void addMockField(Class<?> mockClass, String fieldName) {
        var mock = definedClass.field(JMod.NONE, codeModel.ref(mockClass), fieldName);
        mock.annotate(Mock.class);
    }

    public void addTestMethod(TestMethod testMethod) {
        var name = "should" + StringUtils.capitalize(testMethod.getName());
        var method = definedClass.method(JMod.NONE, codeModel.VOID, name);
        method.annotate(codeModel.ref(Test.class));
        method._throws(Exception.class);
        var body = method.body();


        var methodCalls = testMethod.getMethodCalls();
        if (!methodCalls.isEmpty()) {
            body.directStatement("//when");
        }
        methodCalls.forEach(mockCall -> {
            var arg = codeModel.ref(Mockito.class)
                    .staticInvoke("mock").arg(JExpr.ref(mockCall.getMockName()).invoke(mockCall.getMethod()))
                    .invoke("thenReturn").arg("some value to return");
            body.add(arg);
        });

        body.directStatement("//then");
        body.add(testee.invoke(testMethod.getName()));
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
            CodeWriter codeWriter = new CodeWriter() {
                @Override
                public OutputStream openBinary(JPackage jPackage, String name) {
                    return os;
                }

                @Override
                public void close() {
                }
            };
            codeModel.build(codeWriter);
            return os.toString();
        }
    }
}
