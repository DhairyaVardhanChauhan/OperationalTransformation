package com.dvc.OTAction.utils;

import com.dvc.OTAction.dto.TextOperation;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Text;

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

}
