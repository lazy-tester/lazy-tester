package com.github.lazy.tester.demo;

import com.github.lazy.tester.TestGenerator;
import org.junit.jupiter.api.Test;

class BestEverServiceTest {

    @Test
    public static void main(String[] args) {
        var generatedTestClass = TestGenerator.generate(BestEverService.class);
        System.out.println(generatedTestClass);
    }

}