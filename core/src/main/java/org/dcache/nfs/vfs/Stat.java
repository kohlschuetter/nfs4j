/*
 * Copyright (c) 2009 - 2022 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs.vfs;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;

public class Stat implements Cloneable {

    public enum StatAttribute {
        DEV, INO, MODE, NLINK, OWNER, GROUP, RDEV, SIZE, GENERATION, ATIME, MTIME, CTIME, BTIME, BACKUPTIME, FLAGS
    };

    /**
     * A special {@link EnumSet} indicating that we're really only interested in the file type, which is usually part of
     * {@link StatAttribute#MODE}; the remaining bits of {@code mode} may be 0.
     */
    public static final EnumSet<StatAttribute> STAT_ATTRIBUTES_TYPE_ONLY = EnumSet.of(StatAttribute.MODE);

    private static final DateTimeFormatter LS_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd HH:mm");

    /**
     * Set of attributes defined in this {@code stat} object.
     */
    private final EnumSet<StatAttribute> _definedAttrs = EnumSet.noneOf(StatAttribute.class);

    public static final int S_TYPE = 0770000; // type mask
    public static final int S_PERMS = 0777; // permissions mask
    /**
     * Unix domain socket
     */
    public static final int S_IFSOCK = 0140000;
    /**
     * Symbolic link
     */
    public static final int S_IFLNK = 0120000;
    /**
     * Regular file
     */
    public static final int S_IFREG = 0100000;
    /**
     * BLock device
     */
    public static final int S_IFBLK = 0060000;
    /**
     * Directory
     */
    public static final int S_IFDIR = 0040000;
    /**
     * Character device
     */
    public static final int S_IFCHR = 0020000;
    /**
     * Named pipe
     */
    public static final int S_IFIFO = 0010000;

    public enum Type {

        LEGACY, REGULAR, DIRECTORY, SYMLINK, CHAR, BLOCK, FIFO, SOCK;

        public int toMode() {
            switch (this) {
                case REGULAR:
                    return S_IFREG;
                case DIRECTORY:
                    return S_IFDIR;
                case SYMLINK:
                    return S_IFLNK;
                case CHAR:
                    return S_IFCHR;
                case BLOCK:
                    return S_IFBLK;
                case FIFO:
                    return S_IFIFO;
                case SOCK:
                    return S_IFSOCK;
                default:
                    throw new IllegalArgumentException("unhandled: " + this);
            }
        }

        public static Type fromMode(int mode) {
            switch (mode & S_TYPE) {
                case S_IFREG:
                    return Type.REGULAR;
                case S_IFDIR:
                    return Type.DIRECTORY;
                case S_IFLNK:
                    return Type.SYMLINK;
                case S_IFCHR:
                    return Type.CHAR;
                case S_IFBLK:
                    return Type.BLOCK;
                case S_IFIFO:
                    return Type.FIFO;
                case S_IFSOCK:
                    return Type.SOCK;
                default:
                    return Type.REGULAR;
            }
        }
    }

    // see <sys/stat.h> on macOS, BSD, etc.
    public enum Flag {
        /** mask of owner changeable flags */
        UF_SETTABLE(0x0000ffff),

        /** do not dump file */
        UF_NODUMP(0x00000001),

        /** file may not be changed */
        UF_IMMUTABLE(0x00000002),

        /** writes to file may only append */
        UF_APPEND(0x00000004),

        /** directory is opaque wrt. union */
        UF_OPAQUE(0x00000008),

        /** file may not be removed or renamed */
        UF_NOUNLINK(0x00000010),

        /** file is compressed (macOS) */
        UF_COMPRESSED(0x00000020),

        /** renames and deletes are tracked (macOS) */
        UF_TRACKED(0x00000040),

        /** Windows system file bit, or macOS: entitlement required for reading/writing */
        UF_SYSTEM_OR_DATAVAULT(0x00000080),

        /** sparse file (BSD); macOS: see "Extended flags" */
        UF_SPARSE(0x00000100),

        /** file is offline */
        UF_OFFLINE(0x00000200),

        /** Windows reparse point file bit */
        UF_REPARSE(0x00000400),

        /**
         * file needs to be archived
         *
         * @see #SF_ARCHIVED
         */
        UF_ARCHIVE(0x00000800),

        /** Windows readonly file bit */
        UF_READONLY(0x00001000),

        /** file is hidden */
        UF_HIDDEN(0x00008000),

        /** mask of superuser changeable flags; note that SF_DATALESS and higher are not settable */
        SF_SETTABLE(0x3fff0000),
        /** mask of system read-only synthetic flags */
        SF_SYNTHETIC(0xc0000000),

        /** file is archived */
        SF_ARCHIVED(0x00010000),

        /** file may not be changed */
        SF_IMMUTABLE(0x00020000),

        /** writes to file may only append */
        SF_APPEND(0x00040000),

        /** macOS: entitlement required for writing */
        SF_RESTRICTED(0x00080000),

        /** file may not be removed or renamed */
        SF_NOUNLINK(0x00100000),

        /** BSD: snapshot inode */
        SF_SNAPSHOT(0x00200000),

        /** macOS: file is a firmlink */
        SF_FIRMLINK(0x00800000),

        /** macOS: file is dataless object */
        SF_DATALESS(0x40000000),
        ;

        private final int bitmask;

        Flag(int bitmask) {
            this.bitmask = bitmask;
        }

        public int getBitmask() {
            return bitmask;
        }

        public static int bitmask(EnumSet<Flag> enumSet) {
            int mask = 0;
            for (Flag f : enumSet) {
                mask |= f.bitmask;
            }
            return mask;
        }
    }

    private int _dev;
    private long _ino;
    private int _mode;
    private int _nlink;
    private int _owner;
    private int _group;
    private int _rdev;
    private long _size;
    private long _generation;
    private int _flags;
    private int _flagsMask;

    /*
     * Opposite to classic Unix, all times in milliseconds
     */
    private long _atime;
    private long _mtime;
    private long _ctime;
    private long _btime;
    private long _backupTime;

    /**
     * Returns the ID of device containing file.
     */
    public int getDev() {
        guard(StatAttribute.DEV);
        return _dev;
    }

    /**
     * Set the ID of device containing file.
     */
    public void setDev(int dev) {
        define(StatAttribute.DEV);
        _dev = dev;
    }

    /**
     * Returns file inode number.
     */
    public long getIno() {
        guard(StatAttribute.INO);
        return _ino;
    }

    /**
     * Set files inode number.
     */
    public void setIno(long ino) {
        define(StatAttribute.INO);
        _ino = ino;
    }

    /**
     * Returns files type and mode.
     */
    public int getMode() {
        guard(StatAttribute.MODE);
        return _mode;
    }

    /**
     * Set files type and mode.
     */
    public void setMode(int mode) {
        define(StatAttribute.MODE);
        _mode = mode;
    }

    /**
     * Returns number of hard links.
     */
    public int getNlink() {
        guard(StatAttribute.NLINK);
        return _nlink;
    }

    /**
     * Sets number of hard links.
     */
    public void setNlink(int nlink) {
        define(StatAttribute.NLINK);
        _nlink = nlink;
    }

    /**
     * Returns User ID of owner.
     */
    public int getUid() {
        guard(StatAttribute.OWNER);
        return _owner;
    }

    /**
     * Sets user ID of owner.
     */
    public void setUid(int owner) {
        define(StatAttribute.OWNER);
        _owner = owner;
    }

    /**
     * Returns group ID of owner.
     */
    public int getGid() {
        guard(StatAttribute.GROUP);
        return _group;
    }

    /**
     * Sets group ID of owner.
     */
    public void setGid(int group) {
        define(StatAttribute.GROUP);
        _group = group;
    }

    /**
     * Returns device ID, if special file.
     */
    public int getRdev() {
        guard(StatAttribute.RDEV);
        return _rdev;
    }

    /**
     * Sets' special files device ID.
     */
    public void setRdev(int rdev) {
        define(StatAttribute.RDEV);
        _rdev = rdev;
    }

    /**
     * Returns total file size, in bytes.
     */
    public long getSize() {
        guard(StatAttribute.SIZE);
        return _size;
    }

    /**
     * Sets total file size, in bytes.
     */
    public void setSize(long size) {
        define(StatAttribute.SIZE);
        _size = size;
    }

    /**
     * Returns files last access time, in milliseconds since January 1, 1970.
     */
    public long getATime() {
        guard(StatAttribute.ATIME);
        return _atime;
    }

    /**
     * Set files last access time, in milliseconds since January 1, 1970.
     */
    public void setATime(long atime) {
        define(StatAttribute.ATIME);
        _atime = atime;
    }

    /**
     * Returns files last modification time, in milliseconds since January 1, 1970.
     */
    public long getMTime() {
        guard(StatAttribute.MTIME);
        return _mtime;
    }

    /**
     * Set files last modification time, in milliseconds since January 1, 1970.
     */
    public void setMTime(long mtime) {
        define(StatAttribute.MTIME);
        _mtime = mtime;
    }

    /**
     * Returns files last attribute change time, in milliseconds since January 1, 1970.
     */
    public long getCTime() {
        guard(StatAttribute.CTIME);
        return _ctime;
    }

    /**
     * Set files last attribute change time, in milliseconds since January 1, 1970.
     */
    public void setCTime(long ctime) {
        define(StatAttribute.CTIME);
        _ctime = ctime;
    }

    /**
     * Returns files creation (birth) time, in milliseconds since January 1, 1970.
     */
    public long getBTime() {
        guard(StatAttribute.BTIME);
        return _btime;
    }

    /**
     * Set files creation (birth) time, in milliseconds since January 1, 1970.
     */
    public void setBTime(long btime) {
        define(StatAttribute.BTIME);
        _btime = btime;
    }

    /**
     * Returns the last-backup time, in milliseconds since January 1, 1970.
     */
    public long getBackupTime() {
        guard(StatAttribute.BACKUPTIME);
        return _backupTime;
    }

    /**
     * Set the last-backup, in milliseconds since January 1, 1970.
     */
    public void setBackupTime(long backuptime) {
        define(StatAttribute.BACKUPTIME);
        _backupTime = backuptime;
    }

    /**
     * Returns files change counter.
     */
    public long getGeneration() {
        guard(StatAttribute.GENERATION);
        return _generation;
    }

    /**
     * Set files change counter.
     */
    public void setGeneration(long generation) {
        define(StatAttribute.GENERATION);
        _generation = generation;
    }

    /**
     * Sets the flags, using the given flagsMask describing which flags are covered.
     */
    public void setFlags(int flags, int flagsMask) {
        define(StatAttribute.FLAGS);
        _flags = flags;
        _flagsMask = flagsMask;
    }

    public void addFlag(Flag flag, boolean on) {
        define(StatAttribute.FLAGS);
        _flagsMask |= flag.bitmask;
        if (on) {
            _flags |= flag.bitmask;
        }
    }

    public boolean getFlagState(Flag flag) {
        return (_flags & flag.bitmask) != 0;
    }

    public boolean containsFlag(Flag flag) {
        if (!isDefined(StatAttribute.FLAGS)) {
            return false;
        }
        return ((_flags | _flagsMask) & flag.bitmask) != 0;
    }

    public int flagsOtherThan(int bitmask) {
        if (!isDefined(StatAttribute.FLAGS)) {
            return 0;
        }
        return ((_flags | _flagsMask) & ~bitmask);
    }

    /**
     * Retunrs files type.
     */
    public Type type() {
        guard(StatAttribute.MODE);
        return Type.fromMode(_mode);
    }

    public static String modeToString(int mode) {
        StringBuilder result = new StringBuilder(10);
        switch (Type.fromMode(mode)) {
            case BLOCK:
                result.append("b");
                break;
            case CHAR:
                result.append("c");
                break;
            case DIRECTORY:
                result.append("d");
                break;
            case FIFO:
                result.append("p");
                break;
            case SOCK:
                result.append("s");
                break;
            case SYMLINK:
                result.append("l");
                break;
            default:
                result.append("-");
        }
        // owner, group, other
        for (int i = 0; i < 3; i++) {
            int acl = (mode >> (6 - 3 * i)) & 0000007;
            switch (acl) {
                case 00:
                    result.append("---");
                    break;
                case 01:
                    result.append("--x");
                    break;
                case 02:
                    result.append("-w-");
                    break;
                case 03:
                    result.append("-wx");
                    break;
                case 04:
                    result.append("r--");
                    break;
                case 05:
                    result.append("r-x");
                    break;
                case 06:
                    result.append("rw-");
                    break;
                case 07:
                    result.append("rwx");
                    break;
            }
        }
        return result.toString();
    }

    // technically _size (java long) will overflow after ~8 exabytes, so "Z"/"Y" is unreachable
    private final static String[] SIZE_UNITS = {"", "K", "M", "G", "T", "P", "E", "Z", "Y"};

    public static String sizeToString(long bytes) {
        if (bytes == 0) {
            return "0";
        }
        int orderOfMagnitude = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        double significantSize = (double) bytes / (1L << orderOfMagnitude * 10);
        DecimalFormat sizeFormat = new DecimalFormat("#.#"); // not thread safe
        return sizeFormat.format(significantSize) + SIZE_UNITS[orderOfMagnitude];
    }

    @Override
    public Stat clone() {
        try {
            return (Stat) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the equivalent of "ls -lh" (as close as possible)
     */
    @Override
    public String toString() {
        String humanReadableSize = sizeToString(_size);
        String humanReadableMTime = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(_mtime), ZoneId.systemDefault())
                .format(LS_TIME_FORMAT);

        String flagsStr = isDefined(StatAttribute.FLAGS) ? String.format(" flags=%x/%x", _flags, _flagsMask) : "";

        return modeToString(_mode) + " " + String.format("%4d %4d %4d %4s %s", _nlink, _owner, _group,
                humanReadableSize, humanReadableMTime) + flagsStr;
    }

    /**
     * Check is attribute defined in this {@code stat} object;
     *
     * @param attr attribute to check
     * @return true iff specified attribute is defined in this stat object.
     */
    public boolean isDefined(StatAttribute attr) {
        return _definedAttrs.contains(attr);
    }

    /**
     * Undefine attribute this {@code stat} object. Accessing an attribute after it's undefined will throw
     * {@link IllegalStateException}.
     *
     * @param attr attribute to undefine.
     */
    public void undefine(StatAttribute attr) {
        _definedAttrs.remove(attr);
    }

    /** Throws IllegalStateException if attribute is not defined. */
    private void guard(StatAttribute attr) throws IllegalStateException {
        if (!isDefined(attr)) {
            throw new IllegalStateException("Attribute is not defined: " + attr);
        }
    }

    private void define(StatAttribute attr) {
        _definedAttrs.add(attr);
    }
}
