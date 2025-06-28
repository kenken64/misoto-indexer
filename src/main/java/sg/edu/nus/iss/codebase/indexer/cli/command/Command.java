package sg.edu.nus.iss.codebase.indexer.cli.command;

/**
 * Command interface for CLI actions
 * Implements Command Pattern
 */
public interface Command {

    /**
     * Execute the command
     */
    void execute();

    /**
     * Get the command description
     */
    String getDescription();

    /**
     * Check if the command can be executed
     */
    default boolean canExecute() {
        return true;
    }
}
