package dev.parfenov.sowa.schema.plugin.generators.config;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class EnumMapDefinitionProvider implements CustomDefinitionProviderV2 {

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType targetType, SchemaGenerationContext context) {
        var key = context.getTypeContext().getTypeParameterFor(targetType, Map.class, 0);
        if (key == null || !key.isInstanceOf(Enum.class)) {
            return null;
        }
        var value = Optional
                .ofNullable(context.getTypeContext().getTypeParameterFor(targetType, Map.class, 1))
                .orElseGet(() -> context.getTypeContext().resolve(Object.class));

        var customSchema = context.getGeneratorConfig().createObjectNode();
        var propertiesNode = context.getGeneratorConfig().createObjectNode();
        customSchema.set(context.getKeyword(SchemaKeyword.TAG_PROPERTIES), propertiesNode);

        Stream.of(((Class<? extends Enum<?>>) key.getErasedType()).getEnumConstants())
                .map(Enum::name)
                .forEach(propertyName ->
                        propertiesNode.set(
                                propertyName,
                                context.createStandardDefinitionReference(value, this)
                        ));
        return new CustomDefinition(customSchema);
    }
}
