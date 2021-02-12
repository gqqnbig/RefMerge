package result.refMergeTestData.refactorings;

public class RefactoringTestdata {
    public int doMath(int x, int y, int z) {
        int res1 = x + y - z;
        int res2 = x * y - y * z;
        int res3 = z - x + y;
        graph(res1, res2);
        graph(res2, res3);
        graph(res1, res3);
    }

    public void graph(int x, int y) {
        Graph.plot(x, y);
    }

    public String getDescription() {
        String description = "Prints a pair of numbers or does plotting for 20 points";
        return description;
    }
}

class Graph {
    public void plot(int x, int y) {
        System.out.println("Plotting:");
        System.out.println(x + ", " + y);
    }
    protected static class PlotHelper {
        protected void plotter(int x) {
            for(int i = 0; i < 20; i++) {
                int y = x * x;
                doPlotting(x, y);
            }
        }
    }
}