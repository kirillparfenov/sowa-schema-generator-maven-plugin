package dev.parfenov.sowa.schema.plugin.generator;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;

public class CustomJacksonModule extends JacksonModule {

    static final Predicate<Annotation> NESTED_ANNOTATION_CHECK = annotation ->
            annotation.annotationType().isAnnotationPresent(JacksonAnnotationsInside.class);

    private final ObjectMapper objectMapper;

    public CustomJacksonModule(final ObjectMapper objectMapper) {
        super(
                JacksonOption.ALWAYS_REF_SUBTYPES,
                JacksonOption.RESPECT_JSONPROPERTY_ORDER
        );
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldIgnoreField(FieldScope field) {
        if (field.getAnnotationConsideringFieldAndGetterIfSupported(JsonBackReference.class, NESTED_ANNOTATION_CHECK) != null) {
            return true;
        }
        // @since 4.32.0
        var unwrappedAnnotation = field.getAnnotationConsideringFieldAndGetterIfSupported(JsonUnwrapped.class, NESTED_ANNOTATION_CHECK);
        if (unwrappedAnnotation != null && unwrappedAnnotation.enabled()) {
            // unwrapped properties should be ignored here, as they are included in their unwrapped form
            return true;
        }
        // instead of re-creating the various ways a property may be included/excluded in jackson: just use its built-in introspection
        var topMostHierarchyType = field.getDeclaringTypeMembers().allTypesAndOverrides().get(0);
        var beanDescription = this.getBeanDescriptionForClass(topMostHierarchyType.getType());
        // some kinds of field ignorals are only available via an annotation introspector
        var ignoredProperties = this.objectMapper.getSerializationConfig().getAnnotationIntrospector()
                .findPropertyIgnoralByName(null, beanDescription.getClassInfo()).getIgnored();
        var declaredName = field.getDeclaredName();
        if (ignoredProperties.contains(declaredName)) {
            return true;
        }
        // @since 4.37.0 also consider overridden property name as it may match the getter method
        var fieldName = field.getName();
        // other kinds of field ignorals are handled implicitly, i.e. are only available by way of being absent
        return beanDescription.findProperties().stream()
                .noneMatch(propertyDefinition ->
                        declaredName.equals(propertyDefinition.getInternalName())
                                || fieldName.equals(propertyDefinition.getInternalName())
                                || fieldName.equals(
                                Optional.ofNullable(propertyDefinition.getGetter())
                                        .map(AnnotatedMethod::getName)
                                        .orElse(null)
                        ) //для полей boolean is...
                );
    }
}
