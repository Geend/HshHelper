package views;

import policyenforcement.Policy;

import javax.inject.Inject;

public class TemplateEnvironment {
    @Inject
    static Policy policy;

    public static Policy GetPolicy() {
        return policy;
    }
}
