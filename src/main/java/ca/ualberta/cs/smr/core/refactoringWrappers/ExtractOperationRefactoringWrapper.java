package ca.ualberta.cs.smr.core.refactoringWrappers;

import com.intellij.psi.PsiStatement;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;

import java.util.List;

public class ExtractOperationRefactoringWrapper extends ExtractOperationRefactoring {
    private final PsiStatement[] surroundingStatemets;

    public ExtractOperationRefactoringWrapper(UMLOperationBodyMapper bodyMapper,
                                              UMLOperation sourceOperationAfterExtraction,
                                              List<OperationInvocation> operationInvocations,
                                              PsiStatement[] surroundingStatemets) {
        super(bodyMapper, sourceOperationAfterExtraction, operationInvocations);
        this.surroundingStatemets = surroundingStatemets;
    }
}
