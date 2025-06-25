package dev.parfenov.sowa.schema.plugin.generator;

import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

import java.util.Objects;
import java.util.UUID;

//todo можно вынести в отдельные модули
public class CustomJakartaValidationModule extends JakartaValidationModule {

    public CustomJakartaValidationModule(JakartaValidationOption... options) {
        super(options);
    }

    @Override
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)){
            var maxLength = super.resolveStringMaxLength(member);
            return Objects.isNull(maxLength) ? 300 : maxLength;
        }
        return null;
    }

    @Override
    protected String resolveStringPattern(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(UUID.class)) {
            return "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        }
        return super.resolveStringPattern(member);
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
