package com.dvc.OTAction.debug;

import com.dvc.OTAction.dto.TextOperation;
import com.dvc.OTAction.utils.OTUtils;

import java.util.List;

public class TestTransform {
    public static void main(String[] args) {

        String doc = "ABCD";
        System.out.println("Original Document = " + doc);

        // ======== Create Operation 1: delete("C") ========
        TextOperation op1 = new TextOperation();
        op1.retain(2);  // keep "AB"
        op1.delete(-1);  // delete "C"
        op1.retain(1);  // keep "D"

        // ======== Create Operation 2: delete("BC") ========
        TextOperation op2 = new TextOperation();
        op2.retain(1);  // keep "A"
        op2.delete(-2);  // delete "BC"
        op2.retain(1);  // keep "D"

        System.out.println("Operation 1 ops: " + op1.getOps());
        System.out.println("Operation 2 ops: " + op2.getOps());
        System.out.println("op1 baseLength=" + op1.getBaseLength());
        System.out.println("op2 baseLength=" + op2.getBaseLength());
        System.out.println();

        // ======== Run transform() ========
        try {
            String result1 = OTUtils.apply(doc, op1);       // apply op1 first
            String result2 = OTUtils.apply(doc, op2);       // apply op2 first

            List<TextOperation> result = OTUtils.transform(op1, op2);

            TextOperation op1Prime = result.get(0);
            TextOperation op2Prime = result.get(1);

            System.out.println("=== TRANSFORM RESULT ===");
            System.out.println("op1' : " + op1Prime.getOps());
            System.out.println("op2' : " + op2Prime.getOps());
            System.out.println("op1' baseLength=" + op1Prime.getBaseLength() +
                    " targetLength=" + op1Prime.getTargetLength());
            System.out.println("op2' baseLength=" + op2Prime.getBaseLength() +
                    " targetLength=" + op2Prime.getTargetLength());
            System.out.println();

            // ============================================================
            // APPLY OPERATIONS TO CHECK OT CONSISTENCY
            // ============================================================
            result1 = OTUtils.apply(result1, op2Prime);     // then apply op2'

            result2 = OTUtils.apply(result2, op1Prime);     // then apply op1'

            System.out.println("=== APPLYING OPERATIONS TO DOCUMENT ===");
            System.out.println("apply(op1, op2') => " + result1);
            System.out.println("apply(op2, op1') => " + result2);

            if (result1.equals(result2)) {
                System.out.println("✔ OT Correct: Both results match");
            } else {
                System.out.println("❌ OT WRONG: Results differ");
            }

        } catch (Exception e) {
            System.err.println("Transform error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
