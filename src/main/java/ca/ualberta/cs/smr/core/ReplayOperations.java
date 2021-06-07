package ca.ualberta.cs.smr.core;

import ca.ualberta.cs.smr.core.refactoringObjects.ExtractOperationRefactoringWrapper;
import ca.ualberta.cs.smr.utils.Utils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.changeSignature.ThrownExceptionInfo;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.refactoring.util.duplicates.Match;
import com.intellij.usageView.UsageInfo;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ReplayOperations {

    Project project;

    public ReplayOperations(Project proj) {
        this.project = proj;
    }

    /*
     * replayRenameMethod performs the rename method refactoring.
     */
    public void replayRenameMethod(Refactoring ref) {
        UMLOperation original = ((RenameOperationRefactoring) ref).getOriginalOperation();
        UMLOperation renamed = ((RenameOperationRefactoring) ref).getRenamedOperation();
        String destName = renamed.getName();
        String qualifiedClass = renamed.getClassName();
        String filePath = renamed.getLocationInfo().getFilePath();
        Utils utils = new Utils(project);
        PsiClass psiClass = utils.getPsiClassFromClassAndFileNames(qualifiedClass, filePath);
        assert psiClass != null;
        PsiMethod method = Utils.getPsiMethod(psiClass, original);
        assert method != null;
        RefactoringFactory factory = JavaRefactoringFactory.getInstance(project);
        RenameRefactoring renameRefactoring = factory.createRename(method, destName, true, true);
        UsageInfo[] refactoringUsages = renameRefactoring.findUsages();
        renameRefactoring.doRefactoring(refactoringUsages);
        // Update the virtual file containing the refactoring
        VirtualFile vFile = psiClass.getContainingFile().getVirtualFile();
        vFile.refresh(false, true);
    }


    public void replayRenameClass(Refactoring ref) {

        UMLClass original = ((RenameClassRefactoring) ref).getOriginalClass();
        UMLClass renamed = ((RenameClassRefactoring) ref).getRenamedClass();
        String srcQualifiedClass = original.getName();
        String destQualifiedClass = renamed.getName();
        String destClassName = destQualifiedClass.substring(destQualifiedClass.lastIndexOf(".") + 1);
        Utils utils = new Utils(project);
        String filePath = original.getLocationInfo().getFilePath();
        PsiClass psiClass = utils.getPsiClassFromClassAndFileNames(srcQualifiedClass, filePath);
        if(psiClass == null) {
            return;
        }
        RefactoringFactory factory = JavaRefactoringFactory.getInstance(project);
        RenameRefactoring renameRefactoring = factory.createRename(psiClass, destClassName, true, true);
        UsageInfo[] refactoringUsages = renameRefactoring.findUsages();
        renameRefactoring.doRefactoring(refactoringUsages);
        // Update the virtual file of the class
        VirtualFile vFile = psiClass.getContainingFile().getVirtualFile();
        vFile.refresh(false, true);

    }

    public void replayExtractMethod(Refactoring ref) {
        ExtractOperationRefactoringWrapper refactoringWrapper = (ExtractOperationRefactoringWrapper) ref;
        ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
        UMLOperation sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
        UMLOperation extractedOperation = extractOperationRefactoring.getExtractedOperation();
        String refactoringName = extractedOperation.getName();
        String initialMethodName = sourceOperation.getName();
        String initialClassName = sourceOperation.getClassName();
        String filePath = sourceOperation.getLocationInfo().getFilePath();
        String helpId = "";

        Utils utils = new Utils(project);
        PsiClass psiClass = utils.getPsiClassFromClassAndFileNames(initialClassName, filePath);
        assert psiClass != null;
        PsiMethod psiMethod = Utils.getPsiMethod(psiClass, sourceOperation);
        assert psiMethod != null;

        PsiElement[] surroundingElements = refactoringWrapper.getSurroundingElements();
        PsiElement[] psiElements = getPsiElementsBetweenElements(surroundingElements);


        if(psiElements.length == 0) {
            Set<Replacement> replacements = extractOperationRefactoring.getReplacements();
            psiElements = useReplacements(replacements, psiMethod);
        }
        PsiType forcedReturnType = getPsiReturnType(extractOperationRefactoring, psiMethod);
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        ExtractMethodProcessor extractMethodProcessor = new ExtractMethodProcessor(project, editor, psiElements,
                forcedReturnType, refactoringName, initialMethodName, helpId);
        extractMethodProcessor.setMethodName(refactoringName);
        String visibility = extractedOperation.getVisibility();
        extractMethodProcessor.setMethodVisibility(visibility);
        try {
            extractMethodProcessor.prepare();
        } catch (PrepareFailedException e) {
            e.printStackTrace();
        }
        extractMethodProcessor.setDataFromInputVariables();
        ExtractMethodHandler.extractMethod(project, extractMethodProcessor);
        // Check for duplicates and prepare method signature
        if(extractMethodProcessor.initParametrizedDuplicates(false)) {
            // Handle duplicate extract method calls
            handleDuplicates(extractMethodProcessor, extractedOperation);
        }

        updateSignature(extractMethodProcessor, extractedOperation, refactoringName,
                forcedReturnType, refactoringWrapper.getThrownExceptionInfos());
        VirtualFile vFile = psiClass.getContainingFile().getVirtualFile();
        vFile.refresh(false, true);
    }

    /*
     * Extracts the duplicate extract method calls. After all duplicates have been extracted, it renames the
     * parameters to match what the developers expected after the merge.
     */
    private void handleDuplicates(ExtractMethodProcessor processor, UMLOperation operation) {
        final List<Match> duplicates = processor.getDuplicates();
        for (final Match match : duplicates) {
            if (!match.getMatchStart().isValid() || !match.getMatchEnd().isValid()) continue;
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            Runnable runnable = () -> ApplicationManager.getApplication().runWriteAction(() -> {
                processor.processMatch(match);
            });
            CommandProcessor.getInstance().executeCommand(project, runnable, "Extract Method", null);
        }
        PsiMethod extractedPsiMethod = processor.getExtractedMethod();
        renameParameters(extractedPsiMethod, operation);
    }

    /*
     * Update the extracted method signature to include the throws exception list and rearrange the parameters to be
     * in the correct order.
     */
    private void updateSignature(ExtractMethodProcessor processor, UMLOperation operation, String refactoringName,
                                 PsiType forcedReturnType, ThrownExceptionInfo[] thrownExceptionInfo) {
        PsiMethod extractedPsiMethod = processor.getExtractedMethod();
        if(extractedPsiMethod.getParameterList().getParametersCount() > 1) {
            ParameterInfoImpl[] parameterInfo = getParameterInfo(extractedPsiMethod, operation);
            // Temporary workaround until we rename the parameters in duplicates
            if(parameterInfo[0] != null) {
                if(thrownExceptionInfo == null) {
                    ChangeSignatureProcessor changeSignatureProcessor =
                            new ChangeSignatureProcessor(project, extractedPsiMethod, false, null,
                                    refactoringName, forcedReturnType, parameterInfo);
                    changeSignatureProcessor.run();
                }
                else {
                    ChangeSignatureProcessor changeSignatureProcessor =
                            new ChangeSignatureProcessor(project, extractedPsiMethod, false, null,
                                    refactoringName, forcedReturnType, parameterInfo, thrownExceptionInfo);
                    changeSignatureProcessor.run();
                }
            }
        }
    }

    /*
     * Use the new psi statement at the beginning and end of the extracted method to get all involved psi statements
     * in the refactoring.
     */
    private PsiElement[] getPsiElementsBetweenElements(PsiElement[] surroundingElements) {
        PsiElement firstElement = surroundingElements[0];
        PsiElement lastElement = surroundingElements[1];
        if(firstElement instanceof PsiJavaToken) {
            firstElement = firstElement.getNextSibling();
        }
        else {
            firstElement = firstElement.getNextSibling();
        }
        if(firstElement instanceof PsiWhiteSpace) {
            firstElement = firstElement.getNextSibling();
        }
        if(lastElement instanceof PsiJavaToken) {
            lastElement = lastElement.getPrevSibling();
        }
        else {
            lastElement = lastElement.getPrevSibling();
        }
        if(lastElement instanceof PsiWhiteSpace) {
            lastElement = lastElement.getPrevSibling();
        }
        assert firstElement != null;
        assert lastElement != null;
        List<PsiElement> psiElements = PsiTreeUtil.getElementsOfRange(firstElement, lastElement);
        return psiElements.toArray(new PsiElement[0]);
    }


    private PsiElement[] useReplacements(Set<Replacement> replacements, PsiMethod psiMethod) {
        ArrayList<PsiElement> psiElements = new ArrayList<>();
        PsiCodeBlock psiCodeBlock = psiMethod.getBody();
        assert psiCodeBlock != null;
        PsiStatement[] psiStatements = psiCodeBlock.getStatements();
        for(Replacement replacement : replacements) {
            String after = replacement.getAfter();
            after = Utils.formatText(after);
            for(PsiStatement psiStatement : psiStatements) {
                String psiStatementText = Utils.formatText(psiStatement.getText());
                if(psiStatementText.equals(after)) {
                    psiElements.add(psiStatement);
                }
            }
        }
        return psiElements.toArray(new PsiElement[0]);
    }



    /*
     * Gets the return type of the extracted method.
     */
    private PsiType getPsiReturnType(ExtractOperationRefactoring extractOperationRefactoring, PsiMethod psiMethod) {
        UMLParameter returnParameter = extractOperationRefactoring.getExtractedOperation().getReturnParameter();
        String parameterType = returnParameter.getType().toString();
        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        return factory.createTypeFromText(parameterType, psiMethod);
    }

    /*
     * Use the PSI method and UML operation to reorder the parameters in the extracted method.
     */
    private ParameterInfoImpl[] getParameterInfo(PsiMethod psiMethod, UMLOperation umlOperation) {

        List<UMLParameter> umlParameterList = umlOperation.getParameters();
        PsiParameterList psiParameterList = psiMethod.getParameterList();
        ParameterInfoImpl[] parameterInfoImplArray = new ParameterInfoImpl[umlParameterList.size() - 1];
        for(int i = 1; i < umlParameterList.size(); i++) {
            UMLParameter umlParameter = umlParameterList.get(i);
            String umlParameterType = umlParameter.getType().toString();
            String umlParameterName = umlParameter.getName();
            ParameterInfoImpl parameterInfo;
            for(PsiParameter psiParameter : psiParameterList.getParameters()) {
                String psiParameterName = psiParameter.getName();
                PsiType psiType = psiParameter.getType();
                String psiParameterType = psiType.getPresentableText();
                if(umlParameterName.equals(psiParameterName) && umlParameterType.equals(psiParameterType)) {
                    int index = psiParameterList.getParameterIndex(psiParameter);
                    parameterInfo = ParameterInfoImpl.create(index).withName(psiParameterName).withType(psiType);
                    parameterInfoImplArray[i-1] = parameterInfo;
                    break;
                }
            }

        }

        return parameterInfoImplArray;
    }

    /*
     * Renames the parameters in the extracted method using the UML parameters detected by RefMiner.
     */
    private void renameParameters(PsiMethod psiMethod, UMLOperation umlOperation) {

        List<UMLParameter> umlParameterList = umlOperation.getParameters();
        PsiParameterList psiParameterList = psiMethod.getParameterList();
        ParameterInfoImpl[] parameterInfoImplArray = new ParameterInfoImpl[umlParameterList.size() - 1];
        // Start at 1 to ignore return type
        for(int i = 1; i < umlParameterList.size(); i++) {
            UMLParameter umlParameter = umlParameterList.get(i);
            String umlParameterType = umlParameter.getType().toString();
            String umlParameterName = umlParameter.getName();
            PsiParameter psiParameter = psiParameterList.getParameter(i-1);
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            RefactoringFactory factory = JavaRefactoringFactory.getInstance(project);
            RenameRefactoring renameRefactoring = factory.createRename(psiParameter, umlParameterName, true, false);
            UsageInfo[] refactoringUsages = renameRefactoring.findUsages();
            renameRefactoring.doRefactoring(refactoringUsages);
        }
    }

}
