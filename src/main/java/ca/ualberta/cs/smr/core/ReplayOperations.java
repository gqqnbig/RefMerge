package ca.ualberta.cs.smr.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.rename.RenameProcessor;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import org.refactoringminer.api.Refactoring;

public class ReplayOperations {

    Project proj;

    public ReplayOperations(Project proj) {
        this.proj = proj;
    }

    /*
     * replayRenameMethod performs the rename method refactoring.
     */
    public void replayRenameMethod(Refactoring ref) {
        UMLOperation original = ((RenameOperationRefactoring) ref).getOriginalOperation();
        UMLOperation renamed = ((RenameOperationRefactoring) ref).getRenamedOperation();
        String destName = renamed.getName();
        String srcName = original.getName();
        String qualifiedClass = renamed.getClassName();
        String className = original.getNonQualifiedClassName();
        JavaPsiFacade jPF = new JavaPsiFacadeImpl(proj);
        DumbService.isDumb(proj);
        // Get the PSI class using the qualified class name
        PsiClass jClass = jPF.findClass(qualifiedClass, GlobalSearchScope.allScope(proj));
        RenameProcessor processor;
        // If the PSI class is null, then this part of the project wasn't built and we need to find the PSI class
        // another way
        if(jClass == null) {
            // Get the name of the java file
            String fileName = className + ".java";
            // Get a list of java files with the name qClass in the project
            PsiFile[] pFiles = FilenameIndex.getFilesByName(proj, fileName, GlobalSearchScope.allScope(proj));
            // If no files are found, give an error message for debugging
            // If it is not found, it does not mean there is a bug necessarily. It could be that another refactoring
            // was performed and isn't handled yet
            if(pFiles.length == 0) {
                System.out.println("FAILED HERE");
                System.out.println(fileName);
                System.out.println(srcName);
                return;
            }
            // Assuming that it is the first file that is returned
            PsiJavaFile pFile = (PsiJavaFile) pFiles[0];
            // Get the classes in the file
            PsiClass[] jClasses = pFile.getClasses();
            for (PsiClass it : jClasses) {
                // Find the class that the refactoring happens in
                if (it.getQualifiedName().equals(qualifiedClass)) {
                    jClass = it;
                    break;
                }
            }
            // Get the methods inside that class

        }
        PsiMethod[] methods = jClass.getMethods();
        for (PsiMethod method : methods) {
            // If we find the method that needs to be refactored
            if (method.getName().equals(srcName)) {
                // Create a rename processor using the method and the name that we're refactoring it to
                processor = new RenameProcessor(proj, method, destName, false, false);
                // Run the refactoring processor
                ApplicationManager.getApplication().invokeAndWait(processor::doRun, ModalityState.current());
                // Update the virtual file containing the refactoring
                VirtualFile vFile = jClass.getContainingFile().getVirtualFile();
                vFile.refresh(false, true);
                break;
            }
        }




    }

}
