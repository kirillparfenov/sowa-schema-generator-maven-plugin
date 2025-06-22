package dev.parfenov.sowa.schema.plugin.config;

import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;

import java.util.Objects;

public class CustomJakartaValidationModule extends JakartaValidationModule {

    @Override
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)){
            var maxLength = super.resolveStringMaxLength(member);
            return Objects.isNull(maxLength) ? 300 : maxLength;
        }
        return null;
    }

    @Override
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (member.isContainerType()) {
            var maxItems = super.resolveArrayMaxItems(member);
            return Objects.isNull(maxItems) ? 1000 : maxItems;
        }
        return null;
    }
}
