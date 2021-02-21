package com.github.lazy.tester.builder;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class StringCodeWriter extends CodeWriter {

    private final ByteArrayOutputStream os;

    @Override
    public OutputStream openBinary(JPackage jPackage, String name) {
        return os;
    }

    @Override
    public void close() {
    }
}
