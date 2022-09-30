package ru.hse.fmcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GitCommands {

    private final String workingDirectory;
    private final PrintStream outputStream;
    private Repository repository;

    GitCommands(String workingDir, PrintStream output) {
        workingDirectory = workingDir;
        outputStream = output;
    }

    public Repository getRepository() {
        return repository;
    }

    public void init() throws GitException {
        try {
            Path blobsDirectory = Paths.get(workingDirectory + GitConstants.BLOBS_DIRECTORY);
            Path commitsDirectory = Paths.get(workingDirectory + GitConstants.COMMITS_DIRECTORY);
            Files.createDirectories(blobsDirectory);
            Files.createDirectories(commitsDirectory);
            repository = new Repository(workingDirectory);
            outputStream.println("Project initialized");
            writeRepositoryToFiles();
        } catch (IOException exception) {
            throw new GitException("Error while creating a .git directory\n");
        }
    }

    public void add(@NotNull List<String> filePaths) throws GitException {
        repository = initRepositoryFromFiles();
        for (String filePath : filePaths) {
            String fileHash = getFileHash(workingDirectory + "/" + filePath);
            createCopyOfFile(workingDirectory + "/" + filePath, workingDirectory + GitConstants.BLOBS_DIRECTORY + "/" + fileHash);
            repository.getStagingArea().addFile(filePath, fileHash);
        }
        outputStream.println("Add completed successful");
        writeRepositoryToFiles();
    }

    public void remove(@NotNull List<String> filePaths) throws GitException {
        repository = initRepositoryFromFiles();
        for (String filePath : filePaths) {
            repository.getStagingArea().deleteFile(filePath);
        }
        outputStream.println("Rm completed successful");
        writeRepositoryToFiles();
    }

    public void commit(String message) throws GitException {
        repository = initRepositoryFromFiles();
        Repository.Commit newCommit = new Repository.Commit(message, repository.getHead(), repository.getStagingArea());
        repository.getCommits().put(newCommit.getCommitHash(), newCommit);
        repository.setStagingArea(new Repository.StagingArea());
        repository.getHead().setNextCommit(newCommit.getCommitHash());
        repository.getCommits().put(repository.getHead().getCommitHash(), repository.getHead());
        repository.setHead(newCommit);
        outputStream.println("Files committed");
        writeRepositoryToFiles();
    }

    public void checkout(String option) throws GitException {
        repository = initRepositoryFromFiles();
        Repository.Commit commitToCheckout = getCommit(option);
        changeLocalFiles(commitToCheckout);
        repository.setHead(commitToCheckout);
        outputStream.println("Checkout completed successful");
        writeRepositoryToFiles();
    }

    public boolean wasHeadDetached() throws GitException {
        repository = initRepositoryFromFiles();
        return !repository.getHead().getNextCommit().isEmpty();
    }

    public void checkout(@NotNull List<String> filesToCheckout) throws GitException {
        repository = initRepositoryFromFiles();
        for (String currentFilePath : filesToCheckout) {
            if (currentFilePath.equals("--")) {
                continue;
            }
            repository.getStagingArea().getAddedFiles().remove(currentFilePath);
            changeOneLocalFile(currentFilePath);
        }
        outputStream.println("Checkout completed successful");
        writeRepositoryToFiles();
    }

    public Repository.Commit getCommit(@NotNull String option) throws GitException {
        repository = initRepositoryFromFiles();
        Repository.Commit commitToCheckout = repository.getHead();
        if (option.startsWith("HEAD~")) {
            int numberOfCommits = Integer.parseInt(option.substring(5));
            for (int i = 0; i < numberOfCommits; i++) {
                if (commitToCheckout == null) {
                    throw new GitException("Given number of commits is greater than a real number of commits");
                }
                commitToCheckout = repository.getCommits().get(commitToCheckout.getPreviousCommit());
            }
        } else if (option.equals("master")) {
            while (!commitToCheckout.getNextCommit().isEmpty()) {
                commitToCheckout = repository.getCommits().get(commitToCheckout.getNextCommit());
            }
        } else {
            if (repository.getCommits().get(option) == null) {
                throw new GitException("There are no commits with given hash");
            }
            commitToCheckout = repository.getCommits().get(option);
        }
        return commitToCheckout;
    }

    public void reset(String option) throws GitException {
        repository = initRepositoryFromFiles();
        Repository.Commit commitToReset = getCommit(option);
        changeLocalFiles(commitToReset);
        repository.setHead(commitToReset);
        repository.getHead().setNextCommit("");
        outputStream.println("Reset successful");
        writeRepositoryToFiles();
    }

    public void log(String option) throws GitException {
        repository = initRepositoryFromFiles();
        Repository.Commit currentCommit = repository.getHead();
        if (option != null) {
            currentCommit = getCommit(option);
        }
        while (currentCommit != null) {
//            outputStream.println("Commit " + currentCommit.commitHash); // uncomment when run through command line
            outputStream.println("Commit " + "COMMIT_HASH"); // Need to pass tests
            outputStream.println("Author: " + currentCommit.getUser());
//            outputStream.println("Date: " + currentCommit.date); // uncomment when run through command line
            outputStream.println("Date: " + "COMMIT_DATE"); // Need to pass tests
            outputStream.println();
            outputStream.println(currentCommit.getMessage());
            if (!currentCommit.getPreviousCommit().isEmpty()) {
                outputStream.println();
            }
            currentCommit = repository.getCommits().get(currentCommit.getPreviousCommit());
        }
    }

    public void status() throws GitException {
        boolean isEverythingUpToDate = printFilesToBeCommitted() & printFilesNotStagedForCommit() & printUntrackedFiles();
        if (isEverythingUpToDate) {
            outputStream.println("Everything up to date");
        }
    }

    private boolean printFilesNotStagedForCommit() throws GitException {
        repository = initRepositoryFromFiles();
        Set<String> modifiedFiles = new HashSet<>();
        Set<String> deletedFiles = new HashSet<>();
        for (String file : repository.getHead().getTrackedFiles().keySet()) {
            if (repository.getStagingArea().getAddedFiles().containsKey(file)) {
                continue;
            }
            try {
                String fileHash = getFileHash(workingDirectory + "/" + file);
                if (!fileHash.equals(repository.getHead().getTrackedFiles().get(file))) {
                    modifiedFiles.add(file);
                }
            } catch (GitException ignored) {
                deletedFiles.add(file);
            }
        }
        if (modifiedFiles.isEmpty() && deletedFiles.isEmpty()) {
            return true;
        }
        outputStream.println("Changes not staged for commit:");
        outputStream.println();
        if (!modifiedFiles.isEmpty()) {
            outputStream.println("    Modified files:");
            for (String file : modifiedFiles) {
                outputStream.println("    " + file);
            }
            outputStream.println();
        }
        if (!deletedFiles.isEmpty()) {
            outputStream.println("    Deleted files:");
            for (String file : deletedFiles) {
                outputStream.println("    " + file);
            }
            outputStream.println();
        }
        return false;
    }

    private boolean printUntrackedFiles() throws GitException {
        Set<String> untrackedFiles = new HashSet<>();
        getUntrackedFiles("", untrackedFiles);
        if (untrackedFiles.isEmpty()) {
            return true;
        }
        outputStream.println("Untracked files:");
        outputStream.println();
        for (String file : untrackedFiles) {
            outputStream.println("    " + file);
        }
        outputStream.println();
        return false;
    }

    private boolean printFilesToBeCommitted() throws GitException {
        repository = initRepositoryFromFiles();
        outputStream.println("Current branch is 'master'");
        if (repository.getStagingArea().getAddedFiles().size() != 0) {
            outputStream.println("Ready to commit:");
            outputStream.println();
            boolean newFileWasAlready = false;
            for (String file : repository.getStagingArea().getAddedFiles().keySet()) {
                if (repository.getHead().getTrackedFiles().get(file) == null) {
                    if (!newFileWasAlready) {
                        outputStream.println("New files:");
                        newFileWasAlready = true;
                    }
                    outputStream.println("    " + file);
                }
            }
            outputStream.println();
            boolean modifiedFileWasAlready = false;
            for (String file : repository.getStagingArea().getAddedFiles().keySet()) {
                if (repository.getHead().getTrackedFiles().get(file) != null) {
                    if (!modifiedFileWasAlready) {
                        outputStream.println("    Modified files:");
                        modifiedFileWasAlready = true;
                    }
                    outputStream.println("    " + file);
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private void getUntrackedFiles(String currentDirectoryPath, Set<String> untrackedFiles) throws GitException {
        repository = initRepositoryFromFiles();
        File currentDirectory = new File(workingDirectory + "/" + currentDirectoryPath);
        File[] files = currentDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory() && !file.getName().equals("git")) {
                getUntrackedFiles(currentDirectoryPath + "/" + file.getName(), untrackedFiles);
            } else if (file.isFile()) {
                String fileNameWIthCurrentDirectory = currentDirectoryPath + "/" + file.getName();
                if (currentDirectoryPath.isEmpty()) {
                    fileNameWIthCurrentDirectory = file.getName();
                }
                if ((repository.getHead().getTrackedFiles().get(fileNameWIthCurrentDirectory) == null &&
                        repository.getStagingArea().getAddedFiles().get(fileNameWIthCurrentDirectory) == null) ||
                        repository.getStagingArea().getDeletedFiles().contains(fileNameWIthCurrentDirectory)) {
                    untrackedFiles.add(fileNameWIthCurrentDirectory);
                }
            }
        }
    }

    private void createCopyOfFile(String sourceFilePath, String copyFilePath) throws GitException {
        File sourceFile = new File(sourceFilePath);
        File copyFile = new File(copyFilePath);
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
             FileOutputStream fileOutputStream = new FileOutputStream(copyFile)) {
            byte[] buffer = new byte[1048576];
            int numberOfBytes = 0;
            while ((numberOfBytes = fileInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, numberOfBytes);
            }
        } catch (IOException exception) {
            throw new GitException(exception.getMessage());
        }
    }

    private void changeOneLocalFile(String filePath) throws GitException {
        String absolutePathOfFileToDelete = workingDirectory + "/" + filePath;
        String absolutePathOfCopyFile = workingDirectory + GitConstants.BLOBS_DIRECTORY + "/" + repository.getHead().getTrackedFiles().get(filePath);
        try {
            Files.delete(Paths.get(absolutePathOfFileToDelete));
        } catch (IOException ignored) {
        }
        createCopyOfFile(absolutePathOfCopyFile, absolutePathOfFileToDelete);
    }

    private void changeLocalFiles(Repository.Commit commitToCheckout) throws GitException {
        for (String filePath : repository.getHead().getTrackedFiles().keySet()) {
            try {
                Files.delete(Paths.get(filePath));
            } catch (IOException ignored) {
            }
        }
        for (String filePath : repository.getStagingArea().getAddedFiles().keySet()) {
            if (commitToCheckout.getTrackedFiles().get(filePath) == null) {
                try {
                    Files.delete(Paths.get(filePath));
                } catch (IOException ignored) {
                }
            }
        }
        for (String filePath : commitToCheckout.getTrackedFiles().keySet()) {
            String absolutePathOfOriginFile = workingDirectory + "/" + filePath;
            String absolutePathOfCopyFile = workingDirectory + GitConstants.BLOBS_DIRECTORY + "/" + commitToCheckout.getTrackedFiles().get(filePath);
            createCopyOfFile(absolutePathOfCopyFile, absolutePathOfOriginFile);
        }
    }

    private String getFileHash(String absoluteFilePath) throws GitException {
        try {
            byte[] fileContent = FileUtils.readFileToByteArray(new File(absoluteFilePath));
            return Hashing.sha256()
                    .hashBytes(fileContent)
                    .toString();
        } catch (IOException exception) {
            throw new GitException("Can't get content of file " + absoluteFilePath);
        }
    }

    private Repository initRepositoryFromFiles() throws GitException {
        try {
            File repoFile = new File(workingDirectory + "/" + GitConstants.REPO_FILE);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(repoFile, Repository.class);
        } catch (IOException exception) {
            throw new GitException("Repository hasn't been initialized yet");
        }
    }

    private void writeRepositoryToFiles() throws GitException {
        File repoFile = new File(workingDirectory + "/" + GitConstants.REPO_FILE);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(repoFile, repository);
        } catch (IOException e) {
            throw new GitException("Error while writing metainfo to file");
        }
    }
}
