/*
 * Copyright (c) 2017 - 2025 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.v4;

import static org.dcache.nfs.v4.NfsTestUtils.createClient;
import static org.dcache.nfs.v4.NfsTestUtils.generateFileHandle;
import static org.dcache.nfs.v4.xdr.nfs4_prot.OPEN4_SHARE_ACCESS_BOTH;
import static org.dcache.nfs.v4.xdr.nfs4_prot.OPEN4_SHARE_ACCESS_READ;
import static org.dcache.nfs.v4.xdr.nfs4_prot.OPEN4_SHARE_ACCESS_WANT_NO_DELEG;
import static org.dcache.nfs.v4.xdr.nfs4_prot.OPEN4_SHARE_ACCESS_WANT_READ_DELEG;
import static org.dcache.nfs.v4.xdr.nfs4_prot.OPEN4_SHARE_ACCESS_WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.UnknownHostException;

import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.status.BadStateidException;
import org.dcache.nfs.status.DelayException;
import org.dcache.nfs.status.InvalException;
import org.dcache.nfs.status.ShareDeniedException;
import org.dcache.nfs.v4.xdr.nfs_fh4;
import org.dcache.nfs.v4.xdr.seqid4;
import org.dcache.nfs.vfs.Inode;
import org.dcache.oncrpc4j.util.Opaque;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileTrackerTest {

    private OpenCloseTrackerTester openCloseTracker;
    private FileTracker tracker;
    private NFSv4StateHandler sh;

    @Before
    public void setUp() {
        openCloseTracker = new OpenCloseTrackerTester();
        sh = new NFSv4StateHandler(openCloseTracker);
        tracker = new FileTracker(openCloseTracker);
    }

    @After
    public void tearDown() {
        openCloseTracker.tearDown();
    }

    @Test
    public void shouldAllowNonConflictingOpens() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFileIdKey(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, 0);

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldReturnSameStateIdForSameClient() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFileIdKey(fh.value);

        var openRecord1 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord2 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, 0);
        assertEquals("New stateid returned", openRecord1.openStateId(), openRecord2.openStateId());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldReturnDifferentStateIdForDifferentOwners() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        StateOwner stateOwner2 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord2 = tracker.addOpen(client1, stateOwner2, inode, OPEN4_SHARE_ACCESS_READ, 0);
        assertNotEquals("Same stateid for different owners returned", openRecord1.openStateId(), openRecord2
                .openStateId());

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldMergeAccessModesOnMultipleOpenes() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, 0);
        int accessMode = tracker.getShareAccess(client1, inode, openRecord.openStateId());
        assertEquals("Access mode not merged", OPEN4_SHARE_ACCESS_BOTH, accessMode);

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldChangeAccessModesAfterDowngrade() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, 0);

        tracker.downgradeOpen(client1, openRecord.openStateId(), inode, OPEN4_SHARE_ACCESS_READ, 0);

        int accessMode = tracker.getShareAccess(client1, inode, openRecord.openStateId());
        assertEquals("Access mode not changed on downgrade", OPEN4_SHARE_ACCESS_READ, accessMode);

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test(expected = InvalException.class)
    public void shouldRejectDowngradeToNotOwnedMode() throws Exception {
        openCloseTracker.expectUponTeardownNumOpenNew(1);
        openCloseTracker.expectUponTeardownNumOpenAlreadyOpen(1);

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);

        tracker.downgradeOpen(client1, openRecord.openStateId(), inode, OPEN4_SHARE_ACCESS_WRITE, 0);
    }

    @Test(expected = InvalException.class)
    public void shouldRejectDowngradeDenyToNotOwnedMode() throws Exception {
        openCloseTracker.expectUponTeardownNumOpenNew(1);
        openCloseTracker.expectUponTeardownNumOpenAlreadyOpen(1);

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_BOTH, OPEN4_SHARE_ACCESS_READ);
        var openRecord = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, 0);

        tracker.downgradeOpen(client1, openRecord.openStateId(), inode, OPEN4_SHARE_ACCESS_READ,
                OPEN4_SHARE_ACCESS_WRITE);
    }

    @Test
    public void shouldReturnDifferentStateIdForDifferentClient() throws Exception {
        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        NFS4Client client2 = createClient(sh);
        StateOwner stateOwner2 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        var openRecord2 = tracker.addOpen(client2, stateOwner2, inode, OPEN4_SHARE_ACCESS_WRITE, 0);
        assertNotEquals("Same stateid returned", openRecord1.openStateId(), openRecord2.openStateId());

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test(expected = ShareDeniedException.class)
    public void shouldRejectConflictingOpens() throws Exception {
        openCloseTracker.expectUponTeardownNumOpenNew(1);
        openCloseTracker.expectUponTeardownNumOpenAlreadyOpen(0);

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, OPEN4_SHARE_ACCESS_READ);
    }

    @Test
    public void shouldAllowConflictingOpensAfterRemove() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        tracker.removeOpen(inode, openRecord.openStateId());

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_WRITE, OPEN4_SHARE_ACCESS_READ);

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
        assertEquals(1, openCloseTracker.getNumClose());
    }

    @Test(expected = BadStateidException.class)
    public void shouldFailToGetAccessModeWithBadStateid() throws Exception {

        NFS4Client client1 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        tracker.getShareAccess(client1, inode, client1.createOpenState(stateOwner1).stateid());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldGetReadDelegation() throws Exception {

        NFS4Client client = createClient(sh);
        ClientCB mockCallBack = mock(ClientCB.class);
        client.setCB(mockCallBack);

        StateOwner stateOwner1 = client.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord = tracker.addOpen(client, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);
        assertTrue("Read delegation not granted", openRecord.hasDelegation());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldNotReadDelegation() throws Exception {

        NFS4Client client = createClient(sh);
        ClientCB mockCallBack = mock(ClientCB.class);
        client.setCB(mockCallBack);

        StateOwner stateOwner1 = client.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord = tracker.addOpen(client, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        assertFalse("Read delegation is granted, but not requested", openRecord.hasDelegation());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldReCallReadDelegationOnConflict() throws Exception {

        NFS4Client client1 = createClient(sh);
        NFS4Client client2 = createClient(sh);

        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        StateOwner stateOwner2 = client2.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);
        try {
            var openRecord2 = tracker.addOpen(client2, stateOwner2, inode, OPEN4_SHARE_ACCESS_WRITE,
                    0);
            fail("Delay exception expected");
        } catch (DelayException e) {
            // expected
        }

        verify(client1.getCB()).cbDelegationRecall(any(), any(), anyBoolean());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldAllowMultipleReadDelegation() throws Exception {

        NFS4Client client1 = createClient(sh);
        NFS4Client client2 = createClient(sh);

        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        StateOwner stateOwner2 = client2.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);
        var openRecord2 = tracker.addOpen(client2, stateOwner2, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);

        assertTrue("Read delegation not granted", openRecord2.hasDelegation());

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldIssueReadDelegationOnMultipleOpens() throws Exception {

        NFS4Client client = createClient(sh);
        StateOwner stateOwner = client.getOrCreateOwner(Opaque.forUtf8Bytes("client"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client, stateOwner, inode, OPEN4_SHARE_ACCESS_READ, 0);
        assertFalse("Delegation not expected, but granted", openRecord1.hasDelegation());

        var openRecord2 = tracker.addOpen(client, stateOwner, inode, OPEN4_SHARE_ACCESS_READ, 0);
        assertTrue("Read opportunistic delegation not granted", openRecord2.hasDelegation());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void shouldNotIssueReadDelegation() throws Exception {

        NFS4Client client = createClient(sh);
        StateOwner stateOwner = client.getOrCreateOwner(Opaque.forUtf8Bytes("client"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        var openRecord1 = tracker.addOpen(client, stateOwner, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_NO_DELEG, 0);
        assertFalse("Unwanted delegation", openRecord1.hasDelegation());

        var openRecord2 = tracker.addOpen(client, stateOwner, inode, OPEN4_SHARE_ACCESS_READ
                | OPEN4_SHARE_ACCESS_WANT_NO_DELEG, 0);
        assertFalse("Unwanted delegation", openRecord2.hasDelegation());

        assertEquals(1, openCloseTracker.getNumOpenNew());
        assertEquals(1, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void getOpenFiles() throws UnknownHostException, ChimeraNFSException, IOException {

        NFS4Client client1 = createClient(sh);
        NFS4Client client2 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        StateOwner stateOwner2 = client2.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ, 0);
        tracker.addOpen(client2, stateOwner2, inode, OPEN4_SHARE_ACCESS_READ, 0);

        var openFiles = tracker.getOpenFiles();

        assertEquals("Number of open files not as expected", 1, openFiles.size());

        var clients = openFiles.get(inode);
        assertThat("Expected clients not found", clients, Matchers.containsInAnyOrder(client1, client2));

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }

    @Test
    public void getDelegations() throws UnknownHostException, ChimeraNFSException, IOException {

        NFS4Client client1 = createClient(sh);
        NFS4Client client2 = createClient(sh);
        StateOwner stateOwner1 = client1.getOrCreateOwner(Opaque.forUtf8Bytes("client1"), new seqid4(0));
        StateOwner stateOwner2 = client2.getOrCreateOwner(Opaque.forUtf8Bytes("client2"), new seqid4(0));

        nfs_fh4 fh = generateFileHandle();
        Inode inode = Inode.forFile(fh.value);

        tracker.addOpen(client1, stateOwner1, inode, OPEN4_SHARE_ACCESS_READ | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);
        tracker.addOpen(client2, stateOwner2, inode, OPEN4_SHARE_ACCESS_READ | OPEN4_SHARE_ACCESS_WANT_READ_DELEG, 0);

        var delegations = tracker.getDelegations();

        assertEquals("Number of open files not as expected", 1, delegations.size());

        var clients = delegations.get(inode);
        assertThat("Expected clients not found", clients, Matchers.containsInAnyOrder(client1, client2));

        assertEquals(2, openCloseTracker.getNumOpenNew());
        assertEquals(0, openCloseTracker.getNumOpenAlreadyOpen());
    }
}
