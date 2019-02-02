package managers.netservicemanager;

import extension.crypto.Cipher;
import extension.crypto.CryptoConstants;
import extension.crypto.CryptoKey;
import extension.crypto.KeyGenerator;
import extension.RandomDataGenerator;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.*;
import models.finders.NetServiceCredentialFinder;
import models.finders.NetServiceFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NetServiceManagerTest {


    @Mock
    NetServiceFinder netServiceFinder;

    @Mock
    NetServiceCredentialFinder netServiceCredentialFinder;

    @Mock
    EbeanServer defaultServer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    SessionManager sessionManager;


    KeyGenerator keyGenerator;

    Cipher cipher;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private NetServiceManager nsm;


    private User admin;
    private User peter;
    private User klaus;

    private Group all;
    private Group admins;
    private Group petersGroup;

    @Before
    public void setup() {
        admin = new User("admin", "hsh.helper+admin@gmail.com", "admin", false, 10l);
        peter = new User("peter", "hsh.helper+peter@gmail.com", "peter", false, 10l);
        klaus = new User("klaus", "hsh.helper+klaus@gmail.com", "klaus", false, 10l);

        all = new Group("All", admin);
        admins = new Group("Administrators", admin);
        petersGroup = new Group("Peter's Group", peter);

        all.setIsAllGroup(true);
        admins.setIsAdminGroup(true);

        all.setMembers(Stream.of(admin, peter, klaus).collect(Collectors.toList()));
        admins.setMembers(Stream.of(admin).collect(Collectors.toList()));
        petersGroup.setMembers(Stream.of(admin, peter).collect(Collectors.toList()));

        admin.setGroups(Stream.of(all, admins, petersGroup).collect(Collectors.toList()));
        peter.setGroups(Stream.of(all, petersGroup).collect(Collectors.toList()));
        klaus.setGroups(Stream.of(all).collect(Collectors.toList()));


        cipher = new Cipher();
        keyGenerator = new KeyGenerator();
        nsm = new NetServiceManager(sessionManager, defaultServer, netServiceFinder, netServiceCredentialFinder, keyGenerator, cipher);
    }


    /*
        createNetService
     */
    @Test
    public void testCreateNetService() throws UnauthorizedException, NetServiceAlreadyExistsException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = nsm.createNetService("testservice", "http://example.org/login");
        verify(defaultServer).save(netService);

        Assert.assertEquals("testservice", netService.getName());
        Assert.assertEquals("http://example.org/login", netService.getUrl());

    }

    @Test
    public void testCreateNetServiceNoAdmin() throws UnauthorizedException, NetServiceAlreadyExistsException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));
        when(sessionManager.currentUser()).thenReturn(peter);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        expected.expect(UnauthorizedException.class);

        NetService netService = nsm.createNetService("testservice", "http://example.org/login");
        verify(defaultServer, never()).save(netService);
    }

    @Test
    public void testDoubleCreateNetService() throws UnauthorizedException, NetServiceAlreadyExistsException {

        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = nsm.createNetService("testservice", "http://example.org/login");

        verify(defaultServer).save(netService);
        when(netServiceFinder.byName("testservice")).thenReturn(Optional.of(netService));

        expected.expect(NetServiceAlreadyExistsException.class);
        nsm.createNetService("testservice", "http://example.org/login");

        verify(defaultServer, times(1)).save(netService);
    }

    /*
        editNetService
     */
    @Test
    public void testEditNetService() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);

        NetService netService = mock(NetService.class);
        when(netService.getNetServiceId()).thenReturn(0L);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));


        nsm.editNetService(0L, "testName", "testUrl");

        verify(netService).setName("testName");
        verify(netService).setUrl("testUrl");
        verify(defaultServer).save(netService);

    }

    @Test
    public void testEditNetServiceNoAdmin() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));
        when(sessionManager.currentUser()).thenReturn(peter);

        NetService netService = mock(NetService.class);
        when(netService.getNetServiceId()).thenReturn(0L);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        expected.expect(UnauthorizedException.class);
        nsm.editNetService(0L, "testName", "testUrl");

        verify(defaultServer, times(0)).save(netService);
    }


    /*
        addNetServiceParameter
     */

    @Test
    public void testAddNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = mock(NetService.class);
        when(netService.getParameters()).thenReturn(mock(List.class));

        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.USERNAME, "username", "");

        ArgumentCaptor<NetServiceParameter> captor = ArgumentCaptor.forClass(NetServiceParameter.class);
        verify(netService.getParameters()).add(captor.capture());

        Assert.assertEquals(NetServiceParameter.NetServiceParameterType.USERNAME, captor.getValue().getParameterType());
        Assert.assertEquals("username", captor.getValue().getName());
        Assert.assertEquals("", captor.getValue().getDefaultValue());

        verify(defaultServer, times(1)).save(netService);
    }

    @Test
    public void testAddNetServiceParameterNoAdmin() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));
        when(sessionManager.currentUser()).thenReturn(peter);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = mock(NetService.class);

        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        expected.expect(UnauthorizedException.class);
        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.USERNAME, "username", "");

        verify(defaultServer, times(0)).save(netService);

    }

    @Test
    public void testAddNetServiceParameterDobuleTypeAdd() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = mock(NetService.class);
        List<NetServiceParameter> parameterList = new ArrayList<>();
        when(netService.getParameters()).thenReturn(parameterList);

        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.USERNAME, "username", "");
        verify(defaultServer, times(1)).save(netService);

        expected.expect(InvalidArgumentException.class);
        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.USERNAME, "username", "guest");
        verify(defaultServer, times(1)).save(netService);
    }

    @Test
    public void testAddNetServiceParameterDobuleNameAdd() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));

        NetService netService = mock(NetService.class);
        List<NetServiceParameter> parameterList = new ArrayList<>();
        when(netService.getParameters()).thenReturn(parameterList);

        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.USERNAME, "username", "");
        verify(defaultServer, times(1)).save(netService);

        expected.expect(InvalidArgumentException.class);
        nsm.addNetServiceParameter(0L, NetServiceParameter.NetServiceParameterType.HIDDEN, "username", "");
        verify(defaultServer, times(1)).save(netService);
    }


    /*
        removeNetServiceParameter
     */

    @Test
    public void testRemoveNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);

        NetService netService = new NetService();
        NetServiceParameter netServiceParameter = mock(NetServiceParameter.class);

        netService.getParameters().add(netServiceParameter);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        nsm.removeNetServiceParameter(0L, 0L);

        verify(defaultServer, times(1)).delete(netServiceParameter);
        verify(defaultServer, times(1)).save(netService);
    }

    @Test
    public void testRemoveNetServiceParameterNoAdmin() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));
        when(sessionManager.currentUser()).thenReturn(peter);

        NetService netService = new NetService();
        NetServiceParameter netServiceParameter = mock(NetServiceParameter.class);

        netService.getParameters().add(netServiceParameter);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        expected.expect(UnauthorizedException.class);
        nsm.removeNetServiceParameter(0L, 0L);

        verify(defaultServer, times(0)).delete(netServiceParameter);
        verify(defaultServer, times(0)).save(netService);
    }

    @Test
    public void testRemoveParameterNotInNetService() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);

        NetService netService = new NetService();
        NetServiceParameter netServiceParameter = mock(NetServiceParameter.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        expected.expect(InvalidArgumentException.class);
        nsm.removeNetServiceParameter(0L, 0L);

        verify(defaultServer, times(0)).delete(netServiceParameter);
        verify(defaultServer, times(0)).save(netService);
    }

    /*
        deleteNetService
     */
    @Test
    public void testDeleteNetService() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);

        NetService netService = mock(NetService.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        nsm.deleteNetService(0L);

        verify(defaultServer, times(1)).delete(netService);
    }

    @Test
    public void testDeleteNetServiceNoAdmin() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));
        when(sessionManager.currentUser()).thenReturn(peter);

        NetService netService = mock(NetService.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        expected.expect(UnauthorizedException.class);
        nsm.deleteNetService(0L);

        verify(defaultServer, times(0)).delete(netService);
    }

    @Test
    public void testDeleteNonExistingNetService() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));
        when(sessionManager.currentUser()).thenReturn(admin);

        expected.expect(InvalidArgumentException.class);
        nsm.deleteNetService(0L);
    }


    /*
        getUserNetServiceCredentials
     */
    @Test
    public void testGetUserNetServiceCredentials() {
        User testUser = mock(User.class);
        when(sessionManager.currentUser()).thenReturn(testUser);

        List<NetServiceCredential> netServiceCredentials = mock(List.class);
        when(testUser.getNetServiceCredentials()).thenReturn(netServiceCredentials);

        Assert.assertEquals(netServiceCredentials, nsm.getUserNetServiceCredentials());
    }

    /*
        createNetUserCredential
     */
    @Test
    public void testCreateNetUserCredential() throws InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);

        NetService netService = mock(NetService.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        byte[] credentialKey = (new RandomDataGenerator(new SecureRandom())).generateBytes(CryptoConstants.GENERATED_KEY_BYTE);
        when(sessionManager.getCredentialKey()).thenReturn(credentialKey);

        nsm.createNetUserCredential(0L, "usr", "pwd");

        ArgumentCaptor<NetServiceCredential> captor = ArgumentCaptor.forClass(NetServiceCredential.class);
        verify(defaultServer, times(1)).save(captor.capture());


        CryptoKey key = keyGenerator.generate(credentialKey);

        Assert.assertEquals(netService, captor.getValue().getNetService());
        Assert.assertArrayEquals("usr".getBytes(), cipher.decrypt(key,
                captor.getValue().getInitializationVectorUsername(),
                captor.getValue().getUsernameCipherText()));
        Assert.assertArrayEquals("pwd".getBytes(), cipher.decrypt(key,
                captor.getValue().getInitializationVectorPassword(),
                captor.getValue().getPasswordCipherText()));
    }

    @Test
    public void testDoubleCreateNetUserCredential() throws InvalidArgumentException {
        NetService netService = mock(NetService.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        byte[] credentialKey = (new RandomDataGenerator(new SecureRandom())).generateBytes(CryptoConstants.GENERATED_KEY_BYTE);
        when(sessionManager.getCredentialKey()).thenReturn(credentialKey);

        nsm.createNetUserCredential(0L, "usr1", "pwd1");

        nsm.createNetUserCredential(0L, "usr2", "pwd2");

        ArgumentCaptor<NetServiceCredential> captor = ArgumentCaptor.forClass(NetServiceCredential.class);

        verify(defaultServer, times(2)).save(captor.capture());
    }


    /*
        deleteNetServiceCredential
     */
    @Test
    public void testDeleteNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));

        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(netServiceCredentialFinder.byIdOptional(0L)).thenReturn(Optional.of(credential));
        when(credential.getUser()).thenReturn(peter);

        nsm.deleteNetServiceCredential(0L);
        verify(defaultServer, times(1)).delete(credential);
    }

    @Test
    public void testDeleteNetServiceCredentialByForeignUser() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(admin));

        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(netServiceCredentialFinder.byIdOptional(0L)).thenReturn(Optional.of(credential));
        when(credential.getUser()).thenReturn(peter);

        expected.expect(UnauthorizedException.class);
        nsm.deleteNetServiceCredential(0L);
        verify(defaultServer, times(0)).delete(credential);
    }


    @Test
    public void testDeleteNotExistingNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(peter));

        expected.expect(InvalidArgumentException.class);
        nsm.deleteNetServiceCredential(1L);
    }

    /*
        getEncryptedCredential
     */
    @Test
    public void testGetEncryptedCredential() throws UnauthorizedException, InvalidArgumentException {
        NetServiceCredential credential = mock(NetServiceCredential.class);

        when(sessionManager.currentPolicy().canReadCredential(credential)).thenReturn(true);
        when(netServiceCredentialFinder.byIdOptional(0L)).thenReturn(Optional.of(credential));

        NetServiceCredential nsReturned = nsm.getEncryptedCredential(0L);

        Assert.assertEquals(nsReturned, credential);
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetEncryptedCredentialNotAllowed() throws UnauthorizedException, InvalidArgumentException {
        NetServiceCredential credential = mock(NetServiceCredential.class);

        when(sessionManager.currentPolicy().canReadCredential(credential)).thenReturn(false);
        when(netServiceCredentialFinder.byIdOptional(0L)).thenReturn(Optional.of(credential));

        nsm.getEncryptedCredential(0L);
    }

    /*
        decryptCredential
     */
    @Test
    public void testDecryptCredential() throws InvalidArgumentException, UnauthorizedException {
        NetService netService = mock(NetService.class);
        when(netServiceFinder.byIdOptional(0L)).thenReturn(Optional.of(netService));

        byte[] credentialKey = (new RandomDataGenerator(new SecureRandom())).generateBytes(CryptoConstants.GENERATED_KEY_BYTE);
        when(sessionManager.getCredentialKey()).thenReturn(credentialKey);

        nsm.createNetUserCredential(0L, "usr", "pwd");

        ArgumentCaptor<NetServiceCredential> captor = ArgumentCaptor.forClass(NetServiceCredential.class);
        verify(defaultServer, times(1)).save(captor.capture());

        NetServiceCredential netServiceCredential = captor.getValue();
        netServiceCredential.setUser(peter);

        PlaintextCredential plaintextCredential = nsm.decryptCredential(netServiceCredential);

        Assert.assertEquals("usr", plaintextCredential.getUsername());
        Assert.assertEquals("pwd", plaintextCredential.getPassword());

    }

}
