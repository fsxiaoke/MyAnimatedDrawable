package com.facebook.image;

/**
 * Created by Administrator on 2017/3/28 0028.
 */

import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.facebook.cacheKey.CacheKey;
import com.facebook.common.PooledByteBufferInputStream;
import com.facebook.common.s.Preconditions;
import com.facebook.common.util.BitmapUtil;
import com.facebook.common.util.JfifUtil;
import com.facebook.common.util.WebpUtil;
import com.facebook.image.imageFormat.DefaultImageFormats;
import com.facebook.image.imageFormat.ImageFormat;
import com.facebook.image.imageFormat.ImageFormatChecker;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.references.CloseableReference;
import com.facebook.references.SharedReference;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * 包含了所有编码image的class，包括一个image字节流(从一个字节缓存或者一个input streams中来)
 * 和其他对于image转换有用的数据
 * Class that contains all the information for an encoded image, both the image bytes (held on
 * a byte buffer or a supplier of input streams) and the extracted meta data that is useful for
 * image transforms.
 *
 * 只有一个input stream supplier或者一个字节缓冲可以被设置。如果使用一个input stream supplier
 * 返回字节缓冲的方法会返回null。getInputStream会一直被提供。
 * <p>Only one of the input stream supplier or the byte buffer can be set. If using an input stream
 * supplier, the methods that return a byte buffer will simply return null. However, getInputStream
 * will always be supported, either from the supplier or an input stream created from the byte
 * buffer held.
 * 当前的数据是可以用rotation and resize 的
 * <p>Currently the data is useful for rotation and resize.
 */
@Immutable
public class EncodedImage implements Closeable {
    public static final int UNKNOWN_ROTATION_ANGLE = -1;
    public static final int UNKNOWN_WIDTH = -1;
    public static final int UNKNOWN_HEIGHT = -1;
    public static final int UNKNOWN_STREAM_SIZE = -1;

    public static final int DEFAULT_SAMPLE_SIZE = 1;

    // Only one of this will be set. The EncodedImage can either be backed by a ByteBuffer or a
    // Supplier of InputStream, but not both.
    private final @Nullable
    CloseableReference<PooledByteBuffer> mPooledByteBufferRef;
    private final @Nullable
    Supplier<FileInputStream> mInputStreamSupplier;

    private ImageFormat mImageFormat = ImageFormat.UNKNOWN;
    private int mRotationAngle = UNKNOWN_ROTATION_ANGLE;
    private int mWidth = UNKNOWN_WIDTH;
    private int mHeight = UNKNOWN_HEIGHT;
    private int mSampleSize = DEFAULT_SAMPLE_SIZE;
    private int mStreamSize = UNKNOWN_STREAM_SIZE;
    private @Nullable
    CacheKey mEncodedCacheKey;

    public EncodedImage(CloseableReference<PooledByteBuffer> pooledByteBufferRef) {
        Preconditions.checkArgument(CloseableReference.isValid(pooledByteBufferRef));
        this.mPooledByteBufferRef = pooledByteBufferRef.clone();
        this.mInputStreamSupplier = null;
    }

    public EncodedImage(Supplier<FileInputStream> inputStreamSupplier) {
        Preconditions.checkNotNull(inputStreamSupplier);
        this.mPooledByteBufferRef = null;
        this.mInputStreamSupplier = inputStreamSupplier;
    }

    public EncodedImage(Supplier<FileInputStream> inputStreamSupplier, int streamSize) {
        this(inputStreamSupplier);
        this.mStreamSize = streamSize;
    }

    /**
     * 返回被克隆的编码image
     * Returns the cloned encoded image if the parameter received is not null, null otherwise.
     *
     * @param encodedImage the EncodedImage to clone
     */
    public static EncodedImage cloneOrNull(EncodedImage encodedImage) {
        return encodedImage != null ? encodedImage.cloneOrNull() : null;
    }

    //具体的执行克隆的方法，为浅拷贝
    public EncodedImage cloneOrNull() {
        EncodedImage encodedImage;
        if (mInputStreamSupplier != null) {
            encodedImage = new EncodedImage(mInputStreamSupplier, mStreamSize);
        } else {
            CloseableReference<PooledByteBuffer> pooledByteBufferRef =
                    CloseableReference.cloneOrNull(mPooledByteBufferRef);
            try {
                encodedImage = (pooledByteBufferRef == null) ? null : new EncodedImage(pooledByteBufferRef);
            } finally {
                // Close the recently created reference since it will be cloned again in the constructor.
                CloseableReference.closeSafely(pooledByteBufferRef);
            }
        }
        if (encodedImage != null) {
            encodedImage.copyMetaDataFrom(this);
        }
        return encodedImage;
    }

    /**
     *
     * Closes the buffer enclosed by this class.
     */
    @Override
    public void close() {
        CloseableReference.closeSafely(mPooledByteBufferRef);
    }

    /**
     * 是否是有效的，如果两个数据源其中一个不为null的返回true
     * Returns true if the internal buffer reference is valid or the InputStream Supplier is not null,
     * false otherwise.
     */
    public synchronized boolean isValid() {
        return CloseableReference.isValid(mPooledByteBufferRef) || mInputStreamSupplier != null;
    }

    /**
     *
     * Returns a cloned reference to the stored encoded bytes.
     *
     * <p>The caller has to close the reference once it has finished using it.
     */
    public CloseableReference<PooledByteBuffer> getByteBufferRef() {
        return CloseableReference.cloneOrNull(mPooledByteBufferRef);
    }

    /**
     * Returns an InputStream from the internal InputStream Supplier if it's not null. Otherwise
     * returns an InputStream for the internal buffer reference if valid and null otherwise.
     *
     * <p>The caller has to close the InputStream after using it.
     */
    public InputStream getInputStream() {
        if (mInputStreamSupplier != null) {
            return mInputStreamSupplier.get();
        }
        CloseableReference<PooledByteBuffer> pooledByteBufferRef =
                CloseableReference.cloneOrNull(mPooledByteBufferRef);
        if (pooledByteBufferRef != null) {
            try {
                return new PooledByteBufferInputStream(pooledByteBufferRef.get());
            } finally {
                CloseableReference.closeSafely(pooledByteBufferRef);
            }
        }
        return null;
    }

    /**
     * Sets the image format
     */
    public void setImageFormat(ImageFormat imageFormat) {
        this.mImageFormat = imageFormat;
    }

    /**
     * Sets the image height
     */
    public void setHeight(int height) {
        this.mHeight = height;
    }

    /**
     * Sets the image width
     */
    public void setWidth(int width) {
        this.mWidth = width;
    }

    /**
     * Sets the image rotation angle
     */
    public void setRotationAngle(int rotationAngle) {
        this.mRotationAngle = rotationAngle;
    }

    /**
     * Sets the image sample size
     */
    public void setSampleSize(int sampleSize) {
        this.mSampleSize = sampleSize;
    }

    /**
     * Sets the size of an image if backed by an InputStream
     *
     * <p> Ignored if backed by a ByteBuffer
     */
    public void setStreamSize(int streamSize) {
        this.mStreamSize = streamSize;
    }

    /**
     * Sets the key related to this image for encoded caches
     * @param encodedCacheKey
     */
    public void setEncodedCacheKey(@Nullable CacheKey encodedCacheKey) {
        mEncodedCacheKey = encodedCacheKey;
    }

    /**
     * Returns the image format if known, otherwise ImageFormat.UNKNOWN.
     */
    public ImageFormat getImageFormat() {
        return mImageFormat;
    }

    /**
     * Only valid if the image format is JPEG.
     * @return the rotation angle if the rotation angle is known, else -1. The rotation angle may not
     * be known if the image is incomplete (e.g. for progressive JPEGs).
     */
    public int getRotationAngle() {
        return mRotationAngle;
    }

    /**
     * Only valid if the image format is JPEG.
     * @return width if the width is known, else -1.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Only valid if the image format is JPEG.
     * @return height if the height is known, else -1.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Only valid if the image format is JPEG.
     * @return sample size of the image.
     */
    public int getSampleSize() {
        return mSampleSize;
    }

    /**
     * Gets the key to use when storing this image in encoded caches
     * @return the encoded cache key
     */
    @Nullable
    public CacheKey getEncodedCacheKey() {
        return mEncodedCacheKey;
    }

    /**
     * 如果image是一个JPEG，并且其数据在特殊的length之前完成了，那么返回true
     * Returns true if the image is a JPEG and its data is already complete at the specified length,
     * false otherwise.
     */
    public boolean isCompleteAt(int length) {
        if (mImageFormat != DefaultImageFormats.JPEG) {
            return true;
        }
        // If the image is backed by FileInputStreams return true since they will always be complete.
        if (mInputStreamSupplier != null) {
            return true;
        }
        // The image should be backed by a ByteBuffer
        Preconditions.checkNotNull(mPooledByteBufferRef);
        PooledByteBuffer buf = mPooledByteBufferRef.get();
        return (buf.read(length - 2) == (byte) JfifUtil.MARKER_FIRST_BYTE)
                && (buf.read(length - 1) == (byte) JfifUtil.MARKER_EOI);
    }

    /**
     * Returns the size of the backing structure.
     *
     * <p> If it's a PooledByteBuffer returns its size if its not null, -1 otherwise. If it's an
     * InputStream, return the size if it was set, -1 otherwise.
     */
    public int getSize() {
        if (mPooledByteBufferRef != null && mPooledByteBufferRef.get() != null) {
            return mPooledByteBufferRef.get().size();
        }
        return mStreamSize;
    }

    /**
     * 设置编码image的元数据
     * Sets the encoded image meta data.
     */
    public void parseMetaData() {
        final ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
                getInputStream());
        mImageFormat = imageFormat;
        // BitmapUtil.decodeDimensions has a bug where it will return 100x100 for some WebPs even though
        // those are not its actual dimensions
        final Pair<Integer, Integer> dimensions;
        if (DefaultImageFormats.isWebpFormat(imageFormat)) {
            dimensions = readWebPImageSize();
        } else {
            dimensions = readImageSize();
        }
        if (imageFormat == DefaultImageFormats.JPEG && mRotationAngle == UNKNOWN_ROTATION_ANGLE) {
            // Load the JPEG rotation angle only if we have the dimensions
            if (dimensions != null) {
                mRotationAngle = JfifUtil.getAutoRotateAngleFromOrientation(
                        JfifUtil.getOrientation(getInputStream()));
            }
        } else {
            mRotationAngle = 0;
        }
    }

    /**
     * We get the size from a WebP image
     */
    private Pair<Integer, Integer> readWebPImageSize() {
        final Pair<Integer, Integer> dimensions = WebpUtil.getSize(getInputStream());
        if (dimensions != null) {
            mWidth = dimensions.first;
            mHeight = dimensions.second;
        }
        return dimensions;
    }

    /**
     * We get the size from a generic image
     */
    private Pair<Integer, Integer> readImageSize() {
        InputStream inputStream = null;
        Pair<Integer, Integer> dimensions = null;
        try {
            inputStream = getInputStream();
            dimensions = BitmapUtil.decodeDimensions(inputStream);
            if (dimensions != null) {
                mWidth = dimensions.first;
                mHeight = dimensions.second;
            }
        }finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Head in the sand
                }
            }
        }
        return dimensions;
    }

    /**
     * 拷贝其他EncodedImage的元数据
     * Copy the meta data from another EncodedImage.
     *
     * @param encodedImage the EncodedImage to copy the meta data from.
     */
    public void copyMetaDataFrom(EncodedImage encodedImage) {
        mImageFormat = encodedImage.getImageFormat();
        mWidth = encodedImage.getWidth();
        mHeight = encodedImage.getHeight();
        mRotationAngle = encodedImage.getRotationAngle();
        mSampleSize = encodedImage.getSampleSize();
        mStreamSize = encodedImage.getSize();
        mEncodedCacheKey = encodedImage.getEncodedCacheKey();
    }

    /**
     * 如果所有的image信息都已经被载入的话，那么返回true
     * Returns true if all the image information has loaded, false otherwise.
     */
    public static boolean isMetaDataAvailable(EncodedImage encodedImage) {
        return encodedImage.mRotationAngle >= 0
                && encodedImage.mWidth >= 0
                && encodedImage.mHeight >= 0;
    }

    /**
     * Closes the encoded image handling null.
     *
     * @param encodedImage the encoded image to close.
     */
    public static void closeSafely(@Nullable EncodedImage encodedImage) {
        if (encodedImage != null) {
            encodedImage.close();
        }
    }

    /**
     * Checks if the encoded image is valid i.e. is not null, and is not closed.
     * @return true if the encoded image is valid
     */
    public static boolean isValid(@Nullable EncodedImage encodedImage) {
        return encodedImage != null && encodedImage.isValid();
    }

    /**
     * A test-only method to get the underlying references.
     *
     * <p><b>DO NOT USE in application code.</b>
     */
    @VisibleForTesting
    public synchronized SharedReference<PooledByteBuffer> getUnderlyingReferenceTestOnly() {
        return (mPooledByteBufferRef != null) ?
                mPooledByteBufferRef.getUnderlyingReferenceTestOnly() : null;
    }
}
