package ca.ualberta.cs.smr.refmerge.matrix.logicCells;

import ca.ualberta.cs.smr.refmerge.refactoringObjects.PushDownFieldObject;
import ca.ualberta.cs.smr.refmerge.refactoringObjects.RefactoringObject;
import ca.ualberta.cs.smr.refmerge.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;

import static ca.ualberta.cs.smr.refmerge.utils.MatrixUtils.ifClassExtends;
import static ca.ualberta.cs.smr.refmerge.utils.MatrixUtils.isSameName;

public class PushDownFieldPushDownFieldCell {
    Project project;

    public PushDownFieldPushDownFieldCell(Project project) {
        this.project = project;
    }

    public boolean conflictCell(RefactoringObject dispatcher, RefactoringObject receiver) {
        PushDownFieldObject dispatcherObject = (PushDownFieldObject) dispatcher;
        PushDownFieldObject receiverObject = (PushDownFieldObject) receiver;
        // Shadow conflict
        if(shadowConflict(dispatcherObject, receiverObject)) {
            return true;
        }
        // Naming conflict
        return namingConflict(dispatcherObject, receiverObject);
    }

    public boolean shadowConflict(PushDownFieldObject dispatcherObject, PushDownFieldObject receiverObject) {

        String newDispatcherClass = dispatcherObject.getTargetSubClass();
        String newReceiverClass = receiverObject.getTargetSubClass();

        // Cannot have shadow conflict in same class
        if(newDispatcherClass.equals(newReceiverClass)) {
            return false;
        }

        String dispatcherFile = dispatcherObject.getDestinationFilePath();
        String receiverFile = receiverObject.getDestinationFilePath();
        Utils utils = new Utils(project);
        PsiClass psiDispatcher = utils.getPsiClassByFilePath(dispatcherFile, newDispatcherClass);
        PsiClass psiReceiver = utils.getPsiClassByFilePath(receiverFile, newReceiverClass);
        if(psiReceiver != null && psiDispatcher != null) {
            // If there is no inheritance relationship, there is no shadow conflict
            if (!ifClassExtends(psiDispatcher, psiReceiver)) {
                return false;
            }
        }

        String newDispatcherField = dispatcherObject.getRefactoredFieldName();
        String newReceiverField = receiverObject.getRefactoredFieldName();

        // The original name does not matter in this case
        return isSameName(newDispatcherField, newReceiverField);

    }

    public boolean namingConflict(PushDownFieldObject dispatcher, PushDownFieldObject receiver) {

        String dispatcherOriginalClass = dispatcher.getOriginalClass();
        String receiverOriginalClass = receiver.getOriginalClass();

        String dispatcherOriginalFieldName = dispatcher.getOriginalFieldName();
        String dispatcherDestinationFieldName = dispatcher.getRefactoredFieldName();
        String receiverOriginalFieldName = receiver.getOriginalFieldName();
        String receiverDestinationFieldName = receiver.getRefactoredFieldName();

        // If the fields are not pushed down from the same class, it cannot result in a naming conflict
        // because fields pushed down from the same class that are the same field have potential to be combined
        if(dispatcherOriginalClass.equals(receiverOriginalClass) && dispatcherOriginalFieldName.equals(receiverOriginalFieldName)) {
            return false;
        }


        // If the original field names are not the same and the new field names are not the same, there is no potential
        // for a naming conflict
        if(!dispatcherOriginalFieldName.equals(receiverOriginalFieldName)
                && !dispatcherDestinationFieldName.equals(receiverDestinationFieldName)) {
            return false;
        }

        // If the two fields are pushed down from two different classes, report a naming conflict
        return dispatcherOriginalFieldName.equals(receiverOriginalFieldName)
                && !dispatcherOriginalClass.equals(receiverOriginalClass);
    }

}
