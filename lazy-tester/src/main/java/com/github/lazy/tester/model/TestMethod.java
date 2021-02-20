package com.github.lazy.tester.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TestMethod {
    String name;
    List<MethodCall> methodCalls;
}
