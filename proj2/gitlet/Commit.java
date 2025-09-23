package gitlet;

// TODO: any imports you need here

import java.util.Date; // TODO: You'll likely use this in this class
import java.io.File;
//import java.gitlet.Utils.*;
import java.io.Serializable;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date timestamp;
    private String parent;
    private HashMap<String, String> blobs;
    public Commit(String message, String parent, Date timestamp, HashMap<String, String> blobs) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent;
        this.blobs = blobs;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
    public String getParent() {
        return parent;
    }
    public HashMap<String, String> getBlobs() {
        return blobs;
    }
    /* TODO: fill in the rest of this class. */
}
