package validation;

import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import models.BaseDomain;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;

import static play.libs.F.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

public class HsHConstraints {
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = UniqueValidator.class)
    @Repeatable(Unique.List.class)
    public @interface Unique {
        String message() default UniqueValidator.message;
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        Class<? extends Model> model();
        String[] columns();

        /**
         * Defines several {@code @Unique} annotations on the same element.
         */
        @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
        @Retention(RUNTIME)
        public @interface List {
            Unique[] value();
        }
    }

    public static class UniqueValidator extends play.data.validation.Constraints.Validator<String> implements ConstraintValidator<Unique, String> {

        final static public String message = "error.unique";
        private String[] columns;
        private Class<? extends Model> model;

        public void initialize(Unique constraintAnnotation) {
            this.model = constraintAnnotation.model();
            columns = constraintAnnotation.columns();
        }

        public boolean isValid(String object) {
            Finder<Long, ? extends BaseDomain> find = new Finder(model);
            ExpressionList el = find.query().where();
            for (String f: columns) {
                el.eq(f, object);
            }

            return el.findCount() == 0;
        }

        public Tuple<String, Object[]> getErrorMessageKey() {
            return Tuple(message, new Object[]{});
        }
    }
}

