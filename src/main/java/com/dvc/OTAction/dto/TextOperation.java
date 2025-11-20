package com.dvc.OTAction.dto;

import java.util.List;

/**
 * Represents an operation on a text document, similar to ot.js TextOperation.
 * Operations consist of a list of ops:
 * - Positive integer: Retain (skip) characters.
 * - String: Insert characters.
 * - Negative integer: Delete characters.
 * - ye ek norm hai jo many docks follow...
 */

public class TextOperation {
    private List<Object> ops;
    private int baseLength;
    private int targetLength;

    public List<Object> getOps() {
        return ops;
    }

    public void setOps(List<Object> ops) {
        this.ops = ops;
    }

    public int getBaseLength() {
        return baseLength;
    }

    public void setBaseLength(int baseLength) {
        this.baseLength = baseLength;
    }

    public int getTargetLength() {
        return targetLength;
    }

    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }

    public static boolean isRetain(Object op){
        return op instanceof Integer && (Integer)op > 0;
    }

    public static boolean isInsert(Object op) {
        return op instanceof String;
    }
    public static boolean isDelete(Object op) {
        return op instanceof Integer && (Integer)op < 0;
    }

}
