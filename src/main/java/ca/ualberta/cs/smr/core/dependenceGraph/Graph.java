package ca.ualberta.cs.smr.core.dependenceGraph;

import ca.ualberta.cs.smr.utils.sortingUtils.Pair;
import com.intellij.openapi.project.Project;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    Project project;
    private List<Node> nodes;
    private List<Node> allNodes;

    public Graph(Project project) {
        this.project = project;
        this.allNodes = new ArrayList<>();
    }

    public void createPartialGraph(List<Pair> pairs) {
        this.nodes = new ArrayList<>();
        if(pairs.size() == 0) {
            return;
        }
        if(pairs.size() == 1) {
            Node node = new Node(pairs.get(0).getValue());
            addNode(node);
            return;
        }
        for(int i = pairs.size() - 1; i > -1; i--) {
            Pair pair = pairs.get(i);
            Refactoring refactoring = pair.getValue();
            Node node = new Node(refactoring);
            insertNode(node);
            addNode(node);
        }
        this.allNodes.addAll(nodes);
    }

    public void addNode(Node node) {
        this.nodes.add(node);
    }

    public void addEdge(Node from, Node to) {
        Edge edge = new Edge(from, to);
        from.addEdge(edge);
    }

    void insertNode(Node newNode) {
        Node temp = null;
        for(Node node : nodes) {
            if(hasDependence(node, newNode)) {
                newNode.addToDependsList(node);
                if(!node.hasNeighbors()) {
                    addEdge(node, newNode);
                    return;
                }
                else {
                    temp = node;
                }
            }
            else {
                if(node.hasNeighbors() || newNode.hasNeighbors()) {
                    continue;
                }
                if(temp != null) {
                    addEdge(temp, newNode);
                    temp = null;
                }
            }
        }

    }

    public boolean hasDependence(Node node1, Node node2) {
        Refactoring refactoring1 = node1.getRefactoring();
        Refactoring refactoring2 = node2.getRefactoring();
        RefactoringType type1 = refactoring1.getRefactoringType();
        RefactoringType type2 = refactoring2.getRefactoringType();
        if(type1 == RefactoringType.RENAME_CLASS && type2 == RefactoringType.RENAME_CLASS) {
            // Only need to check this case because the node cannot depend on the new node
            String class1 = ((RenameClassRefactoring) refactoring1).getRenamedClassName();
            String class2 = ((RenameClassRefactoring) refactoring2).getOriginalClassName();
            return class1.equals(class2);
        }
        else if(type1 == RefactoringType.RENAME_CLASS && type2 == RefactoringType.RENAME_METHOD) {
            // Check if they happen in the same commit
            String class1 = ((RenameClassRefactoring) refactoring1).getOriginalClassName();
            String class2 = ((RenameOperationRefactoring) refactoring2).getOriginalOperation().getClassName();
            if(class1.equals(class2)) {
                return true;
            }
            // Check if the method is renamed in a commit after the class is renamed
            class1 = ((RenameClassRefactoring) refactoring1).getRenamedClassName();
            return class1.equals(class2);
        }
        else if(type1 == RefactoringType.RENAME_METHOD && type2 == RefactoringType.RENAME_CLASS) {
            // Do the same thing as above
            String class1 = ((RenameOperationRefactoring) refactoring1).getOriginalOperation().getClassName();
            String class2 = ((RenameClassRefactoring) refactoring2).getOriginalClassName();
            if(class1.equals(class2)) {
                return true;
            }
            class2 = ((RenameClassRefactoring) refactoring2).getRenamedClassName();
            return class1.equals(class2);
        }
        return false;


    }

    public void printGraph() {

        for(Node node : allNodes) {
            if(node.wasVisited()) {
                continue;
            }
            node.visiting();
            if(node.dependsOn().isEmpty() && node.getEdges().isEmpty()) {
                System.out.println("Island: " + node.getRefactoring().toString());
            }
            for(Edge edge : node.getEdges()) {
                edge.getDestination().wasVisited();
                    System.out.println(edge.getSource().getRefactoring().toString() +
                            " <== " + edge.getDestination().getRefactoring().toString());
            }
        }

    }


}
