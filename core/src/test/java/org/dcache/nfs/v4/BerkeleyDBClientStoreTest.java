package org.dcache.nfs.v4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dcache.nfs.status.NoGraceException;
import org.dcache.nfs.status.ReclaimBadException;
import org.dcache.oncrpc4j.util.Opaque;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class BerkeleyDBClientStoreTest {

    private BerkeleyDBClientStore clientRecoveryStore;
    private Path storeDirectory;

    @Before
    public void setUp() throws IOException {
        storeDirectory = Files.createTempDirectory("nfs-client-store");
    }

    @After
    public void tearDown() throws Exception {
        clientRecoveryStore.close();
        Files.list(storeDirectory).forEach(f -> {
            try {
                Files.delete(f);
            } catch (IOException e) {
                // as we fail to delete, directory remove will fail as well
            }
        });
        Files.delete(storeDirectory);
    }

    @Test
    public void shouldNotWaitForClientsOnFirstStart() {
        givenServer();
        assertFalse(clientRecoveryStore.waitingForReclaim());
    }

    @Test
    public void shouldCloseReclaimWindowOnComplete() throws Exception {
        givenServer();
        clientRecoveryStore.reclaimComplete();
        assertFalse(clientRecoveryStore.waitingForReclaim());
    }

    @Test
    public void shouldWaitForClientsAfterRestart() throws Exception {
        givenServer();
        clientRecoveryStore.addClient(Opaque.forUtf8Bytes("client1"));
        reboot();

        assertTrue(clientRecoveryStore.waitingForReclaim());
    }

    @Test(expected = ReclaimBadException.class)
    public void shouldFailWhenNewClientWantReclaim() throws Exception {
        givenServer();
        clientRecoveryStore.addClient(Opaque.forUtf8Bytes("client1"));
        clientRecoveryStore.wantReclaim(Opaque.forUtf8Bytes("client1"));
    }

    @Test
    public void shouldReclaimAfterReboot() throws Exception {
        givenServer();
        clientRecoveryStore.addClient(Opaque.forUtf8Bytes("client1"));
        reboot();
        clientRecoveryStore.addClient(Opaque.forUtf8Bytes("client1"));
        clientRecoveryStore.wantReclaim(Opaque.forUtf8Bytes("client1"));
    }

    @Test(expected = ReclaimBadException.class)
    public void shouldFailReclaimAfterRemove() throws Exception {
        givenServer();
        clientRecoveryStore.addClient(Opaque.forUtf8Bytes("client1"));
        clientRecoveryStore.removeClient(Opaque.forUtf8Bytes("client1"));
        clientRecoveryStore.wantReclaim(Opaque.forUtf8Bytes("client1"));
    }

    @Test(expected = NoGraceException.class)
    public void shouldFailOnLateReclaim() throws Exception {
        givenServer();
        clientRecoveryStore.reclaimComplete();
        clientRecoveryStore.wantReclaim(Opaque.forUtf8Bytes("client1"));
    }

    private void givenServer() {
        clientRecoveryStore = new BerkeleyDBClientStore(storeDirectory.toFile());
    }

    private void reboot() throws Exception {
        clientRecoveryStore.close();
        clientRecoveryStore = new BerkeleyDBClientStore(storeDirectory.toFile());
    }
}
