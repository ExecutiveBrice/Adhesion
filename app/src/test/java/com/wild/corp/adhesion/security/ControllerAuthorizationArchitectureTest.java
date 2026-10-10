package com.wild.corp.adhesion.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Prevents a new controller route from silently inheriting an overly broad access rule. */
class ControllerAuthorizationArchitectureTest {

    @Test
    void everyControllerEndpointIsPubliclyDocumentedOrProtectedByMethodSecurity() throws ClassNotFoundException {
        List<String> unsecuredEndpoints = new ArrayList<>();
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var controllers = scanner.findCandidateComponents("com.wild.corp.adhesion");
        assertThat(controllers).as("Le contrôle doit découvrir les contrôleurs de l'application").isNotEmpty();
        assertThat(controllers).as("Les contrôleurs boutique doivent aussi être contrôlés")
                .anyMatch(bean -> bean.getBeanClassName().startsWith("com.wild.corp.adhesion.shop.api."));
        for (var bean : controllers) {
            inspectController(Class.forName(bean.getBeanClassName()), unsecuredEndpoints);
        }

        assertThat(unsecuredEndpoints)
                .as("Chaque endpoint doit être annoté @PreAuthorize ou présent dans PublicApiEndpoints")
                .isEmpty();
    }

    @Test
    void publicEndpointInventoryHasUniqueKeysAndExplanations() {
        Set<String> keys = PublicApiEndpoints.ENDPOINTS.stream()
                .map(endpoint -> endpoint.method() + " " + endpoint.path()).collect(Collectors.toSet());
        assertThat(keys).hasSize(PublicApiEndpoints.ENDPOINTS.size());
        assertThat(PublicApiEndpoints.ENDPOINTS).allSatisfy(endpoint -> {
            assertThat(endpoint.path()).startsWith("/");
            assertThat(endpoint.purpose()).isNotBlank();
        });
    }

    private void inspectController(Class<?> controller, List<String> unsecuredEndpoints) {
        if (!AnnotatedElementUtils.hasAnnotation(controller, RestController.class)) {
            return;
        }
        boolean protectedAtClassLevel = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
        List<String> classPaths = paths(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
        for (Method method : controller.getMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) {
                continue;
            }
            boolean protectedByMethodSecurity = protectedAtClassLevel
                    || AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class);
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
        // An unrestricted @RequestMapping accepts every verb, not zero verbs.
        if (mapping.method().length == 0) {
            return Arrays.stream(RequestMethodMapper.values()).map(value -> value.httpMethod).toList();
        }
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
