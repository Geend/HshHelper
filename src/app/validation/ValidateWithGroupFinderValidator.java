package validation;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import models.Group;
import models.finders.GroupFinder;
import play.data.validation.Constraints.PlayConstraintValidator;

public class ValidateWithGroupFinderValidator implements PlayConstraintValidator<ValidateWithGroupFinder, ValidatableWithFinder<?, Group>> {

    private final GroupFinder finder;

    @Inject
    public ValidateWithGroupFinderValidator(final GroupFinder finder) {
        this.finder = finder;
    }

    @Override
    public void initialize(final ValidateWithGroupFinder constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithFinder<?, Group> value, final ConstraintValidatorContext constraintValidatorContext) {
        return reportValidationStatus(value.validate(this.finder), constraintValidatorContext);
    }
}