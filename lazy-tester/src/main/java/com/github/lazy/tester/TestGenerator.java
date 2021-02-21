package com.github.lazy.tester;

import com.github.lazy.tester.builder.TestClassBuilder;
import com.github.lazy.tester.model.TestMethod;
import com.github.lazy.tester.parser.ClassParser;
import lombok.SneakyThrows;

public class TestGenerator {

    private final ClassParser classParser;
    private final Class<?> testeeClass;
    private final TestClassBuilder testClassBuilder;

    private TestGenerator(Class<?> testeeClass) {
        this.testeeClass = testeeClass;
        classParser = new ClassParser(testeeClass);
        this.testClassBuilder = new TestClassBuilder(testeeClass);
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
                .map(classParser::extractTestMethod)
                .forEach(this::generateTestMethod);
    }

    private void generateTestMethod(TestMethod method) {
        testClassBuilder.addTestMethod(method);
    }

    private void generateTesteeField() {
        testClassBuilder.addTesteeField(testeeClass);
    }

    private void generateMockFields() {
        classParser.getDeclaredFields().forEach(field ->
                testClassBuilder.addMockField(field.getType()));
    }
}
