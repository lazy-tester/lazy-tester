package com.github.lazy.tester;

import com.github.lazy.tester.model.MockCall;
import com.github.lazy.tester.model.TestMethod;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

public class TestGenerator {

    private final ClassParser classParser;
    private final Class<?> testeeClass;
    private final TestClassBuilder testClassBuilder;

    private TestGenerator(Class<?> testeeClass) {
        this.testeeClass = testeeClass;
        classParser = new ClassParser(testeeClass);
        this.testClassBuilder = new TestClassBuilder(testeeClass.getSimpleName() + "Test", testeeClass.getPackage().getName());
    }

    public static String generate(Class<?> testeeClass) {
        return new TestGenerator(testeeClass).generateTestClass();
    }

    @SneakyThrows
    private String generateTestClass() {
        generateMockFields();
        generateTesteeField();
        generateTestMethods();
        return testClassBuilder.build();
    }

    private void generateTestMethods() {
        classParser.getDeclaredPublicMethods().stream()
                .map(classParser::convertToTestMethod)
                .forEach(this::generateTestMethod);
    }

    private void generateTestMethod(TestMethod method) {
        var mockCalls = method.getMockCalls();
        testClassBuilder.addTestMethod("should" + StringUtils.capitalize(method.getName()), mockCalls);
    }

    private String convertToPlainStatement(MockCall mockCall) {
        return mockCall.getMockName() + "." + mockCall.getMethod() + "()";
    }

    private void generateTesteeField() {
        testClassBuilder.addTesteeField(testeeClass, StringUtils.uncapitalize(testeeClass.getSimpleName()));
    }

    private void generateMockFields() {
        for (Field field : testeeClass.getDeclaredFields()) {
            testClassBuilder.addMockField(field.getType(), StringUtils.uncapitalize(field.getType().getSimpleName()));
        }
    }
}
