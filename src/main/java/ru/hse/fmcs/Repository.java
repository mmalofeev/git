package ru.hse.fmcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import java.time.LocalDate;
import java.util.*;

public class Repository {
    private final Map<String, Commit> commits;
    private final String workingDirectory;
    private StagingArea stagingArea;
    private Commit head;

    public Repository(String workingDir) {
        workingDirectory = workingDir;
        stagingArea = new StagingArea();
        commits = new HashMap<>();
        head = new Commit();
    }

    public Repository() {
        commits = new HashMap<>();
        workingDirectory = "";
        stagingArea = new StagingArea();
        head = new Commit();
        commits.put(head.commitHash, head);
    }

    public Map <String, Commit> getCommits() {
        return commits;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public StagingArea getStagingArea() {
        return stagingArea;
    }

    public void setStagingArea(StagingArea newStagingArea) {
        stagingArea = newStagingArea;
    }

    public Commit getHead() {
        return head;
    }

    public void setHead(Commit newHead) {
        head = newHead;
    }

    public static class StagingArea {
        private final Map<String, String> addedFiles;
        private final Set<String> deletedFiles;

        public StagingArea() {
            addedFiles = new HashMap<>();
            deletedFiles = new HashSet<>();
        }

        public Map<String, String> getAddedFiles() {
            return addedFiles;
        }


        public Set <String> getDeletedFiles() {
            return deletedFiles;
        }

        public void addFile(String filePath, String fileHash) {
            deletedFiles.remove(filePath);
            addedFiles.put(filePath, fileHash);
        }

        public void deleteFile(String filePath) {
            addedFiles.remove(filePath);
            deletedFiles.add(filePath);
        }
    }

    public static class Commit {
        private final String commitHash;
        private String nextCommit;
        private final String previousCommit;
        private final Map<String, String> trackedFiles;
        private final String date;
        private final String message;
        private final String user;

        public String getCommitHash() {
            return commitHash;
        }

        public String getNextCommit() {
            return nextCommit;
        }

        public void setNextCommit(String newNextCommit) {
            nextCommit = newNextCommit;
        }

        public String getPreviousCommit() {
            return previousCommit;
        }

        public Map<String, String> getTrackedFiles() {
            return trackedFiles;
        }

        public String getDate() {
            return date;
        }

        public String getMessage() {
            return message;
        }

        public String getUser() {
            return user;
        }

        public Commit() {
            commitHash = Hashing.sha256().hashInt(this.hashCode()).toString();
            nextCommit = "";
            previousCommit = "";
            trackedFiles = new HashMap<>();
            date = LocalDate.now().toString();
            message = "Initial commit";
            user = "Test user";
        }

        public Commit(String message, Commit head, StagingArea stagingArea) {
            date = LocalDate.now().toString();
            this.message = message;
            nextCommit = "";
            previousCommit = head.getCommitHash();
            trackedFiles = new HashMap<>();
            for (String file : head.trackedFiles.keySet()) {
                trackedFiles.put(file, head.trackedFiles.get(file));
            }
            commitHash = Hashing.sha256().hashInt(this.hashCode()).toString();
            head.nextCommit = commitHash;
            for (String file : stagingArea.deletedFiles) {
                trackedFiles.remove(file);
            }
            user = "Test user";
            trackedFiles.putAll(stagingArea.addedFiles);
        }
    }
}
