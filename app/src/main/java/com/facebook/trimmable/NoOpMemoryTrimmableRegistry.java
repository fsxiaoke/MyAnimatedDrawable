package com.facebook.trimmable;

/**
 * Created by heshixiyang on 2017/3/12.
 */
/**
 * 一个实现了{@link MemoryTrimmableRegistry}但是什么都没做的class
 * Implementation of {@link MemoryTrimmableRegistry} that does not do anything.
 */
public class NoOpMemoryTrimmableRegistry implements MemoryTrimmableRegistry {
    private static NoOpMemoryTrimmableRegistry sInstance = null;

    public NoOpMemoryTrimmableRegistry() {
    }

    public static synchronized NoOpMemoryTrimmableRegistry getInstance() {
        if (sInstance == null) {
            sInstance = new NoOpMemoryTrimmableRegistry();
        }
        return sInstance;
    }

    /** Register an object. */
    public void registerMemoryTrimmable(MemoryTrimmable trimmable) {
    }

    /** Unregister an object. */
    public void unregisterMemoryTrimmable(MemoryTrimmable trimmable) {
    }
}

