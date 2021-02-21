package com.github.lazy.tester.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class Type {
    String typeName;
    String variableName;
}
