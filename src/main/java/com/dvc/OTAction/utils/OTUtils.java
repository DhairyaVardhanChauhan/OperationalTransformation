package com.dvc.OTAction.utils;

import com.dvc.OTAction.dto.TextOperation;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.List;
@Configuration
public class OTUtils {

    /**
     * Apply an operation to a string, returning a new string.
     * Based on ot.js TextOperation.prototype.apply
     *
     * @param doc       The document string.
     * @param operation The operation to apply.
     * @return The resulting string.
     * @Work
     * If the operation is an insertion operation the doc insertes the x string,
     *
     * if the operation is delete, it forward deletes x characters by
     * simply shifting the cursor by x spaces and not copying them to the new document
     *
     * if the operation is retain then simply copy the existing string from the old doc to the new doc.
     *
     * @throws IllegalArgumentException If the operation's base length doesn't match the document length or if the operation is invalid.
     */

    public static String apply(String doc, TextOperation operation){
        StringBuilder newDoc = new StringBuilder();
        // start reading the doc. paheli line se padho, will use some prefix trick to optimize it in future
        int docIdx = 0;
        for(Object op:operation.getOps()){
            if(TextOperation.isRetain(op)){ // agar incoming operation is of type retain
                int retainCount = (Integer) op;
                if(docIdx + retainCount > doc.length()){
                    throw new IllegalArgumentException("Retain exceeds document length.");
                }
                newDoc.append(doc,docIdx,docIdx+retainCount);
                docIdx += retainCount;
            }
            else if(TextOperation.isInsert(op)){
                newDoc.append((String) op);
            }
            else if(TextOperation.isDelete(op)){
                int deleteCount = -(Integer) op;
                if(docIdx + deleteCount > doc.length()){
                    throw new IllegalArgumentException("Delete exceeds document length.");
                }
                docIdx += deleteCount;
            }
            else{
                throw new IllegalArgumentException("Invalid op type in operation: " + op);
            }
        }
        if(docIdx != doc.length()){
            throw new IllegalArgumentException("Operation did not consume the entire document.");
        }
        return newDoc.toString();
    }

    /**
     * Computes the inverse of an operation.
     * Based on ot.js TextOperation.prototype.invert
     *
     * @param doc       The original document string (used for inserts in the inverse).
     * @param operation The operation to invert.
     * @return The inverted operation.
     * @Work
     * Return the inverse of any operation. above (basically ctrl+Z functionality provide karti hai
     * since many users will be typing in the same  document, we cant just inverse the last operation
     * DocIdx is the position of the cursor in the old doc, and it represents how much of the doc is read.
     */
     public static TextOperation invert(String doc,TextOperation operation){
         TextOperation inverse = new TextOperation();
         int docIdx = 0;
         for(Object op: operation.getOps()){
             if(TextOperation.isRetain(op)){
                 int retainCount = (Integer) op;
                 inverse.retain(retainCount);
                 docIdx += retainCount;
             }
             else if(TextOperation.isInsert(op)){
                 inverse.delete(((String)op).length());
             }
             else if(TextOperation.isDelete(op)){
                 int deleteCnt = -(Integer)op;
                 inverse.insert(doc.substring(docIdx, docIdx + deleteCnt));
             }
         }
         return inverse;
     }

//     public static TextOperation compose(TextOperation op1, TextOperation op2){
//
//         if(op1.getTargetLength() != op2.getBaseLength()){
//             throw new IllegalArgumentException("Compose error: op1 target length (" + op1.getTargetLength() +
//                     ") must match op2 base length (" + op2.getBaseLength() + ").");
//         }
//
//         TextOperation composed = new TextOperation();
//         List<Object> ops1 = op1.getOps();
//         List<Object> ops2 = op2.getOps();
//         int i1 = 0;
//         int i2 = 0;
//         Object currOperation1 = (i1 < ops1.size())? ops1.get(i1++):null;
//         Object currOperation2 = (i2 < ops2.size())? ops2.get(i2++):null;
//
//         while(currOperation1 != null && currOperation2 != null){
//
//             if(TextOperation.isDelete(currOperation1)){
//                 composed.delete((Integer) currOperation1);
//                 currOperation1 = (i1 < ops1.size())?ops1.get(i1++):null;
//                 continue;
//             }
//             if(TextOperation.isInsert(currOperation2)){
//                 composed.insert((String)currOperation2);
//                 currOperation2 = (i2 < ops2.size())?ops2.get(i2++):null;
//                 continue;
//             }
//
//             if(currOperation1 == null){
//                 throw new IllegalArgumentException("Cannot compose: op2 is longer than op1 affects.");
//             }
//             if(currOperation2 == null){
//                 throw new IllegalArgumentException("Cannot compose: op1 is longer than op2 affects.");
//             }
//
//         }
//     }

    /**
     * ================================
     *  OPERATIONAL TRANSFORMATION (OT)
     * ================================
     *
     * This method transforms two operations (operation1 and operation2)
     * so they can both be applied safely to the same document **in any order**.
     *
     * Why is this needed?
     * -------------------
     * If User A and User B type at the same time, they both send edits
     * based on the *same old version* of the document.
     *
     * Example:
     *   A inserts "X" at index 0
     *   B inserts "Y" at index 0
     *
     * Without OT → conflict → inconsistent documents.
     * With OT → A' becomes insert("X"), B' becomes retain(1)+insert("Y")
     *
     * After OT, both operations can be applied sequentially with no conflict.
     *
     * This transform() implements the same algorithm as **ot.js**
     * (the canonical Google Wave OT implementation).
     *
     * RULE:
     * -----
     * Both operations MUST have the same baseLength.
     * Meaning: both operations must describe changes against the same document state.
     */
    public static List<TextOperation> transform(TextOperation operation1, TextOperation operation2) throws IllegalArgumentException {

        // ==============================================================
        // 0️⃣ PRE-CONDITION CHECK
        // Both operations must be based on the same document length.
        // If not, OT is impossible → throw error.
        // ==============================================================
        if (operation1.getBaseLength() != operation2.getBaseLength()) {
            throw new IllegalArgumentException(
                    String.format("Both operations have to have the same base length (op1: %d, op2: %d)",
                            operation1.getBaseLength(), operation2.getBaseLength()));
        }

        // ==============================================================
        // 1️⃣ Create the transformed result operations
        // After processing, operation1prime and operation2prime will represent
        // the edits AFTER being adjusted for each other's effects.
        // ==============================================================
        TextOperation operation1prime = new TextOperation();
        TextOperation operation2prime = new TextOperation();

        // Extract op lists for iteration
        List<Object> ops1 = operation1.getOps();
        List<Object> ops2 = operation2.getOps();

        // Pointers into both op lists
        int i1 = 0, i2 = 0;

        // Fetch first ops
        Object op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
        Object op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;

        // ==============================================================
        // 2️⃣ MAIN TRANSFORMATION LOOP
        // Keep transforming until we exhaust BOTH operations.
        // ==============================================================
        while (op1 != null || op2 != null) {

            if (op1 == null && op2 == null) { break; }

            // ==============================================================
            // CASE A — INSERTS ALWAYS GO FIRST
            // If either op inserts text, it "wins" against retain/delete.
            //
            // Why?
            // Because insertion does NOT consume any characters from the document.
            // ==============================================================
            if (TextOperation.isInsert(op1)) {
                // operation1 inserts → operation2 simply retains that many chars
                operation1prime.insert((String) op1);
                operation2prime.retain(((String) op1).length());

                // move to next op1
                op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                continue;
            }

            if (TextOperation.isInsert(op2)) {
                // operation2 inserts → operation1 must retain
                operation1prime.retain(((String) op2).length());
                operation2prime.insert((String) op2);

                op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                continue;
            }

            // ==============================================================
            // SAFETY CHECKS
            // If one op ends early unexpectedly → inconsistent operation stream
            // ==============================================================
            if (op1 == null) {
                throw new IllegalArgumentException("Cannot transform operations: first operation is too short.");
            }
            if (op2 == null) {
                throw new IllegalArgumentException("Cannot transform operations: second operation is too short.");
            }

            // ==============================================================
            // CASE B — RETAIN vs RETAIN
            // Both ops skip characters → easy!
            // ==============================================================
            int minLength;
            if (TextOperation.isRetain(op1) && TextOperation.isRetain(op2)) {

                int op1Retain = (Integer) op1;
                int op2Retain = (Integer) op2;

                if (op1Retain > op2Retain) {
                    minLength = op2Retain;
                    op1 = op1Retain - op2Retain;      // consume part of op1
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else if (op1Retain == op2Retain) {
                    minLength = op2Retain;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else { // op1Retain < op2Retain
                    minLength = op1Retain;
                    op2 = op2Retain - op1Retain;      // reduce op2 retain
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                }

                // Both transformed ops must retain the same chars
                operation1prime.retain(minLength);
                operation2prime.retain(minLength);
            }

            // ==============================================================
            // CASE C — DELETE vs DELETE
            // If both delete some section, no one "keeps" those characters.
            //
            // Important:
            // We DO NOT output anything because deleted text disappears anyway.
            // ==============================================================
            else if (TextOperation.isDelete(op1) && TextOperation.isDelete(op2)) {

                int op1Delete = (Integer) op1;
                int op2Delete = (Integer) op2;

                if (-op1Delete > -op2Delete) {
                    op1 = op1Delete - op2Delete;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else if (-op1Delete == -op2Delete) {
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else {
                    op2 = op2Delete - op1Delete;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                }
                // No insertion/retain emitted here
            }

            // ==============================================================
            // CASE D — DELETE (op1) vs RETAIN (op2)
            // Meaning: op1 deletes some characters, while op2 expects to retain them.
            //
            // The delete modifies what op2 sees — so op2 must shrink.
            // ==============================================================
            else if (TextOperation.isDelete(op1) && TextOperation.isRetain(op2)) {

                int op1Delete = (Integer) op1;
                int op2Retain = (Integer) op2;

                if (-op1Delete > op2Retain) {
                    minLength = op2Retain;
                    op1 = op1Delete + op2Retain; // still negative
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else if (-op1Delete == op2Retain) {
                    minLength = op2Retain;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else {
                    minLength = -op1Delete;
                    op2 = op2Retain + op1Delete;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                }

                operation1prime.delete(minLength);
            }

            // ==============================================================
            // CASE E — RETAIN (op1) vs DELETE (op2)
            // Same symmetric case as above.
            //
            // op2 deletes characters that op1 expected to keep.
            // So operation2prime deletes, but operation1prime must skip.
            // ==============================================================
            else if (TextOperation.isRetain(op1) && TextOperation.isDelete(op2)) {

                int op1Retain = (Integer) op1;
                int op2Delete = (Integer) op2;

                if (op1Retain > -op2Delete) {
                    minLength = -op2Delete;
                    op1 = op1Retain + op2Delete;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else if (op1Retain == -op2Delete) {
                    minLength = op1Retain;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                    op2 = (i2 < ops2.size()) ? ops2.get(i2++) : null;
                } else {
                    minLength = op1Retain;
                    op2 = op2Delete + op1Retain;
                    op1 = (i1 < ops1.size()) ? ops1.get(i1++) : null;
                }

                operation2prime.delete(minLength);
            }

            // ==============================================================
            // Unknown case → invalid OT sequence
            // ==============================================================
            else {
                throw new IllegalStateException("Unrecognized case in transform: op1=" + op1 + ", op2=" + op2);
            }
        }

        // Return transformed pair
        return Arrays.asList(operation1prime, operation2prime);
    }





}
