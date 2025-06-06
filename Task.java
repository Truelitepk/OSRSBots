package framework;

public interface Task {
    boolean accept();
    int execute();
    String getName();
}
