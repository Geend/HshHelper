package views;

import extension.RecaptchaHelper;
import managers.mainmanager.MainManager;
import policyenforcement.Policy;

import javax.inject.Inject;

public class TemplateEnvironment {
    @Inject
    static Policy policy;

    @Inject
    static MainManager mainManager;

    @Inject
    static RecaptchaHelper recaptchaHelper;

    public static Policy GetPolicy() {
        return policy;
    }

    public static MainManager GetMainManager() {
        return mainManager;
    }

    public static RecaptchaHelper getRecaptchaHelper() {
        return recaptchaHelper;
    }
}
