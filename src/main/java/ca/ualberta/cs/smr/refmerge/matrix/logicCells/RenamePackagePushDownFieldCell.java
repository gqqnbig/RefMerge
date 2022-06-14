package ca.ualberta.cs.smr.refmerge.matrix.logicCells;

import ca.ualberta.cs.smr.refmerge.refactoringObjects.PushDownFieldObject;
import ca.ualberta.cs.smr.refmerge.refactoringObjects.RefactoringObject;
import ca.ualberta.cs.smr.refmerge.refactoringObjects.RenamePackageObject;

public class RenamePackagePushDownFieldCell {

    public static boolean checkCombination(RefactoringObject dispatcher, RefactoringObject receiver) {
        PushDownFieldObject dispatcherObject = (PushDownFieldObject) dispatcher;
        RenamePackageObject receiverObject = (RenamePackageObject) receiver;

        String dispatcherOriginalClassName = dispatcherObject.getOriginalClass();
        String dispatcherRefactoredClassName = dispatcherObject.getTargetSubClass();
        String receiverOriginalPackageName = receiverObject.getOriginalName();
        String receiverDestinationPackageName = receiverObject.getDestinationName();

        // Need to check both cases
        boolean isCombination = false;
        // If the source method's package is renamed after the push down method refactoring
        if(dispatcherOriginalClassName.contains(receiverOriginalPackageName)) {
            String refactoredClassName = dispatcherRefactoredClassName.substring(dispatcherOriginalClassName.lastIndexOf("."));
            // Update the classes package
            ((PushDownFieldObject) dispatcher).setOriginalClass(receiverDestinationPackageName + refactoredClassName);
            isCombination = true;
        }

        // If the pulled up method's package is renamed after the push down method refactoring
        if(dispatcherRefactoredClassName.contains(receiverOriginalPackageName)) {
            String refactoredClassName = dispatcherRefactoredClassName.substring(dispatcherRefactoredClassName.lastIndexOf("."));
            // Update the classes package
            ((PushDownFieldObject) dispatcher).setTargetSubClass(receiverDestinationPackageName + refactoredClassName);
            isCombination = true;
        }
        return isCombination;
    }

}
