package com.facebook.pool.poolFactory;

/**
 * Created by heshixiyang on 2017/3/31.
 */

import com.facebook.common.s.Preconditions;
import com.facebook.pool.poolParams.DefaultBitmapPoolParams;
import com.facebook.pool.poolParams.DefaultByteArrayPoolParams;
import com.facebook.pool.poolParams.DefaultFlexByteArrayPoolParams;
import com.facebook.pool.poolParams.DefaultNativeMemoryChunkPoolParams;
import com.facebook.pool.poolParams.PoolParams;
import com.facebook.pool.poolUtil.NoOpPoolStatsTracker;
import com.facebook.pool.poolUtil.PoolStatsTracker;
import com.facebook.trimmable.MemoryTrimmableRegistry;
import com.facebook.trimmable.NoOpMemoryTrimmableRegistry;

import javax.annotation.concurrent.Immutable;

/**
 * Pool的配置
 * Configuration class for pools.
 */
@Immutable
public class PoolConfig {

    // There are a lot of parameters in this class. Please follow strict alphabetical order.

    private final PoolParams mBitmapPoolParams;
    private final PoolStatsTracker mBitmapPoolStatsTracker;
    private final PoolParams mFlexByteArrayPoolParams;
    private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    private final PoolParams mNativeMemoryChunkPoolParams;
    private final PoolStatsTracker mNativeMemoryChunkPoolStatsTracker;
    private final PoolParams mSmallByteArrayPoolParams;
    private final PoolStatsTracker mSmallByteArrayPoolStatsTracker;

    private PoolConfig(Builder builder) {
        mBitmapPoolParams =
                builder.mBitmapPoolParams == null ?
                        DefaultBitmapPoolParams.get() :
                        builder.mBitmapPoolParams;
        mBitmapPoolStatsTracker =
                builder.mBitmapPoolStatsTracker == null ?
                        NoOpPoolStatsTracker.getInstance() :
                        builder.mBitmapPoolStatsTracker;
        mFlexByteArrayPoolParams =
                builder.mFlexByteArrayPoolParams == null ?
                        DefaultFlexByteArrayPoolParams.get() :
                        builder.mFlexByteArrayPoolParams;
        mMemoryTrimmableRegistry =
                builder.mMemoryTrimmableRegistry == null ?
                        NoOpMemoryTrimmableRegistry.getInstance() :
                        builder.mMemoryTrimmableRegistry;
        mNativeMemoryChunkPoolParams =
                builder.mNativeMemoryChunkPoolParams == null ?
                        DefaultNativeMemoryChunkPoolParams.get() :
                        builder.mNativeMemoryChunkPoolParams;
        mNativeMemoryChunkPoolStatsTracker =
                builder.mNativeMemoryChunkPoolStatsTracker == null ?
                        NoOpPoolStatsTracker.getInstance() :
                        builder.mNativeMemoryChunkPoolStatsTracker;
        mSmallByteArrayPoolParams =
                builder.mSmallByteArrayPoolParams == null ?
                        DefaultByteArrayPoolParams.get() :
                        builder.mSmallByteArrayPoolParams;
        mSmallByteArrayPoolStatsTracker =
                builder.mSmallByteArrayPoolStatsTracker == null ?
                        NoOpPoolStatsTracker.getInstance() :
                        builder.mSmallByteArrayPoolStatsTracker;
    }

    public PoolParams getBitmapPoolParams() {
        return mBitmapPoolParams;
    }

    public PoolStatsTracker getBitmapPoolStatsTracker() {
        return mBitmapPoolStatsTracker;
    }

    public MemoryTrimmableRegistry getMemoryTrimmableRegistry() {
        return mMemoryTrimmableRegistry;
    }

    public PoolParams getNativeMemoryChunkPoolParams() {
        return mNativeMemoryChunkPoolParams;
    }

    public PoolStatsTracker getNativeMemoryChunkPoolStatsTracker() {
        return mNativeMemoryChunkPoolStatsTracker;
    }

    public PoolParams getFlexByteArrayPoolParams() {
        return mFlexByteArrayPoolParams;
    }

    public PoolParams getSmallByteArrayPoolParams() {
        return mSmallByteArrayPoolParams;
    }

    public PoolStatsTracker getSmallByteArrayPoolStatsTracker() {
        return mSmallByteArrayPoolStatsTracker;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private PoolParams mBitmapPoolParams;
        private PoolStatsTracker mBitmapPoolStatsTracker;
        private PoolParams mFlexByteArrayPoolParams;
        private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
        private PoolParams mNativeMemoryChunkPoolParams;
        private PoolStatsTracker mNativeMemoryChunkPoolStatsTracker;
        private PoolParams mSmallByteArrayPoolParams;
        private PoolStatsTracker mSmallByteArrayPoolStatsTracker;

        private Builder() {
        }

        public Builder setBitmapPoolParams(PoolParams bitmapPoolParams) {
            mBitmapPoolParams = Preconditions.checkNotNull(bitmapPoolParams);
            return this;
        }

        public Builder setBitmapPoolStatsTracker(
                PoolStatsTracker bitmapPoolStatsTracker) {
            mBitmapPoolStatsTracker = Preconditions.checkNotNull(bitmapPoolStatsTracker);
            return this;
        }

        public Builder setFlexByteArrayPoolParams(PoolParams flexByteArrayPoolParams) {
            mFlexByteArrayPoolParams = flexByteArrayPoolParams;
            return this;
        }

        public Builder setMemoryTrimmableRegistry(MemoryTrimmableRegistry memoryTrimmableRegistry) {
            mMemoryTrimmableRegistry = memoryTrimmableRegistry;
            return this;
        }

        public Builder setNativeMemoryChunkPoolParams(PoolParams nativeMemoryChunkPoolParams) {
            mNativeMemoryChunkPoolParams = Preconditions.checkNotNull(nativeMemoryChunkPoolParams);
            return this;
        }

        public Builder setNativeMemoryChunkPoolStatsTracker(
                PoolStatsTracker nativeMemoryChunkPoolStatsTracker) {
            mNativeMemoryChunkPoolStatsTracker =
                    Preconditions.checkNotNull(nativeMemoryChunkPoolStatsTracker);
            return this;
        }

        public Builder setSmallByteArrayPoolParams(PoolParams commonByteArrayPoolParams) {
            mSmallByteArrayPoolParams = Preconditions.checkNotNull(commonByteArrayPoolParams);
            return this;
        }

        public Builder setSmallByteArrayPoolStatsTracker(
                PoolStatsTracker smallByteArrayPoolStatsTracker) {
            mSmallByteArrayPoolStatsTracker =
                    Preconditions.checkNotNull(smallByteArrayPoolStatsTracker);
            return this;
        }

        public PoolConfig build() {
            return new PoolConfig(this);
        }
    }
}
