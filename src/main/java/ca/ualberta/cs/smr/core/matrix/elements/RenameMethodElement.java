package ca.ualberta.cs.smr.core.matrix.elements;

import ca.ualberta.cs.smr.core.matrix.visitors.Visitor;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import org.refactoringminer.api.Refactoring;

import static ca.ualberta.cs.smr.core.matrix.logicHandlers.ConflictCheckers.checkOverrideConflict;

/*
 * Checks if visitor refactorings confict with a rename method refactoring.
 */
public class RenameMethodElement extends RefactoringElement {
    Refactoring elementRef;

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }

    public void set(Refactoring ref) {
        elementRef = ref;
    }

    /*
     *  Check if a rename method refactoring conflicts with a second rename method refactoring.
     */
    public boolean checkRenameMethodConflict(Refactoring visitorRef) {
        // Get the original and renamed UMLOperation for each refactoring
        UMLOperation leftOriginalOperation = ((RenameOperationRefactoring) elementRef).getOriginalOperation();
        UMLOperation rightOriginalOperation = ((RenameOperationRefactoring) visitorRef).getOriginalOperation();
        UMLOperation leftOperation = ((RenameOperationRefactoring) elementRef).getRenamedOperation();
        UMLOperation rightOperation = ((RenameOperationRefactoring) visitorRef).getRenamedOperation();
        // Get the name of the methods before they are refactored
        String originalLeftName = leftOriginalOperation.getName();
        String originalRightName = rightOriginalOperation.getName();
        // Get the name of the methods after they are refactored
        String leftName = leftOperation.getName();
        String rightName = rightOperation.getName();
        // Get the names of the classes that the methods are in
        String leftClass = leftOperation.getClassName();
        String rightClass = rightOperation.getClassName();


        // Check for a method override conflict
        if(checkOverrideConflict(elementRef, visitorRef)) {
            return true;
        }

        // If the methods have the same name and different parameters in the same class, check for overloading
        else if(originalLeftName.equals(originalRightName) && !leftName.equals(rightName) &&
                !leftOperation.equalParameters(rightOperation)) {
            System.out.println("Overloading Conflict");
            return true;
        }
        // If the original method names are equal but the destination names are not equal, check for conflict
        else if(originalLeftName.equals(originalRightName) && !leftName.equals(rightName)) {
            return true;
        }
        // If the original method names are not equal but the destination names are equal
        else if(!originalLeftName.equals(originalRightName) && leftName.equals(rightName)) {
            return true;
        }

        return false;
    }


}
