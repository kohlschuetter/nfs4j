package org.dcache.nfs.vfs;

import java.io.IOException;

/**
 * Hooks for {@link Inode} open/close operations.
 */
public interface OpenCloseTracker {
    /**
     * Called upon an attempt to open the given inode for reading and/or writing.
     * 
     * @param oh The open-handle.
     * @param inode The inode.
     * @param accessMode The share-access mode.
     * @param denyMode The share-deny mode.
     * @param alreadyOpen Whether the resource has already been open using the given {@link OpenHandle}.
     * @throws IOException on error, on access denied.
     */
    default void open(OpenHandle oh, Inode inode, int accessMode, int denyMode, boolean alreadyOpen)
            throws IOException {
    }

    /**
     * Called upon closing the given inode.
     * 
     * @param oh The open-handle.
     * @param inode The inode.
     * @param remainingOpens The number of remaining "open" calls under the given {@link OpenHandle} ({@code 0} for "all
     *            closed"), or {@code -1} for "unknown".
     */
    default void close(OpenHandle oh, Inode inode, int remainingOpens) {
    }
}
