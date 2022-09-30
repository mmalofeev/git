package ru.hse.fmcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Git {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Not enough arguments");
            return;
        }
        GitCli gitCli = new GitCliImpl(args[0]);
        List<String> arguments = new ArrayList<String>();
        arguments.addAll(Arrays.asList(args).subList(2, args.length));
        gitCli.setOutputStream(System.out);
        try {
            gitCli.runCommand(args[1], arguments);
        } catch (GitException exception) {
            System.out.println(exception.getMessage());
        }
    }
}
