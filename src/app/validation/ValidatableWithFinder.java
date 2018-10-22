package validation;

import io.ebean.Finder;

public interface ValidatableWithFinder<T, Model> {
    public T validate(final Finder<Long, Model> finder);
}