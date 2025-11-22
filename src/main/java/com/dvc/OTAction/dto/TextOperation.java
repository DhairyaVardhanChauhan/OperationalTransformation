package com.dvc.OTAction.dto;

import java.util.ArrayList;
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
    private int baseLength; //how many characters the operation consumes from original doc
    private int targetLength; //how many characters the resulting doc will contain

    public TextOperation() {
        this.ops = new ArrayList<>();
        this.baseLength = 0;
        this.targetLength = 0;
    }

    public TextOperation(List<Object> ops) {
        this();
        for (Object op : ops) {
            if (isRetain(op)) {
                this.retain((Integer) op);
            } else if (isInsert(op)) {
                this.insert((String) op);
            } else if (isDelete(op)) {
                this.delete((Integer) op); // Use the negative value directly
            } else {
                throw new IllegalArgumentException("Unknown operation type in list: " + op);
            }
        }
    }


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


    /**
     * Add's an operation to the TextOperationList, if the last operation was also of type retain then merge happens
     */

    public TextOperation retain(int n) {
        if(n < 0){
            throw new IllegalArgumentException("Retain Count must be greater than 0");
        }
        else if(n == 0){
            return this;
        }
        this.baseLength+= n;
        this.targetLength += n; // merge operations for optimizations.
        if(!this.ops.isEmpty() && isRetain(this.ops.get(this.ops.size() - 1))){
            int lastOperation = (Integer) this.ops.get(this.ops.size()-1);
            int netShift = lastOperation + n;
            this.ops.set(this.ops.size()-1,netShift);
        }
        else{
            this.ops.add(n);
        }
        return this;
    }

    public TextOperation delete(int n) {

        if(n == 0){
            return this;
        }
        int deleteCnt = (n > 0) ? -n : n;
        this.baseLength -= deleteCnt;
        if (!this.ops.isEmpty() && isDelete(this.ops.get(this.ops.size() - 1))) {
            int lastOp = (Integer) this.ops.get(this.ops.size() - 1);
            this.ops.set(this.ops.size() - 1, lastOp + deleteCnt);
        } else {
            this.ops.add(deleteCnt);
        }
        return this;
    }

    public TextOperation insert(String str) {
        if(str == null || str.isEmpty()){
            return this;
        }
        this.targetLength+= str.length();
        if (!this.ops.isEmpty() && isInsert(this.ops.get(this.ops.size() - 1))) { // prev operation is also insert, merge
            String lastOp = (String) this.ops.get(this.ops.size() - 1);
            this.ops.set(this.ops.size() - 1, lastOp + str);
        }
        else if(!this.ops.isEmpty() && isDelete(this.ops.get(this.ops.size()-1))){
            if(this.ops.size() >= 2 && isInsert(this.ops.get(this.ops.size()-2))){
                String secondLastOp = (String) this.ops.get(this.ops.size() - 2);
                this.ops.set(this.ops.size() - 2, secondLastOp + str);
            }
            else{
                Object lastOp = this.ops.remove(this.ops.size() - 1);
                this.ops.add(str);
                this.ops.add(lastOp);
            }
        }
        else{
            this.ops.add(str);
        }
        return this;
    }
}
