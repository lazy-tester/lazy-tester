package com.github.lazy.tester.demo;

import com.github.javaparser.JavaParser;
import com.github.lazy.tester.TestGenerator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TestGeneratorTest {

    @Test
    public void shouldGenerateTestClass() {
        var generatedTestClass = TestGenerator.generate(BestEverService.class);
        assertClassEqualToFileContent(generatedTestClass, "best-service-test-class.txt");
    }

    private void assertClassEqualToFileContent(String generatedTestClass, String fileName) {
        var javaParser = new JavaParser();
        var actualClass = javaParser.parse(generatedTestClass);
        var expectedClass = javaParser.parse(readFile(fileName));
        Assertions.assertEquals(1, 3);
    }

    @SneakyThrows
    public String readFile(String fileName) {
        var path = getResourcePath(fileName);
        return new String(Files.readAllBytes(path));
    }

    private Path getResourcePath(String fileName) throws URISyntaxException {
        var resource = TestGeneratorTest.class.getClassLoader().getResource(fileName);
        return Paths.get(resource.toURI());
    }

}