package validation;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import io.ebean.Finder;
import models.Group;
import models.finders.GroupFinder;
import play.data.validation.Constraints.PlayConstraintValidator;

public class ValidateWithGroupFinder implements PlayConstraintValidator<ValidateWithFinder, ValidatableWithFinder<?, Group>> {

    private final GroupFinder finder;

    @Inject
    public ValidateWithGroupFinder(final GroupFinder finder) {
        this.finder = finder;
    }

    @Override
    public void initialize(final ValidateWithFinder constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithFinder<?, Group> value, final ConstraintValidatorContext constraintValidatorContext) {
        return reportValidationStatus(value.validate(this.finder), constraintValidatorContext);
    }
}