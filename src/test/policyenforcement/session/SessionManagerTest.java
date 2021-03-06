package policyenforcement.session;

import extension.B64Helper;
import extension.crypto.Cipher;
import extension.crypto.KeyGenerator;
import extension.HashHelper;
import extension.RandomDataGenerator;
import io.ebean.Model;
import models.User;
import org.joda.time.DateTimeUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.test.Helpers;

import java.security.SecureRandom;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class SessionManagerTest {
    public static Application app;
    public static SessionManager sessionManager;
    public static User robin;
    public static HashHelper hashHelper;

    public static String defaultIp = "1.2.23.4";
    public static String otherIp = "2.2.2.2";

    public static byte[] credentialKey = new byte[]{1,2,3,4,5,1};

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);

        hashHelper = new HashHelper();
        sessionManager = new SessionManager(
            new KeyGenerator(),
            new Cipher(),
            new RandomDataGenerator(new SecureRandom()),
            new B64Helper()
        );

        robin = new User("robin", "hsh.helper+robin@gmail.com", hashHelper.hashPassword("robin"), true, 10l);
        robin.save();
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Before
    public void setup() {
        InternalSession.finder.all().forEach(Model::delete);
        setIp(defaultIp);
    }

    private void setIp(String ip) {
        Http.Context.current.set(getContext(ip));
    }

    private Http.Context getContext(String ip) {
        Http.Request request = Helpers.fakeRequest("GET", "/").remoteAddress(ip).build();
        return Helpers.httpContext(request);
    }

    private void simulateNewRequest() {
        Http.Context.current().args.clear();
    }

    @Test
    public void sessionLifespan() {
        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );

        sessionManager.startNewSession(robin, credentialKey);

        assertThat(
            sessionManager.hasActiveSession(),
            is(true)
        );

        assertThat(
            sessionManager.currentUser(),
            is(robin)
        );

        assertArrayEquals(
            sessionManager.getCredentialKey(),
            credentialKey
        );

        simulateNewRequest();

        sessionManager.destroyCurrentSession();

        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );
    }

    @Test
    public void sessionLifespanIpChange() {
        sessionManager.startNewSession(robin, credentialKey);

        assertThat(
            sessionManager.hasActiveSession(),
            is(true)
        );

        String currentSessionKey = Http.Context.current().session().get("HsHSession");
        setIp(otherIp);
        Http.Context.current().session().put("HsHSession", currentSessionKey);

        simulateNewRequest();

        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );
    }

    @Test
    public void sessionLifespanAfterTimeout() {
        sessionManager.startNewSession(robin, credentialKey);

        assertThat(
                sessionManager.hasActiveSession(),
                is(true)
        );

        DateTimeUtils.setCurrentMillisFixed(
                DateTimeUtils.currentTimeMillis()
                + (long)robin.getSessionTimeoutInMinutes()*60L*1000L
                + 1000L // gurantee that we are *beyond* the timeout
        );

        simulateNewRequest();

        assertThat(
                sessionManager.hasActiveSession(),
                is(false)
        );
    }

    @Test
    public void sessionLifespanBeforeTimeout() {
        sessionManager.startNewSession(robin, credentialKey);

        assertThat(
                sessionManager.hasActiveSession(),
                is(true)
        );

        DateTimeUtils.setCurrentMillisFixed(
                DateTimeUtils.currentTimeMillis()
                        + (long) robin.getSessionTimeoutInMinutes()*60L*1000L
                        - 1000L // gurantee that we are just *before* the timeout
        );

        simulateNewRequest();

        assertThat(
                sessionManager.hasActiveSession(),
                is(true)
        );
    }

    @Test
    public void sessionListSingleSession() {
        sessionManager.startNewSession(robin, credentialKey);

        List<Session> sessions = sessionManager.activeSessionsByUser(robin);
        assertThat(sessions.size(), is(1));

        assertThat(
                sessionManager.hasActiveSession(),
                is(true)
        );

        sessions.get(0).destroy();

        simulateNewRequest();

        assertThat(
                sessionManager.hasActiveSession(),
                is(false)
        );
    }

    @Test
    public void sessionListQuadroSession() {
        Http.Context[] requests = new Http.Context[]{
            getContext(defaultIp),
            getContext(defaultIp),
            getContext(defaultIp),
            getContext(defaultIp)
        };

        // Für jeden "Request" eine Session initiieren
        for(Http.Context ctx : requests) {
            Http.Context.current.set(ctx);
            sessionManager.startNewSession(robin, credentialKey);
        }

        // Prüfen, dass auch bei jedem Request die Session besteht
        for(Http.Context ctx : requests) {
            Http.Context.current.set(ctx);
            assertThat(
                sessionManager.hasActiveSession(),
                is(true)
            );
        }

        // Prüfen ob auch so viele Sessions existieren, wie wir angelegt haben
        List<Session> sessions = sessionManager.activeSessionsByUser(robin);
        assertThat(sessions.size(), is(requests.length));

        // Eine der sessions löschen!
        int deleteIndex = 2;
        sessions.get(deleteIndex).destroy();
        for(int i=0; i<sessions.size(); i++) {
            Http.Context.current.set(requests[i]);
            simulateNewRequest();
            assertThat(
                    sessionManager.hasActiveSession(),
                    is(i!=deleteIndex)
            );
        }
    }

    @Test
    public void getCredentialKey() {
        assertThat(
                sessionManager.hasActiveSession(),
                is(false)
        );

        sessionManager.startNewSession(robin, credentialKey);

        assertThat(
                sessionManager.hasActiveSession(),
                is(true)
        );

        assertThat(
                sessionManager.currentUser(),
                is(robin)
        );

        assertArrayEquals(
                sessionManager.getCredentialKey(),
                credentialKey
        );

    }
}