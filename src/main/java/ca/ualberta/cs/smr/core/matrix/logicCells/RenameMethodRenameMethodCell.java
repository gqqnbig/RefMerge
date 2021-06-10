package ca.ualberta.cs.smr.core.matrix.logicCells;

import ca.ualberta.cs.smr.core.refactoringObjects.MethodSignatureObject;
import ca.ualberta.cs.smr.core.refactoringObjects.RefactoringObject;
import ca.ualberta.cs.smr.core.refactoringObjects.RenameMethodObject;
import ca.ualberta.cs.smr.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;

import static ca.ualberta.cs.smr.utils.MatrixUtils.*;

/*
 * Contains the logic checks for rename method/rename method conflicts.
 */
public class RenameMethodRenameMethodCell {
    final private Project project;

    public RenameMethodRenameMethodCell(Project project) {
        this.project = project;
    }

    /*
     *  Check if a rename method refactoring conflicts with a second rename method refactoring. Rename method/rename method
     *  can result in an override conflict, an overload conflict, or a naming conflict.
     */
    public boolean renameMethodRenameMethodConflictCell(RefactoringObject dispatcherObject, RefactoringObject receiverObject) {
        RenameMethodRenameMethodCell renameMethodRenameMethodCell = new RenameMethodRenameMethodCell(project);
        // Check for a method override conflict
        if(renameMethodRenameMethodCell.checkOverrideConflict(dispatcherObject, receiverObject)) {
            System.out.println("Override conflict");
            return true;
        }
        // Check for method overload conflict
        else if(renameMethodRenameMethodCell.checkOverloadConflict(dispatcherObject, receiverObject)) {
            System.out.println("Overload conflict");
            return true;
        }
        // Check for naming conflict

        else if(renameMethodRenameMethodCell.checkMethodNamingConflict(dispatcherObject, receiverObject)) {
            System.out.println("Naming conflict");
            return true;
        }
        return false;
    }

    /*
     * If two methods are renamed to the same name with a different signature in classes that have an inheritance relationship,
     * then they were likely part of an accidental override
     */
    public boolean checkOverrideConflict(RefactoringObject dispatcherObject, RefactoringObject receiverObject) {
        RenameMethodObject dispatcherRenameMethod = ((RenameMethodObject) dispatcherObject);
        RenameMethodObject receiverRenameMethod = ((RenameMethodObject) receiverObject);
        // Get the original operations
        MethodSignatureObject dispatcherOriginalMethod = dispatcherRenameMethod.getOriginalMethodSignature();
        MethodSignatureObject receiverOriginalMethod = receiverRenameMethod.getOriginalMethodSignature();
        // Get the refactored operations
        MethodSignatureObject dispatcherDestinationMethod = dispatcherRenameMethod.getDestinationMethodSignature();
        MethodSignatureObject receiverDestinationMethod = receiverRenameMethod.getDestinationMethodSignature();
        // Get the class names
        String dispatcherClassName = dispatcherRenameMethod.getOriginalClassName();
        String receiverClassName = receiverRenameMethod.getOriginalClassName();

        // If the rename methods happen in the same class then there is no override conflict
        if(dispatcherClassName.equals(receiverClassName)) {
            return false;
        }
        String dispatcherFile = dispatcherRenameMethod.getOriginalFilePath();
        String receiverFile = receiverRenameMethod.getOriginalFilePath();
        Utils utils = new Utils(project);
        PsiClass psiDispatcher = utils.getPsiClassByFilePath(dispatcherFile, dispatcherClassName);
        PsiClass psiReceiver = utils.getPsiClassByFilePath(receiverFile, receiverClassName);
        if(!ifClassExtends(psiDispatcher, psiReceiver)) {
            return false;
        }
        // Get original method names
        String dispatcherOriginalMethodName = dispatcherOriginalMethod.getName();
        String receiverOriginalMethodName = receiverOriginalMethod.getName();
        // get new method names
        String dispatcherNewMethodName = dispatcherDestinationMethod.getName();
        String receiverNewMethodName = receiverDestinationMethod.getName();
        // Check if the methods start with the same name and end with different names, or if they end with the same name
        // and start with different names. If they do, then there's a likely override conflict.
        return !isSameName(dispatcherOriginalMethodName, receiverOriginalMethodName) &&
                isSameName(dispatcherNewMethodName, receiverNewMethodName) &&
                dispatcherDestinationMethod.equalsSignature(receiverDestinationMethod);
    }

    /*
     * Check if both branches renamed two methods with different signatures to the same name. If they did, this is a
     * possible accidental overloading conflict.
     */
    public boolean checkOverloadConflict(RefactoringObject dispatcherObject, RefactoringObject receiverObject) {
        RenameMethodObject dispatcherRenameMethod = ((RenameMethodObject) dispatcherObject);
        RenameMethodObject receiverRenameMethod = ((RenameMethodObject) receiverObject);
        // Get the original operations
        MethodSignatureObject dispatcherOriginalMethod = dispatcherRenameMethod.getOriginalMethodSignature();
        MethodSignatureObject receiverOriginalMethod = receiverRenameMethod.getOriginalMethodSignature();
        // Get the refactored operations
        MethodSignatureObject dispatcherDestinationMethod = dispatcherRenameMethod.getDestinationMethodSignature();
        MethodSignatureObject receiverDestinationMethod = receiverRenameMethod.getDestinationMethodSignature();
        // Get class names
        String dispatcherClassName = dispatcherRenameMethod.getOriginalClassName();
        String receiverClassName = receiverRenameMethod.getOriginalClassName();
        // If the methods are in different classes, no overloading happens
        if (!dispatcherClassName.equals(receiverClassName)) {
            Utils utils = new Utils(project);
            String dispatcherFile = dispatcherRenameMethod.getOriginalFilePath();
            String receiverFile = receiverRenameMethod.getOriginalFilePath();
            PsiClass psiDispatcher = utils.getPsiClassByFilePath(dispatcherFile, dispatcherClassName);
            PsiClass psiReceiver = utils.getPsiClassByFilePath(receiverFile, receiverClassName);
            if(!ifClassExtends(psiDispatcher, psiReceiver)) {
                return false;
            }
        }
        String dispatcherOriginalMethodName = dispatcherOriginalMethod.getName();
        String dispatcherDestinationMethodName = dispatcherDestinationMethod.getName();
        String receiverOriginalMethodName = receiverOriginalMethod.getName();
        String receiverDestinationMethodname = receiverDestinationMethod.getName();
        // If two methods with different signatures are renamed to the same method, this overloading conflict
        return (!dispatcherOriginalMethodName.equals(receiverOriginalMethodName) &&
                dispatcherDestinationMethodName.equals(receiverDestinationMethodname)) &&
                !dispatcherDestinationMethod.equalsSignature(receiverDestinationMethod);
    }

    /*
     * Check for two methods being renamed to the same name or one method being renamed to two different names.
     */
    public boolean checkMethodNamingConflict(RefactoringObject dispatcherObject, RefactoringObject receiverObject) {
        // Use the original class name because they will have different class names if a class was renamed on one branch
        String dispatcherClassName = ((RenameMethodObject) dispatcherObject).getOriginalClassName();
        String receiverClassName = ((RenameMethodObject) receiverObject).getOriginalClassName();
        if(!dispatcherClassName.equals(receiverClassName)) {
            return false;
        }
        // We already checked for overriding and overloading so we can just use the name instead of the full
        // signature
        String dispatcherOriginalName = ((RenameMethodObject) dispatcherObject).getOriginalMethodSignature().getName();
        String receiverOriginalName = ((RenameMethodObject) receiverObject).getOriginalMethodSignature().getName();
        String dispatcherDestinationName = ((RenameMethodObject) dispatcherObject).getDestinationMethodSignature().getName();
        String receiverDestinationName = ((RenameMethodObject) receiverObject).getDestinationMethodSignature().getName();

        return checkNamingConflict(dispatcherOriginalName, receiverOriginalName,
                dispatcherDestinationName, receiverDestinationName);

    }

    /*
     * Check if the second refactoring is a transitive refactoring of the first refactoring.
     */
    public boolean checkRenameMethodRenameMethodTransitivity(RefactoringObject firstRefactoring,
                                                             RefactoringObject secondRefactoring) {
        boolean isTransitive = false;
        RenameMethodObject firstObject = (RenameMethodObject) firstRefactoring;
        RenameMethodObject secondObject = (RenameMethodObject) secondRefactoring;
        String firstDestinationClass = firstObject.getDestinationClassName();
        MethodSignatureObject firstDestinationMethod = firstObject.getDestinationMethodSignature();
        String secondOriginalClass = secondObject.getOriginalClassName();
        MethodSignatureObject secondOriginalMethod = secondObject.getOriginalMethodSignature();
        String secondDestinationClass = secondObject.getDestinationClassName();
        // If the renamed method of the first refactoring and original method of the second refactoring are the same
        if(firstDestinationClass.equals(secondOriginalClass) && firstDestinationMethod.equalsSignature(secondOriginalMethod)) {
            //This is a transitive refactoring
            isTransitive = true;
            firstRefactoring.setDestinationFilePath(secondObject.getDestinationFilePath());
            ((RenameMethodObject) firstRefactoring).setDestinationClassName(secondObject.getDestinationClassName());
            ((RenameMethodObject) firstRefactoring).setDestinationMethodSignature(secondObject.getDestinationMethodSignature());
        }
        else if(firstDestinationClass.equals(secondDestinationClass) && firstDestinationMethod.equalsSignature(secondOriginalMethod)) {
            isTransitive = true;
            firstRefactoring.setDestinationFilePath(secondObject.getDestinationFilePath());
            ((RenameMethodObject) firstRefactoring).setDestinationClassName(secondObject.getDestinationClassName());
            ((RenameMethodObject) firstRefactoring).setDestinationMethodSignature(secondObject.getDestinationMethodSignature());
        }

        return isTransitive;
    }

}
