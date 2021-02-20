package com.github.lazy.tester.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MethodCall {
    String mockName;
    String method;
}
