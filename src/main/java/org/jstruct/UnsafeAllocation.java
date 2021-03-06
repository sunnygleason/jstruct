/* =====================================================================
 *
 * Copyright (c) 2006 Dain Sundstrom.  All rights reserved.
 *
 * =====================================================================
 */
package org.jstruct;

import static org.jstruct.Allocator.*;
import sun.misc.Unsafe;

public class UnsafeAllocation implements Allocation
{
    private final Unsafe unsafe;
    public final long address;
    public final long size;
    private final boolean checkBounds;
    private final BlockCopy blockCopy;
    private boolean released;

    public UnsafeAllocation(long address, long size)
    {
        this(UnsafeAllocator.unsafe, UnsafeAllocator.blockCopy, address, size, true);
    }
    
    public UnsafeAllocation(long address, long size, boolean isCheckBounds)
    {
        this(UnsafeAllocator.unsafe, UnsafeAllocator.blockCopy, address, size, isCheckBounds);
    }

    public UnsafeAllocation(Unsafe unsafe, BlockCopy blockCopy, long address, long size, boolean isCheckBounds)
    {
        if (unsafe == null) {
            throw new NullPointerException("unsafe is null");
        }
        if (blockCopy == null) {
            throw new NullPointerException("blockCopy is null");
        }
        if (address <= 0) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
        if (address + size < size) {
            throw new IllegalArgumentException("Address + size is greater than 64 bits");
        }

        this.unsafe = unsafe;
        this.blockCopy = blockCopy;
        this.address = address;
        this.size = size;
        this.checkBounds = isCheckBounds;
    }

    public void free() {
        if (checkBounds && released) {
            throw new IllegalStateException("Memory has already been released");
        }
        released = true;
        unsafe.freeMemory(address);
    }

    public Allocation reallocate(long size) throws IllegalArgumentException, OutOfMemoryError {
        checkReleased();
        if (size < 0) {
            throw new IllegalArgumentException("Size is negative: " + size);
        }

        // If size is unchanged, simply return the same pointer instance
        if (size == this.size) return this;

        // Allocating zero bytes, results in a pointer to null
        if (size == 0) {
            free();
            return NULL_POINTER;
        }

        // reallocate the memory
        long newAddress = unsafe.reallocateMemory(address, size);

        // this pointer has been released... address may not have changed, but the old size in the pointer is now invalid
        released = true;

        // return a new pointer
        return new UnsafeAllocation(unsafe, blockCopy, newAddress, size, checkBounds);
    }

    public long getAddress() {
        return address;
    }

    public long size() {
        return size;
    }

    public byte getByte(long offset) {
        checkBounds(offset, 1);
        long location = address + offset;
        return unsafe.getByte(location);
    }

    public void putByte(long offset, byte value) {
        checkBounds(offset, 1);
        long location = address + offset;
        unsafe.putByte(location, value);
    }

    public Region getRegion(long offset)
    {
        checkBounds(offset, 1);
        return new SubRegion(this, offset, size - offset);
    }

    public Region getRegion(long offset, long length)
    {
        checkBounds(offset, length);
        return new SubRegion(this, offset, length);
    }

    public byte[] getBytes(long srcOffset, int length) {
        byte[] bytes = new byte[length];
        getBytes(srcOffset, bytes, 0, length);
        return bytes;
    }

    public void getBytes(long srcOffset, byte[] target) {
        getBytes(srcOffset, target, 0, target.length);
    }

    public void getBytes(long srcOffset, byte[] target, int targetOffset, int length) {
        checkBounds(srcOffset, length);
        if (checkBounds) AllocatorUtil.checkBounds(target.length, targetOffset, length);
        long srcLocation = address + srcOffset;

//        // this assumes that the target byte array will not move during the next two lines
//        // in Java7 this should use the double-register version of copyMemory
//        long targetAddress = unsafe.getLong(new Object[] {target}, OBJECT_ARRAY_BASE_OFFSET);
//        long targetLocation = targetAddress + BYTE_ARRAY_BASE_OFFSET + targetOffset;
//        unsafe.copyMemory(srcLocation, targetLocation, length);

        blockCopy.getBytes(srcLocation, target, targetOffset, length);
    }

    public void putBytes(long targetOffset, byte[] src) {
        putBytes(targetOffset, src, 0, src.length);
    }

    public void putBytes(long targetOffset, byte[] src, int srcOffset, int length) {
        checkBounds(targetOffset, length);
        if (checkBounds) AllocatorUtil.checkBounds(src.length, srcOffset, length);
        long targetLocation = address + targetOffset;

//        // this assumes that the target byte array will not move during the next two lines
//        // in Java7 this should use the double-register version of copyMemory
//        long srcAddress = unsafe.getLong(new Object[] {src}, OBJECT_ARRAY_BASE_OFFSET);
//        long srcLocation = srcAddress + BYTE_ARRAY_BASE_OFFSET + srcOffset;
//        unsafe.copyMemory(srcLocation, targetLocation, length);

        blockCopy.putBytes(targetLocation, src, srcOffset, length);
    }

    public short getShort(long offset) {
        checkBounds(offset, SHORT_SIZE);
        long location = address + offset;
        return unsafe.getShort(location);
    }

    public void putShort(long offset, short value) {
        checkBounds(offset, SHORT_SIZE);
        long location = address + offset;
        unsafe.putShort(location, value);
    }

    public char getChar(long offset) {
        checkBounds(offset, CHAR_SIZE);
        long location = address + offset;
        return unsafe.getChar(location);
    }

    public void putChar(long offset, char value) {
        checkBounds(offset, CHAR_SIZE);
        long location = address + offset;
        unsafe.putChar(location, value);
    }

    public int getInt(long offset) {
        checkBounds(offset, INT_SIZE);
        long location = address + offset;
        return unsafe.getInt(location);
    }

    public void putInt(long offset, int value) {
        checkBounds(offset, INT_SIZE);
        long location = address + offset;
        unsafe.putInt(location, value);
    }

    public long getLong(long offset) {
        checkBounds(offset, LONG_SIZE);
        long location = address + offset;
        return unsafe.getLong(location);
    }

    public void putLong(long offset, long value) {
        checkBounds(offset, LONG_SIZE);
        long location = address + offset;
        unsafe.putLong(location, value);
    }

    public float getFloat(long offset) {
        checkBounds(offset, FLOAT_SIZE);
        long location = address + offset;
        return unsafe.getFloat(location);
    }

    public void putFloat(long offset, float value) {
        checkBounds(offset, FLOAT_SIZE);
        long location = address + offset;
        unsafe.putFloat(location, value);
    }

    public double getDouble(long offset) {
        checkBounds(offset, DOUBLE_SIZE);
        long location = address + offset;
        return unsafe.getDouble(location);
    }

    public void putDouble(long offset, double value) {
        checkBounds(offset, DOUBLE_SIZE);
        long location = address + offset;
        unsafe.putDouble(location, value);
    }

    public void setMemory(byte value) {
        checkReleased();
        unsafe.setMemory(address, size, value);
    }

    public void setMemory(long offset, long size, byte value) {
        checkBounds(offset, size);
        long location = address + offset;
        unsafe.setMemory(location, size, value);
    }

    public void copyMemory(long offset, long targetAddress, long size) {
        checkBounds(offset, size);
        long location = address + offset;
        unsafe.copyMemory(location, targetAddress, size);
    }

    public void copyMemory(long srcOffset, Region target) {
        copyMemory(srcOffset, target, 0, target.size());
    }

    public void copyMemory(long srcOffset, Region target, long targetOffset, long size) {
        if (!(target instanceof UnsafeAllocation)) {
            AllocatorUtil.copyMemoryByteByByte(this, srcOffset, target, targetOffset, size);
            return;
        }
        UnsafeAllocation unsafeMemory = (UnsafeAllocation) target;
        
        checkBounds(srcOffset, size);
        unsafeMemory.checkBounds(targetOffset, size);
        long location = address + srcOffset;
        long targetLocation = unsafeMemory.address + targetOffset;
        unsafe.copyMemory(location, targetLocation, size);
    }

    public int compareMemory(long srcOffset, Region target, long targetOffset, long size) {
        return AllocatorUtil.compareMemory(this, srcOffset, target, targetOffset, size);
    }

    public boolean isInBounds(long offset, long length) {
        if (!AllocatorUtil.isInBounds(size, offset, length)) {
            return false;
        }
        if (address + offset + length < length) {
            // Address + offset + length is greater than 64 bits
            return false;            
        }
        return true;
    }

    public void checkBounds(long offset, long length) {
        if (checkBounds) {
            checkReleased();
            AllocatorUtil.checkBounds(size, offset, length);
            if (address + offset + length < length) {
                throw new IllegalArgumentException("Address + offset + length is greater than 64 bits: address=" + address + ", offset=" + offset + ", length=" + length);
            }
        }
    }

    private void checkReleased() {
        if (checkBounds) {
            if (released) {
                throw new IllegalStateException("UnsafePointer has already been freed");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnsafeAllocation memory = (UnsafeAllocation) o;

        return address == memory.address && size == memory.size;
    }

    @Override
    public int hashCode() {
        int result = (int) (address ^ (address >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }

    public String toString() {
        return "UnsafeAllocation{" +
                "address=" + address +
                ", size=" + size +
                (!checkBounds ? ", CHECKS DISABLED" : "") +
                '}';
    }
}