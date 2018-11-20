package views;

import managers.mainmanager.MainManager;
import policyenforcement.Policy;

import javax.inject.Inject;

public class TemplateEnvironment {
    @Inject
    static Policy policy;

    @Inject
    static MainManager mainManager;

    public static Policy GetPolicy() {
        return policy;
    }

    public static MainManager GetMainManager() {
        return mainManager;
    }
}
