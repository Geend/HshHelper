package views;

import extension.RecaptchaHelper;
import managers.mainmanager.MainManager;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;

public class TemplateEnvironment {
    @Inject
    static SessionManager sessionManager;

    @Inject
    static MainManager mainManager;

    @Inject
    static RecaptchaHelper recaptchaHelper;

    public static Policy policy() {
        return sessionManager.currentPolicy();
    }

    public static MainManager GetMainManager() {
        return mainManager;
    }

    public static RecaptchaHelper getRecaptchaHelper() {
        return recaptchaHelper;
    }
}
