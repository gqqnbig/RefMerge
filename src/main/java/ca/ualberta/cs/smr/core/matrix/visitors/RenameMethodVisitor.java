package ca.ualberta.cs.smr.core.matrix.visitors;

import ca.ualberta.cs.smr.core.dependenceGraph.Graph;
import ca.ualberta.cs.smr.core.matrix.elements.RenameClassElement;
import ca.ualberta.cs.smr.core.matrix.elements.RenameMethodElement;
import org.refactoringminer.api.Refactoring;

public class RenameMethodVisitor extends RefactoringVisitor {
    Refactoring visitorRef;
    Graph graph;

    public void set(Refactoring visitorRef, Graph graph) {
        this.visitorRef = visitorRef;
        this.graph = graph;
    }

    /*
     * Check if rename method conflicts with rename method
     */
    @Override
    public void visit(RenameMethodElement renameMethod) {
        boolean foundConflict = renameMethod.checkRenameMethodConflict(visitorRef);
        System.out.println("Rename Method/Rename Method conflict: " + foundConflict);
    }

    /*
     * Check if rename class conflicts with rename method
     */
    @Override
    public void visit(RenameClassElement renameClass) {
        Refactoring elementRef = renameClass.checkRenameMethodDependence(visitorRef);
        if(elementRef != null) {
            graph.updateGraph(visitorRef, elementRef);
        }
    }
}
