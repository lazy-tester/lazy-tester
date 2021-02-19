package com.company;

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
        classParser.getDeclaredPublicMethods()
                .forEach(this::generateTestMethod);
    }

    private void generateTestMethod(String methodName) {
        testClassBuilder.addTestMethod("should" + StringUtils.capitalize(methodName));
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