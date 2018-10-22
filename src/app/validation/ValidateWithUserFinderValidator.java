package validation;

import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import models.User;
import models.finders.UserFinder;
import play.data.validation.Constraints.PlayConstraintValidator;

public class ValidateWithUserFinderValidator implements PlayConstraintValidator<ValidateWithUserFinder, ValidatableWithFinder<?, User>> {

    private final UserFinder finder;

    @Inject
    public ValidateWithUserFinderValidator(final UserFinder finder) {
        this.finder = finder;
    }

    @Override
    public void initialize(final ValidateWithUserFinder constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ValidatableWithFinder<?, User> value, final ConstraintValidatorContext constraintValidatorContext) {
        return reportValidationStatus(value.validate(this.finder), constraintValidatorContext);
    }
}