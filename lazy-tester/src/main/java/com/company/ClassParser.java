package com.company;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassParser {

    private final CompilationUnit compilationUnit;

    private Map<String, MethodDeclaration> declaredMethods;

    public ClassParser(Class<?> testeeClass) {
        this.compilationUnit = getCompilationUnit(testeeClass);
    }

    private CompilationUnit getCompilationUnit(Class<?> clazz) {
        var classPath = CodeGenerationUtils.mavenModuleRoot(clazz).resolve("src/main/java/" + clazz.getPackageName().replaceAll("\\.", "/"));
        return new SourceRoot(classPath).parse("", clazz.getSimpleName() + ".java");
    }

    public Set<String> getDeclaredPublicMethods() {
        return getDeclaredMethods().values().stream()
                .filter(NodeWithPublicModifier::isPublic)
                .map(methodDeclaration -> methodDeclaration.getName().getId())
                .collect(Collectors.toSet());
    }

    private Map<String, MethodDeclaration> getDeclaredMethods() {
        if (declaredMethods == null) {
            declaredMethods = extractMethods();
        }
        return declaredMethods;
    }

    private Map<String, MethodDeclaration> extractMethods() {
        var methods = new HashMap<String, MethodDeclaration>();
        compilationUnit.accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(final MethodDeclaration n, final Object arg) {
                methods.put(n.getName().getId(), n);
            }
        }, null);

//        var methodCalls = new ArrayList<MethodCallExpr>();
//
//        method.accept(new VoidVisitorAdapter<>() {
//            @Override
//            public void visit(final MethodCallExpr n, final Object arg) {
//                methodCalls.add(n);
//            }
//        }, null);
//
//        methodCalls.stream()
//                .filter(methodCallExpr -> methodCallExpr.getScope().isPresent())
//                .map(methodCallExpr -> methodCallExpr.getScope().get())
//                .forEach(methodCallExpr -> {
//                    System.out.println(methodCallExpr);
//                });

        return methods;
    }

}
