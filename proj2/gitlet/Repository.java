package gitlet;

import java.io.File;
import java.io.Serializable;

import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BLOBS_DIR = new File(GITLET_DIR, "blobs");
    public static final File INDEX_FILE = new File(GITLET_DIR, "index");
    public static final File COMMITS_DIR = new File(GITLET_DIR, "commits");
    public static final File HEAD_FILE = new File(GITLET_DIR, "HEAD");
    public static final File REFS_DIR = new File(GITLET_DIR, "refs");
    public static final File HEADS_DIR = new File(REFS_DIR, "heads");
    public static final File REMOVAL_FILE = new File(GITLET_DIR, "removal");

    public void init() {
        boolean gitletDirExists = GITLET_DIR.exists();
        if (gitletDirExists) {
            System.out.println("A gitlet version-control system already exists in the current directory.");
            return;
        }
        //创建.gitlet目录及其子目录
        GITLET_DIR.mkdirs();
        COMMITS_DIR.mkdirs();
        BLOBS_DIR.mkdirs();
        Commit initialCommit = new Commit("initial commit", null, new Date(0), new HashMap<>());
        //序列化初始提交对象并计算其SHA-1哈希值作为提交ID
        byte[] serializedCommit = serialize(initialCommit);
        String commitUID = sha1(serializedCommit);
        //将初始提交对象写入commits目录，文件名为提交ID
        File initialCommitFile = new File(COMMITS_DIR, commitUID);
        writeObject(initialCommitFile, initialCommit);
        //创建refs和heads目录
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();
        File masterFile = new File(HEADS_DIR, "master");
        File headFile = new File(GITLET_DIR, "HEAD");
        //写入master文件，内容是初始提交ID
        writeContents(masterFile, commitUID);
        String headContent = "ref: refs/heads/master";
        //写入HEAD文件，内容是"ref: refs/heads/master"，指向master分支的引用
        writeContents(headFile, headContent);
    }
    public void add(String fileName) {
        //需要添加的文件
        File fileToAdd = new File(CWD, fileName);
        //检查文件是否存在
        if (!fileToAdd.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        //检查文件是否已经被标记为删除,如果是，就从删除暂存区中移除该文件并返回
        if (REMOVAL_FILE.exists()) {
            HashSet<String> stagingRemovals = readObject(REMOVAL_FILE, HashSet.class);
            if (stagingRemovals.contains(fileName)) {
                stagingRemovals.remove(fileName);
                writeObject(REMOVAL_FILE, stagingRemovals);
            }
        }
        //读取文件内容并计算SHA-1哈希值作为blob ID
        byte[] fileContent  = readContents(fileToAdd);
        String blobUID = sha1(fileContent);
        //加载暂存区，存在就将其反序列化为一个HashMap.不存在就创建一个新的
        HashMap<String, String> stagingArea;
        if (INDEX_FILE.exists()) {
            stagingArea = readObject(INDEX_FILE, HashMap.class);
        } else {
            stagingArea = new HashMap<>();
        }
        //保存快照,将文件名和对应的blob ID存入暂存区
        File blobFile = new File (BLOBS_DIR, blobUID);
        //写入内容
        if (!blobFile.exists()) {
            writeContents(blobFile, fileContent);
        }
        //更新暂存区，将文件名映射到blob ID
        stagingArea.put(fileName, blobUID);
        //将更新后的暂存区序列化并写回INDEX_FILE
        writeObject(INDEX_FILE, stagingArea);
    }
    /* TODO: fill in the rest of this class. */
    public void commit(String message) {
        //检查提交信息是否为空
        if (message == null || message.isEmpty()) {
            System.out.println("Please enter a commit message");
            return;
        }
        //加载暂存区
        if (!INDEX_FILE.exists()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        //反序列化读取暂存区内容
        HashMap<String, String> stagingArea = readObject(INDEX_FILE, HashMap.class);
        //检查暂存区是否为空
        if (stagingArea.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        //获取父提交ID
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        String parentCommitID = readContentsAsString(currentBranchFile);
        //创建新的提交对象
        File parentCommitFile = new File(COMMITS_DIR, parentCommitID);
        Commit parentCommit = readObject(parentCommitFile, Commit.class);
        //继承父提交的文件快照
        HashMap<String, String> newCommitBlobs = new HashMap<>(parentCommit.getBlobs());
        //将暂存区的更改应用到新的提交快照中
        for (String fileName : stagingArea.keySet()) {
            String blobUID = stagingArea.get(fileName);
            newCommitBlobs.put(fileName, blobUID);
        }
        //处理删除暂存区
        HashSet<String> stagingRemovals = new HashSet<>();
        if (REMOVAL_FILE.exists()) {
            stagingRemovals = readObject(REMOVAL_FILE, HashSet.class);
        }
        for (String fileName : stagingRemovals) {
            newCommitBlobs.remove(fileName);
        }
        //创建新的提交对象
        Date timesTamp = new Date();
        Commit newCommit = new Commit(message, parentCommitID, timesTamp, newCommitBlobs);
        //序列化新的提交对象并计算其SHA-1哈希值作为提交ID
        byte[] serializedNewCommit = serialize(newCommit);
        String newCommitId = sha1(serializedNewCommit);
        //将新的提交对象写入commits目录，文件名为提交ID
        File newCommitFile = new File(COMMITS_DIR, newCommitId);
        writeObject(newCommitFile, newCommit);
        //将commitId写入当前分支文件，更新HEAD指向
        writeContents(currentBranchFile, newCommitId);
        //清空暂存区
        INDEX_FILE.delete();
        REMOVAL_FILE.delete();
    }
    public void rm(String fileName) {
        HashMap<String, String> stagingAdditions;
        if (INDEX_FILE.exists()) {
            stagingAdditions = readObject(INDEX_FILE, HashMap.class);
        } else {
            stagingAdditions = new HashMap<>();
        }
        //加载待删除暂存区
        HashSet<String> stagingRemovals;
        if (REMOVAL_FILE.exists()) {
            stagingRemovals = readObject(REMOVAL_FILE, HashSet.class);
        } else {
            stagingRemovals = new HashSet<>();
        }
        //获取当前HEAD指向的提交ID
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        String headCommitId = readContentsAsString(currentBranchFile);
        Commit currentCommit = readObject(new File(COMMITS_DIR, headCommitId), Commit.class);
        HashMap<String, String> currentFiles = currentCommit.getBlobs();
        //检查是否在暂存区和是否被跟踪
        boolean isStaged = stagingAdditions.containsKey(fileName);
        boolean isTracked = currentFiles.containsKey(fileName);//如果既不在暂存区也不被跟踪，打印错误信息
        if (!isStaged && !isTracked) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (isStaged) {
            //如果在待添加暂存区，取消暂存
            stagingAdditions.remove(fileName);
        }
        if (isTracked) {
            //如果被commit跟踪，就标记为待删除
            stagingRemovals.add(fileName);
            //删除工作目录中的文件
            File fileToDelete = new File(CWD, fileName);
            restrictedDelete(fileToDelete);
        }

        //更新暂存区文件
        writeObject(INDEX_FILE, stagingAdditions);
        writeObject(REMOVAL_FILE, stagingRemovals);
    }
    public void log() {
        //通过HEAD文件找到当前分支
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        String currentCommitId = readContentsAsString(currentBranchFile);
        //遍历提交历史
        while (currentCommitId != null) {
            File commitFile = new File(COMMITS_DIR, currentCommitId);
            Commit currentCommit = readObject(commitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + currentCommitId);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
            System.out.println(currentCommit.getMessage());
            System.out.println();
            //移动到父提交
            currentCommitId = currentCommit.getParent();
        }
    }
    public void globalLog() {
        //获得所有提交ID，globallog与log不同，它显示所有分支的提交
        List<String> allCommitIds = plainFilenamesIn(COMMITS_DIR);
        if (allCommitIds == null) {
            return;
        }
        for (String commitId : allCommitIds) {
            File commitFile = new File(COMMITS_DIR, commitId);
            Commit commit = readObject(commitFile, Commit.class);

            System.out.println("===");
            System.out.println("commit " + commitId);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }
    public void find(String message) {
        List<String> allCommits = plainFilenamesIn(COMMITS_DIR);
        if (allCommits == null) {
            return;
        }
        boolean found = false;
        for (String commitId : allCommits) {
            File commitFile = new File(COMMITS_DIR, commitId);
            Commit commit = readObject(commitFile, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commitId);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }
    public void status() {
        //获取所有分支信息
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        String currentBranchName = new File(currentBranchPath).getName();
        //加载暂存区
        //待添加暂存区
        HashMap<String, String> stagingAdditions = new HashMap<>();
        if (INDEX_FILE.exists()) {
            stagingAdditions = readObject(INDEX_FILE, HashMap.class);
        }
        //待删除暂存区
        HashSet<String> stagingRemovals = new HashSet<>();
        if (REMOVAL_FILE.exists()) {
            stagingRemovals = readObject(REMOVAL_FILE, HashSet.class);
        }
        //根据路径获取head文件内容，即当前分支的最新提交ID
        String headCommitId = readContentsAsString(new File (GITLET_DIR, currentBranchPath));
        //根据commitID获取最新的提交，然后将其反序列化位为Commit对象
        Commit currentCommit = readObject(new File(COMMITS_DIR, headCommitId), Commit.class);
        //获取最新提交的文件快照，即被跟踪的文件
        HashMap<String, String> trackedFiles = currentCommit.getBlobs();
        //获取工作目录中的所有文件
        List<String> workingDirectoryFiles = plainFilenamesIn(CWD);
        //打印分支信息
        System.out.println("=== Branches ===");
        Collections.sort(branchNames);
        for (String branchName : branchNames) {
            if (branchName.equals(currentBranchName)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();
        //打印暂存区信息,(TreeSet会自动排序)
        System.out.println("=== Staged Files ===");
        for (String fileName : new TreeSet<>(stagingAdditions.keySet())) {
            System.out.println(fileName);
        }
        System.out.println();
        //打印删除暂存区信息
        System.out.println("=== Removed Files ===");
        for (String fileName : new TreeSet<>(stagingRemovals)) {
            System.out.println(fileName);
        }
        System.out.println();
        //打印被修改但未暂存的文件
        System.out.println("=== Tracked Files ===");
        //使用TreeSet对文件名进行排序
        TreeSet<String> modifiedFiles = new TreeSet<>();
        //检查被跟踪的文件是否被修改
        for (String fileName : trackedFiles.keySet()) {
            File file = new File(CWD, fileName);
            String trackedFileID = trackedFiles.get(fileName);

            boolean isStagedForAddition = stagingAdditions.containsKey(fileName);
            boolean isMarkedForRemoval = stagingRemovals.contains(fileName);

            //文件被追踪，但是工作区中已经删除，并且没有被标记为待删除
            if (!file.exists() && !isMarkedForRemoval) {
                modifiedFiles.add(fileName + "(deleted)");
                continue;
            }
            //文件被追踪，也在工作区存在，但是内容有修改，并且没有从新暂存
            if (file.exists()) {
                //重新计算当前工作区文件的内容ID，与被跟踪的内容ID（最新的提交）进行比较
                String currentContentID = sha1(readContents(file));
                if (!currentContentID.equals(trackedFileID) && !isStagedForAddition) {
                    modifiedFiles.add(fileName + "(modified)");
                }
            }
        }
        //检查已经暂存的文件
        for (String fileName : stagingAdditions.keySet()) {
            File file = new File(CWD, fileName);
            String stagingFileID = stagingAdditions.get(fileName);
            //文件在暂存区，但是工作区中已经删除
            if (!file.exists()) {
                modifiedFiles.add(fileName + "(deleted)");
                continue;
            }
            //文件在暂存区，也在工作区存在，但是内容再次有修改
            String currentContentID = sha1(readContents(file));
            if (!currentContentID.equals(stagingFileID)) {
                modifiedFiles.add(fileName + "(modified)");
            }
        }
        //打印结果
        for (String fileName : modifiedFiles) {
            System.out.println(fileName);
        }
        System.out.println();
        //打印未跟踪的文件
        System.out.println("=== Untracked Files ===");
        if (workingDirectoryFiles != null) {
            Collections.sort(workingDirectoryFiles);
            for (String fileName : workingDirectoryFiles) {
                //判断一个文件是否被跟踪或者再暂存区中，如果都没有，就认为是未跟踪文件
                boolean isTracked = trackedFiles.containsKey(fileName);
                boolean isStagedForAddition = stagingAdditions.containsKey(fileName);
                if (!isTracked && !isStagedForAddition) {
                    System.out.println(fileName);
                }
            }
        }
        System.out.println();
    }
    public void checkout(String... args) {
        //检查参数长度
        //checkout --[file name]
        if (args.length == 2 && args[0].equals("--")) {
            checkoutFileFromHead(args[1]);
        }
        //checkout [commit id] -- [file name]
        else if (args.length == 3 && args[1].equals("--")) {
            checkoutFileFromCommit(args[0], args[2]);
        }
        //checkout [branch name]
        else if (args.length == 1) {
            checkoutBranch(args[0]);
        }
        else {
            System.out.println("Incorrect operands.");
        }
    }
    private void checkoutFileFromHead(String fileName) {
        //获取当前HEAD指向的提交ID
        String headFileContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headFileContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        String currentCommitId = readContentsAsString(currentBranchFile);
        //从当前提交中检出指定文件
        checkoutFileFromCommit(currentCommitId, fileName);
    }
    private void checkoutFileFromCommit(String commitId, String fileName) {
        String fullCommitId = findFullId(commitId);
        //检查提交ID是否存在
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        //读取指定提交对象
        File commitFile = new File(COMMITS_DIR, fullCommitId);
        Commit commit = readObject(commitFile, Commit.class);
        HashMap<String, String> blobs = commit.getBlobs();
        //检查指定文件是否在该提交中被跟踪
        if (!blobs.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        //将该文件的内容写入当前工作目录，覆盖同名文件
        String blobId = blobs.get(fileName);
        File blobFile = new File(BLOBS_DIR, blobId);
        byte[] blobContent = readContents(blobFile);
        File targetFileInCWD = new File(CWD, fileName);
        writeContents(targetFileInCWD, blobContent);

    }
    private void checkoutBranch(String branchName) {
        //检查分支是否存在
        File branchFile = new File(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        //获得当前的分支名
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        String currentBranchName = new File(currentBranchPath).getName();
        //检查是否是切换到当前分支
        if (currentBranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        //获取目标分支的最新提交
        String targetBranchCommitId = readContentsAsString(branchFile);
        File targetBranchCommitFile = new File(COMMITS_DIR, targetBranchCommitId);
        Commit targetBranchCommit = readObject(targetBranchCommitFile, Commit.class);
        //检查是否有未被跟踪的文件会被覆盖
        checkForUntrackedFiles(targetBranchCommit);
        //
        String headFileContent = readContentsAsString(HEAD_FILE);
        String currentPath = headFileContent.split(" ")[1];
        File currentFile = new File(GITLET_DIR, currentPath);
        String currentCommitId = readContentsAsString(currentFile);
        Commit currentCommit = readObject(new File(COMMITS_DIR, currentCommitId), Commit.class);
        //覆盖或添加目标分支的文件
        HashMap<String, String> targetBlobs = targetBranchCommit.getBlobs();
        for (String fileName : targetBlobs.keySet()) {
            //调用该函数会覆盖同名内容
            checkoutFileFromCommit(targetBranchCommitId, fileName);
        }
        //删除当前分支有但目标分支没有的文件
        HashMap<String, String> currentBlobs = currentCommit.getBlobs();
        for (String fileName : currentBlobs.keySet()) {
            if (!targetBlobs.containsKey(fileName)) {
                File fileToDelete = new File(CWD, fileName);
                restrictedDelete(fileToDelete);
            }
        }
        String newHeadContent = "ref: refs/heads/" + branchName;
        writeContents(HEAD_FILE, newHeadContent);
        //清空暂存区
        if (INDEX_FILE.exists()) {
            INDEX_FILE.delete();
        }
        if (REMOVAL_FILE.exists()) {
            REMOVAL_FILE.delete();
        }
    }
    //辅助方法
    //根据短ID找到完整的提交ID
    private String findFullId (String shortId) {
        List<String> allCommitIds = plainFilenamesIn(COMMITS_DIR);
        if (allCommitIds == null) {
            return null;
        }
        for (String fullId : allCommitIds) {
            if (fullId.startsWith(shortId)) {
                return fullId;
            }
        }
        return null;
    }
    //检查未跟踪文件
    private void checkForUntrackedFiles(Commit targetCommit) {
        //获取当前工作目录下的2所有文件
        List<String> workingDirFiles = plainFilenamesIn(CWD);
        //工作区域没有文件，直接返回
        if (workingDirFiles == null) {
            return;
        }
        //获取当前HEAD指向的提交ID
        String headFileContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headFileContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        String currentCommitId = readContentsAsString(currentBranchFile);
        //获取当前提交对象
        Commit currentCommit = readObject(new File(COMMITS_DIR, currentCommitId), Commit.class);
        //获取当前提交的文件快照
        HashMap<String, String> trackedFiles = currentCommit.getBlobs();
        //加载暂存区
        HashMap<String, String> stagedFiles = new HashMap<>();
        if (INDEX_FILE.exists()) {
            stagedFiles = readObject(INDEX_FILE, HashMap.class);
        }
        //检查工作目录中的每个文件
        //如果文件既不在当前提交中被跟踪，也不在暂存区中，就认为是未跟踪文件
        for (String fileName : workingDirFiles) {
            boolean isTracked = trackedFiles.containsKey(fileName);
            boolean isStagedForAddition = stagedFiles.containsKey(fileName);
            if (!isTracked && !isStagedForAddition) {
                //并且这个文件会被切换分支的操作覆盖
                if (targetCommit.getBlobs().containsKey(fileName)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                    //直接退出程序
                }
            }
        }
    }
    public String getCurrentCommitId() {
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        File currentBranchFile = new File(GITLET_DIR, currentBranchPath);
        return readContentsAsString(currentBranchFile);
    }
    public String getCurrentBranchName() {
        String headContent = readContentsAsString(HEAD_FILE);
        String currentBranchPath = headContent.split(" ")[1];
        return new File(currentBranchPath).getName();
    }
    public void branch(String branchName) {
        //检查分支是否已经存在
        File newBranchFile = new File(HEADS_DIR, branchName);
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        //获取当前HEAD指向的提交ID
        String currentCommitId = getCurrentCommitId();
        writeContents(newBranchFile, currentCommitId);
    }
    public void rmBranch(String branchName) {
        //检查分支是否存在
        File branchFileToDelete = new File(HEADS_DIR, branchName);
        if (!branchFileToDelete.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        //检查是否是当前所处分支
        String currentBranchName = getCurrentBranchName();
        if (currentBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        //删除分支文件
        branchFileToDelete.delete();
    }
    public void reset(String commitId) {
        String fullCommitId = findFullId(commitId);
        //检查提交ID是否存在
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit targetCommit = readObject(new File(COMMITS_DIR, fullCommitId), Commit.class);
        //检查是否有未被跟踪的文件会被覆盖
        checkForUntrackedFiles(targetCommit);
        //获取当前commit
        String currentCommitId = getCurrentCommitId();
        Commit currentCommit = readObject(new File(COMMITS_DIR, currentCommitId), Commit.class);
        HashMap<String, String> targetBlobs = targetCommit.getBlobs();
        //覆盖或添加目标提交的文件
        for (String fileName : targetBlobs.keySet()) {
            //覆盖同名文件内容
            checkoutFileFromCommit(fullCommitId, fileName);
        }
        HashMap<String, String> currentBlobs = currentCommit.getBlobs();
        //删除当前提交有但目标提交没有的文件
        for (String fileName : currentBlobs.keySet()) {
            if (!targetBlobs.containsKey(fileName)) {
                File fileToDelete = new File(CWD, fileName);
                restrictedDelete(fileToDelete);
            }
        }
        //将当前分支指向目标提交
        //获取当前分支文件
        String currentBranchName = getCurrentBranchName();
        File currentBranchFile = new File(HEADS_DIR, currentBranchName);
        //更新当前分支文件内容为目标提交ID
        writeContents(currentBranchFile, fullCommitId);
        //清空暂存区
        if (INDEX_FILE.exists()) {
            INDEX_FILE.delete();
        }
        if (REMOVAL_FILE.exists()) {
            REMOVAL_FILE.delete();
        }
    }


}
