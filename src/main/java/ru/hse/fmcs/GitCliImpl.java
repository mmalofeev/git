package ru.hse.fmcs;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class GitCliImpl implements GitCli {
    private final String workingDirectory;
    private PrintStream outputStream;
    ObjectMapper objectMapper = new ObjectMapper();

    GitCliImpl(String workingDir) {
        workingDirectory = workingDir;
    }

    public void runCommand(@NotNull String command, @NotNull List<@NotNull String> arguments) throws GitException {
        GitCommands gitCommands = new GitCommands(workingDirectory, outputStream);
        if (command.equals(GitConstants.INIT)) {
            gitCommands.init();
            return;
        }
        if (command.equals(GitConstants.CHECKOUT)) {
            if (arguments.get(0).equals("--")) {
                gitCommands.checkout(arguments);
            } else {
                gitCommands.checkout(arguments.get(0));
            }
            return;
        }
        if (command.equals(GitConstants.LOG)) {
            if (arguments.isEmpty()) {
                gitCommands.log(null);
            } else {
                gitCommands.log(arguments.get(0));
            }
            return;
        }
        if (command.equals(GitConstants.RESET)) {
            gitCommands.reset(arguments.get(0));
            return;
        }
        if (gitCommands.wasHeadDetached()) {
            outputStream.println("Error while performing " + command + ": Head is detached");
            return;
        }
        if (command.equals(GitConstants.ADD)) {
            gitCommands.add(arguments);
        }
        if (command.equals(GitConstants.COMMIT)) {
            gitCommands.commit(arguments.get(0));
        }
        if (command.equals(GitConstants.RM)) {
            gitCommands.remove(arguments);
        }
        if (command.equals(GitConstants.STATUS)) {
            gitCommands.status();
        }
    }

    /*
     * Установить outputStream, в который будет выводиться лог
     */
    public void setOutputStream(@NotNull PrintStream newOutputStream) {
        outputStream = newOutputStream;
    }

    /*
     * Вернуть хеш n-го перед HEAD коммита
     */
    public @NotNull String getRelativeRevisionFromHead(int n) throws GitException {
        GitCommands gitCommands = new GitCommands(workingDirectory, outputStream);
        return gitCommands.getCommit("HEAD~" + n).getCommitHash();
    }
}
