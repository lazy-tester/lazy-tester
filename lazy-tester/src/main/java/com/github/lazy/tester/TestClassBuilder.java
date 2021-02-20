package com.github.lazy.tester;

import com.github.lazy.tester.model.MockCall;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMod;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class TestClassBuilder {
    private final JCodeModel codeModel;
    private final JDefinedClass definedClass;

    public TestClassBuilder(String name, String packageName) {
        codeModel = new JCodeModel();
        definedClass = buildDefinedClass(name, packageName);
    }

    public void addMockField(Class<?> mockClass, String fieldName) {
        var mock = definedClass.field(JMod.NONE, codeModel.ref(mockClass), fieldName);
        mock.annotate(Mock.class);
    }

    public void addTestMethod(String name, List<MockCall> statements) {
        var testMethod = definedClass.method(JMod.NONE, codeModel.VOID, name);
        testMethod.annotate(codeModel.ref(Test.class));
        testMethod._throws(Exception.class);
        var body = testMethod.body();

        statements.forEach(mockCall -> {
            var arg = codeModel.ref(Mockito.class)
                    .staticInvoke("mock").arg(JExpr.ref(mockCall.getMockName()).invoke(mockCall.getMethod()))
                    .invoke("thenReturn").arg("some value to return");
            body.add(arg);
        });
    }

    public void addTesteeField(Class<?> testeeClass, String testeeFieldName) {
        var testee = definedClass.field(JMod.NONE, testeeClass, testeeFieldName);
        testee.annotate(InjectMocks.class);
    }

    public String build() {
        return convertToString();
    }

    @SneakyThrows
    private JDefinedClass buildDefinedClass(String name, String packageName) {
        var jp = codeModel._package(packageName);
        var cls = jp._class(JMod.NONE, name);
        var extendsWithAnnotation = cls.annotate(ExtendWith.class);
        extendsWithAnnotation.param("value", codeModel.ref(MockitoExtension.class));
        return cls;
    }

    @SneakyThrows
    private String convertToString() {
        try (var os = new ByteArrayOutputStream()) {
            codeModel.build(new SingleStreamCodeWriter(os));
            return os.toString();
        }
    }
}
