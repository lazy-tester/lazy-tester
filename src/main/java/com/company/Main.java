package com.company;

import com.company.demo.BestEverService;
import com.sun.codemodel.*;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.stream.Stream;

public class Main {


    public static void main(String[] args) throws Exception {
        var testeeClass = BestEverService.class;

        var cm = new JCodeModel();
        var cls = generateTestClass(testeeClass, cm);
        generateMockFields(testeeClass, cm, cls);
        generateTesteeField(testeeClass, cm, cls);
        generateTestMethods(testeeClass, cm, cls);

        cm.build(new SingleStreamCodeWriter(System.out));
    }

    private static JDefinedClass generateTestClass(Class<?> testeeClass, JCodeModel cm) throws JClassAlreadyExistsException {
        var jp = cm._package(testeeClass.getPackage().getName());
        var cls = jp._class(JMod.NONE,testeeClass.getSimpleName() + "Test");
        var extendsWithAnnotation = cls.annotate(ExtendWith.class);
        extendsWithAnnotation.param("value", cm.ref(MockitoExtension.class));
        return cls;
    }

    private static void generateTestMethods(Class<?> testeeClass, JCodeModel cm, JDefinedClass cls) {
        Stream.of(testeeClass.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .forEach(method -> {
                    var methodName = method.getName();
                    var testMethod = cls.method(JMod.NONE, cm.VOID, "should" + StringUtils.capitalize(methodName));
                    testMethod.annotate(cm.ref(Test.class));
                    testMethod._throws(Exception.class);
                });
    }

    private static void generateTesteeField(Class<?> testeeClass, JCodeModel cm, JDefinedClass cls) {
        var testee = cls.field(JMod.NONE, cm.DOUBLE, StringUtils.uncapitalize(testeeClass.getSimpleName()));
        testee.annotate(InjectMocks.class);
    }

    private static void generateMockFields(Class<?> testeeClass, JCodeModel cm, JDefinedClass cls) {
        for (Field field : testeeClass.getDeclaredFields()) {
            var fieldClass = field.getType();
            System.out.println(StringUtils.uncapitalize(fieldClass.getSimpleName()));
            var mock1 = cls.field(JMod.NONE, cm.ref(fieldClass), StringUtils.uncapitalize(fieldClass.getSimpleName()));
            mock1.annotate(Mock.class);
        }
    }
}
