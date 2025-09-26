package gitlet;


import java.util.ArrayList;
import java.util.Date;
//import java.gitlet.Utils.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date timestamp;
    private List<String> parents;
    private HashMap<String, String> blobs;
    public Commit(String message, List<String> parents, Date timestamp, HashMap<String, String> blobs) {
        this.message = message;
        this.timestamp = timestamp;
        this.parents = parents;
        this.blobs = blobs;
    }
    public Commit(String message, String parent, Date timestamp, HashMap<String, String> blobs) {
        this(message, (parent == null) ? new ArrayList<>() : List.of(parent),
                timestamp,
                blobs);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
    public List<String> getParents() {
        return parents;
    }
    public String getParent() {
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        return parents.get(0);
    }
    public HashMap<String, String> getBlobs() {
        return blobs;
    }
}
