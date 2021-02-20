package com.github.lazy.tester.model;

import lombok.Value;

import java.util.List;

@Value
public class TestMethod {
    String name;
    List<MockCall> mockCalls;
}
