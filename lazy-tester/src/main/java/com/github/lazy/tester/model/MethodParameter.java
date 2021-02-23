package com.github.lazy.tester.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class MethodParameter {
    Class<?> type;
    String variableName;
}
