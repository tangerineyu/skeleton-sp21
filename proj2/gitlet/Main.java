package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        Repository repo = new Repository();
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                repo.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if (args.length < 2) {
                    System.out.println("Please enter a command");
                    break;
                }
                String fileName = args[1];
                repo.add(fileName);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                if (args.length < 2 || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                String message = args[1];
                repo.commit(message);
                break;
            case "rm":
                if (args.length < 2) {
                    System.out.println("Please enter a command");
                    break;
                }
                String fileToRemove = args[1];
                repo.rm(fileToRemove);
                break;
            case "log":
                repo.log();
                break;
            case "global-log":
                repo.globalLog();
                break;
            case "find":
                if (args.length < 2) {
                    System.out.println("Please enter a command");
                    break;
                }
                String commitMessage = args[1];
                repo.find(commitMessage);
                break;
            case "status":
                repo.status();
                break;
            case "checkout":
                String[] checkoutArgs = new String[args.length - 1];
                System.arraycopy(args, 1, checkoutArgs, 0, args.length - 1);
                repo.checkout(checkoutArgs);
                break;
            case "branch":
                if (args.length < 2) {
                    System.out.println("Please enter a command");
                    break;
                }
                String branchName = args[1];
                repo.branch(branchName);
                break;
            case "rm-branch":
                if (args.length < 2) {
                    System.out.println("Please enter a command");
                    break;
                }
                String branchToRemove = args[1];
                repo.rmBranch(branchToRemove);
                break;
            case "reset":
                if (args.length < 2) {
                    System.out.println("Didn't enter a commit id.");
                    break;
                }
                String commitId = args[1];
                repo.reset(commitId);
                break;
            default :
                System.out.println(
                        "No command with that name exits."
                );
        }
    }
}
