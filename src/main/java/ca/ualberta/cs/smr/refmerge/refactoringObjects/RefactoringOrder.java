package ca.ualberta.cs.smr.refmerge.refactoringObjects;

public enum RefactoringOrder {
    MOVE_RENAME_CLASS(1),
    INLINE_METHOD(2),
    MOVE_RENAME_METHOD(3),
    EXTRACT_METHOD(4),
    PULL_UP_METHOD(5),
    MOVE_RENAME_FIELD(6);

    private final int order;

    RefactoringOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

}
