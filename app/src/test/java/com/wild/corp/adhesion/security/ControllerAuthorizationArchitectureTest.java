package com.wild.corp.adhesion.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prevents a new controller route from silently inheriting an overly broad access rule. */
class ControllerAuthorizationArchitectureTest {

    private static final String CONTROLLER_PACKAGE = "com.wild.corp.adhesion.controllers.";
    private static final Path CONTROLLER_SOURCES = Path.of("src/main/java/com/wild/corp/adhesion/controllers");

    @Test
    void everyControllerEndpointIsPubliclyDocumentedOrProtectedByMethodSecurity() throws IOException {
        List<String> unsecuredEndpoints = new ArrayList<>();

        try (var files = Files.list(CONTROLLER_SOURCES)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".java")).toList()) {
                inspectController(Class.forName(CONTROLLER_PACKAGE + sourceClassName(file)), unsecuredEndpoints);
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Impossible de charger un contrôleur pour le test d'architecture", exception);
        }

        assertThat(unsecuredEndpoints)
                .as("Chaque endpoint doit être annoté @PreAuthorize ou présent dans PublicApiEndpoints")
                .isEmpty();
    }

    private void inspectController(Class<?> controller, List<String> unsecuredEndpoints) {
        if (!AnnotatedElementUtils.hasAnnotation(controller, RestController.class)) {
            return;
        }
        List<String> classPaths = paths(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
        for (Method method : controller.getDeclaredMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) {
                continue;
            }
            boolean protectedByMethodSecurity = AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class);
            for (HttpMethod httpMethod : httpMethods(mapping)) {
                for (String classPath : classPaths) {
                    for (String methodPath : paths(mapping)) {
                        String endpoint = join(classPath, methodPath);
                        if (!protectedByMethodSecurity && !PublicApiEndpoints.isPublic(httpMethod, endpoint)) {
                            unsecuredEndpoints.add(httpMethod + " " + endpoint + " -> " + controller.getSimpleName() + "#" + method.getName());
                        }
                    }
                }
            }
        }
    }

    private List<HttpMethod> httpMethods(RequestMapping mapping) {
        return Arrays.stream(mapping.method()).map(RequestMethodMapper::toHttpMethod).toList();
    }

    private List<String> paths(RequestMapping mapping) {
        String[] paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return paths.length == 0 ? List.of("") : Arrays.asList(paths);
    }

    private String join(String base, String child) {
        String path = (base + "/" + child).replaceAll("/{2,}", "/");
        return path.startsWith("/") ? path : "/" + path;
    }

    private String sourceClassName(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".java".length());
    }

    private enum RequestMethodMapper {
        GET(HttpMethod.GET), POST(HttpMethod.POST), PUT(HttpMethod.PUT), DELETE(HttpMethod.DELETE), PATCH(HttpMethod.PATCH),
        HEAD(HttpMethod.HEAD), OPTIONS(HttpMethod.OPTIONS), TRACE(HttpMethod.TRACE);

        private final HttpMethod httpMethod;

        RequestMethodMapper(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
        }

        static HttpMethod toHttpMethod(org.springframework.web.bind.annotation.RequestMethod requestMethod) {
            return valueOf(requestMethod.name()).httpMethod;
        }
    }
}
