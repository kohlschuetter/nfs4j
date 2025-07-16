package org.dcache.nfs.v4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.OpenCloseTracker;
import org.dcache.nfs.vfs.OpenHandle;

public class OpenCloseTrackerTester implements OpenCloseTracker {
    private int numOpenNew = 0;
    private int numOpenAlreadyOpen = 0;
    private int numClose = 0;

    private Integer assertNumOpenNew = null;
    private Integer assertNumOpenAlreadyOpen = null;
    private Integer assertNumClose = null;

    private boolean preventOpen = false;

    @Override
    public void open(OpenHandle oh, Inode inode, int accessMode, int denyMode, boolean alreadyOpen) throws IOException {
        if (alreadyOpen) {
            numOpenAlreadyOpen++;
        } else {
            numOpenNew++;
        }
        if (preventOpen) {
            throw new OpenCloseTrackerTestIOException("explicitly asked to fail \"open\"");
        }
    }

    @Override
    public void close(OpenHandle oh, Inode inode, int remainingOpens) {
        numClose++;
    }

    public int getNumOpen() {
        return numOpenNew + numOpenAlreadyOpen;
    }

    public int getNumOpenNew() {
        return numOpenNew;
    }

    public int getNumOpenAlreadyOpen() {
        return numOpenAlreadyOpen;
    }

    public int getNumClose() {
        return numClose;
    }

    public static class ExpectNoCalls extends OpenCloseTrackerTester {

        public ExpectNoCalls() {
            expectUponTeardownNumOpenNew(0);
            expectUponTeardownNumOpenAlreadyOpen(0);
            expectUponTeardownNumClose(0);
        }

        @Override
        public void open(OpenHandle oh, Inode inode, int accessMode, int denyMode, boolean alreadyOpen)
                throws IOException {
            try {
                super.open(oh, inode, accessMode, denyMode, alreadyOpen);
            } finally {
                fail("Unexpected call");
            }
        }

        @Override
        public void close(OpenHandle oh, Inode inode, int remainingOpens) {
            try {
                super.close(oh, inode, remainingOpens);
            } finally {
                fail("Unexpected call");
            }
        }
    }

    public void expectUponTeardownNumOpenNew(int n) {
        this.assertNumOpenNew = n;
    }

    public void expectUponTeardownNumOpenAlreadyOpen(int n) {
        this.assertNumOpenAlreadyOpen = n;
    }

    public void expectUponTeardownNumClose(int n) {
        this.assertNumClose = n;
    }

    public void tearDown() {
        if (assertNumOpenNew != null) {
            assertEquals("Unexpected number of calls to " + OpenCloseTracker.class.getName()
                    + ".open[alreadyOpen=false]",
                    (int) assertNumOpenNew, numOpenNew);
        }
        if (assertNumOpenAlreadyOpen != null) {
            assertEquals("Unexpected number of calls to " + OpenCloseTracker.class.getName()
                    + ".open[alreadyOpen=true]",
                    (int) assertNumOpenAlreadyOpen, numOpenAlreadyOpen);
        }
        if (assertNumClose != null) {
            assertEquals("Unexpected number of calls to " + OpenCloseTracker.class.getName() + ".close",
                    (int) assertNumClose, numClose);
        }
    }

    public void setPreventOpen(boolean preventOpen) {
        this.preventOpen = preventOpen;
    }

    public static final class OpenCloseTrackerTestIOException extends IOException {
        private static final long serialVersionUID = 1L;

        public OpenCloseTrackerTestIOException(String message) {
            super(message);
        }
    }
}
