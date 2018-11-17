package policyenforcement.session;

import play.mvc.With;
import policyenforcement.session.enforcement.AuthenticationNotAllowed;
import policyenforcement.session.enforcement.AuthenticationRequired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Authentication  {
    @With(AuthenticationRequired.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Required {
    }

    @With(AuthenticationNotAllowed.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotAllowed {
    }
}
