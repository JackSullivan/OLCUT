/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package com.sun.labs.util.command;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.StringReader;

/**
 * This class is a command interpreter. It reads strings from an
 * input stream, parses them into commands and executes them, results
 * are sent back on the output stream.
 *
 * @see CommandInterpreter
 */
public class CommandInterpreter extends Thread {

    private Map commandList;
    private int totalCommands = 0;
    private String prompt;
    private boolean done = false;
    private boolean trace = false;
    private CommandHistory history = new CommandHistory();
    private BufferedReader in;

    private String defaultCommand;

    /** 
     * Creates a command interpreter that won't read a stream.
     *
     */
    public CommandInterpreter() {
        commandList = new HashMap();
        addStandardCommands();
        in = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Sets the trace mode of the command interpreter.
     *
     *  @param trace 	true if tracing.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * Sets a default command to be used as a prefix for a string that isn't
     * a recognized command.
     */
    public void setDefaultCommand(String defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    /**
     * Adds the set of standard commands
     *
     */
    private void addStandardCommands() {
        add("help", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                dumpCommands();
                return "";
            }

            public String getHelp() {
                return "lists available commands";
            }
        });

        add("history", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                history.dump();
                return "";
            }

            public String getHelp() {
                return "shows command history";
            }
        });

        add("status", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                putResponse("Total number of commands: " + totalCommands);
                return "";
            }

            public String getHelp() {
                return "shows command status";
            }
        });

        add("echo", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                StringBuffer b = new StringBuffer(80);

                for (int i = 1; i < args.length; i++) {
                    b.append(args[i]);
                    b.append(" ");
                }
                putResponse(b.toString());
                return "";
            }

            public String getHelp() {
                return "display a line of text";
            }
        });

        add("menu", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length < 2) {
                    return "usage: menu command-number [args]";
                } else {
                    try {
                        int which = Integer.parseInt(args[1]);
                        String cmd = getCommandByNumber(which);
                        if (cmd == null) {
                            return "can't find that command";
                        } else {
                            String[] subargs = new String[args.length - 1];
                            if (args.length > 2) {
                                System.arraycopy(args, 2, subargs,
                                        1, subargs.length - 1);
                            }
                            subargs[0] = cmd;
                            return CommandInterpreter.this.execute(subargs);
                        }
                    } catch (NumberFormatException e) {
                        return "bad number format";
                    }
                }
            }

            public String getHelp() {
                return "execute a command by number";
            }
        });

        addAlias("menu", "m");

        if (false) {
            add("argtest", new CommandInterface() {

                public String execute(CommandInterpreter ci, String[] args) {
                    StringBuffer b = new StringBuffer(80);

                    System.out.println("arg length is " + args.length);
                    for (int i = 0; i < args.length; i++) {
                        b.append(args[i]);
                        b.append("\n");
                    }
                    putResponse(b.toString());
                    return "";
                }

                public String getHelp() {
                    return "argument test";
                }
            });
        }

        add("quit", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                done = true;
                return "";
            }

            public String getHelp() {
                return "exit the shell";
            }
        });

        add("on_exit", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                return "";
            }

            public String getHelp() {
                return "command executed upon exit";
            }
        });

        add("version", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                putResponse("Command Interpreter - Version 1.1 ");
                return "";
            }

            public String getHelp() {
                return "displays version information";
            }
        });

        add("gc", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                Runtime.getRuntime().gc();
                return "";
            }

            public String getHelp() {
                return "performs garbage collection";
            }
        });

        add("memory", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                long totalMem = Runtime.getRuntime().totalMemory();
                long freeMem = Runtime.getRuntime().freeMemory();

                putResponse("Free Memory  : " + freeMem / (1024.0 * 1024) + " mbytes");
                putResponse("Total Memory : " + totalMem / (1024.0 * 1024) + " mbytes");
                return "";
            }

            public String getHelp() {
                return "shows memory statistics";
            }
        });


        add("delay", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length == 2) {
                    try {
                        float seconds = Float.parseFloat(args[1]);
                        Thread.sleep((long) (seconds * 1000));
                    } catch (NumberFormatException nfe) {
                        putResponse("Usage: delay time-in-seconds");
                    } catch (InterruptedException ie) {
                    }
                } else {
                    putResponse("Usage: delay time-in-seconds");
                }
                return "";
            }

            public String getHelp() {
                return "pauses for a given number of seconds";
            }
        });

        add("alias", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length == 3) {
                    String alias = args[1];
                    String cmd = args[2];
                    CommandInterpreter.this.addAlias(cmd, alias);
                } else {
                    putResponse("Usage: alias name def");
                }
                return "";
            }

            public String getHelp() {
                return "adds a pseudonym or shorthand term for a command";
            }
        });

        add("repeat", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length >= 3) {
                    try {
                        int count = Integer.parseInt(args[1]);
                        String[] subargs = new String[args.length - 2];
                        System.arraycopy(args, 2, subargs, 0, subargs.length);

                        for (int i = 0; i < count; i++) {
                            putResponse(
                                    CommandInterpreter.this.execute(subargs));
                        }
                    } catch (NumberFormatException nfe) {
                        putResponse("Usage: repeat count command args");
                    }
                } else {
                    putResponse("Usage: repeat count command args");
                }
                return "";
            }

            public String getHelp() {
                return "repeatedly execute a command";
            }
        });

        add("redirect", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length >= 3) {
                    try {
                        String[] subargs = new String[args.length - 2];
                        System.arraycopy(args, 2, subargs, 0, subargs.length);
                        PrintStream oldOut = System.out;
                        PrintStream newOut = new PrintStream(args[1]);
                        System.setOut(newOut);
                        putResponse(CommandInterpreter.this.execute(subargs));
                        newOut.close();
                        System.setOut(oldOut);
                    } catch (IOException ioe) {
                        System.err.println("Can't write to " + args[1] + " " + ioe);
                    }
                } else {
                    putResponse("Usage: redirect file command [args ...]");
                }
                return "";
            }

            public String getHelp() {
                return "redirect command output to a file";
            }
        });

        add("load", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length == 2) {
                    if (!load(args[1])) {
                        putResponse("load: trouble loading " + args[1]);
                    }
                } else {
                    putResponse("Usage: load filename");
                }
                return "";
            }

            public String getHelp() {
                return "load and execute commands from a file";
            }
        });

        add("chain", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length > 1) {
                    String[] subargs = new String[args.length - 1];
                    List commands = new ArrayList(5);
                    int count = 0;
                    for (int i = 1; i < args.length; i++) {
                        if (args[i].equals(";")) {
                            if (count > 0) {
                                String[] trimmedArgs = new String[count];
                                System.arraycopy(subargs, 0, trimmedArgs,
                                        0, trimmedArgs.length);
                                commands.add(trimmedArgs);
                                count = 0;
                            }
                        } else {
                            subargs[count++] = args[i];
                        }
                    }

                    if (count > 0) {
                        String[] trimmedArgs = new String[count];
                        System.arraycopy(subargs, 0, trimmedArgs,
                                0, trimmedArgs.length);
                        commands.add(trimmedArgs);
                        count = 0;
                    }

                    for (Iterator i = commands.iterator(); i.hasNext();) {
                        putResponse(CommandInterpreter.this.execute(
                                (String[]) i.next()));
                    }
                } else {
                    putResponse("Usage: chain cmd1 ; cmd2 ; cmd3 ");
                }
                return "";
            }

            public String getHelp() {
                return "execute multiple commands on a single line";
            }
        });

        add("time", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length > 1) {
                    String[] subargs = new String[args.length - 1];
                    System.arraycopy(args, 1, subargs, 0, subargs.length);
                    long startTime = System.currentTimeMillis();
                    long endTime;

                    putResponse(CommandInterpreter.this.execute(subargs));
                    endTime = System.currentTimeMillis();

                    putResponse("Time: " + ((endTime - startTime) / 1000.0) + " seconds");

                } else {
                    putResponse("Usage: time cmd [args]");
                }
                return "";
            }

            public String getHelp() {
                return "report the time it takes to run a command";
            }
        });

        add("mstime", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) {
                if (args.length > 1) {
                    String[] subargs = new String[args.length - 1];
                    System.arraycopy(args, 1, subargs, 0, subargs.length);
                    long startTime = System.nanoTime();
                    putResponse(CommandInterpreter.this.execute(subargs));
                    putResponse(String.format("Time: %.3f ms", (System.nanoTime() - startTime) / 1000000.0));
                } else {
                    putResponse("Usage: time cmd [args]");
                }
                return "";
            }

            public String getHelp() {
                return "report the time it takes to run a command";
            }
        });
    }

    /**
     * Dumps the commands in the interpreter
     *
     * @param numbered if true number the commands
     *
     */
    private void dumpCommands() {
        int count = 0;
        Set unsortedKeys = commandList.keySet();
        Set sortedKeys = new TreeSet(unsortedKeys);

        for (Iterator i = sortedKeys.iterator(); i.hasNext();) {
            String cmdName = (String) i.next();
            String help = ((CommandInterface) commandList.get(cmdName)).getHelp();
            putResponse(count + ") " + cmdName + " - " + help);
            count++;
        }
    }

    private String getCommandByNumber(int which) {
        int count = 0;
        Set unsortedKeys = commandList.keySet();
        Set sortedKeys = new TreeSet(unsortedKeys);

        for (Iterator i = sortedKeys.iterator(); i.hasNext();) {
            String cmdName = (String) i.next();
            if (count == which) {
                return cmdName;
            }
            count++;
        }
        return null;
    }

    /**
     * Adds the given command to the command list.
     *
     * @param name	the name of the command.
     * @param command	the command to be executed.
     *
     */
    public void add(String name, CommandInterface command) {
        commandList.put(name, command);
    }

    /**
     * Adds an alias to the command
     *
     * @param command	the name of the command.
     * @param alias	the new aliase
     *
     */
    public void addAlias(String command, String alias) {
        commandList.put(alias, commandList.get(command));
    }

    /**
     * Add the given set of commands to the list
     * of commands.
     * @param newCommands 	the new commands to add to this interpreter.
     */
    public void add(Map newCommands) {
        commandList.putAll(newCommands);
    }

    /**
     * Outputs a response to the sender.
     *
     * @param response 	the response to send.
     *
     */
    public synchronized void putResponse(String response) {
        if (response != null && response.length() > 0) {
            System.out.println(response);
        }
    }

    /**
     * Called when the interpreter is exiting. Default behavior is
     * to execute an "on_exit" command.
     */
    protected void onExit() {
        execute("on_exit");
        System.out.println("----------\n");
    }

    /**
     * Execute the given command.
     *
     *  @param args	command args, args[0] contains name of cmd.
     */
    public String execute(String[] args) {
        return execute(args, true);
    }

    /**
     * Execute the given command.
     *
     *  @param args	command args, args[0] contains name of cmd.
     */
    private String execute(String[] args, boolean first) {
        String response = "";

        CommandInterface ci;

        if (args.length > 0) {

            ci = (CommandInterface) commandList.get(args[0]);
            if (ci != null) {
                try {
                    response = ci.execute(this, args);
                } catch (Exception ex) {
                    response = "ERR command exception " + ex.getMessage();
                    ex.printStackTrace();
                }
            } else {
                if(first && defaultCommand != null) {
                    String[] newArgs = new String[args.length+1];
                    newArgs[0] = defaultCommand;
                    System.arraycopy(args, 0, newArgs, 1,
                                     args.length);
                    return execute(args, false);
                }
                response = "ERR  CMD_NOT_FOUND";
            }

            totalCommands++;
        }
        return response;
    }

    /**
     * Execute the given command string.
     * 
     * @param cmdString the command string.
     *
     */
    public String execute(String cmdString) {
        if (trace) {
            System.out.println("Execute: " + cmdString);
        }
        return execute(parseMessage(cmdString));
    }

    /**
     * Parses the given message into an array of strings.
     *
     * @param message 	the string to be parsed.
     * @return the parsed message as an array of strings
     */
    protected String[] parseMessage(String message) {
        int tokenType;
        List<String> words = new ArrayList<String>();
        StreamTokenizer st = new StreamTokenizer(new StringReader(message));

        st.resetSyntax();
        st.whitespaceChars(0, ' ');
        st.wordChars('!', 255);
        st.quoteChar('"');
        st.quoteChar('\"');
        st.commentChar('#');

        while (true) {
            try {
                tokenType = st.nextToken();
                if (tokenType == StreamTokenizer.TT_WORD) {
                    words.add(st.sval);
                } else if (tokenType == '\'' || tokenType == '"') {
                    words.add(st.sval);
                } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                    System.out.println("Unexpected numeric token!");
                } else {
                    break;
                }
            } catch (IOException e) {
                break;
            }
        }
        return (String[]) words.toArray(new String[words.size()]);
    }

    // inherited from thread.
    public void run() {
        while (!done) {
            try {
                printPrompt();
                String message = getInputLine();
                if (message == null) {
                    break;
                } else {
                    if (trace) {
                        System.out.println("\n----------");
                        System.out.println("In : " + message);
                    }
                    message = message.trim();
                    if (message.length() > 0) {
                        putResponse(execute(message));
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception: CommandInterpreter.run()");
                break;
            }
        }
        onExit();
    }
    // some history patterns used by getInputLine()
    private static Pattern historyPush = Pattern.compile("(.+):p");
    private static Pattern editPattern =
            Pattern.compile("\\^(.+?)\\^(.*?)\\^?");
    private static Pattern bbPattern = Pattern.compile("(!!)");

    /**
     * Gets the input line. Deals with history. Currently we support
     * simple csh-like history. !! - execute last command, !-3 execute
     * 3 from last command, !2 execute second command in history list,
     * !foo - find last command that started with foo and execute it.
     * Also allows editing of the last command wich ^old^new^ type 
     * replacesments
     *
     * @return the next history line or null if done
     */
    private String getInputLine() throws IOException {
        String message = in.readLine();
        if(in == null) {
            return null;
        }
        boolean justPush = false;
        boolean echo = false;
        boolean error = false;

        Matcher m = historyPush.matcher(message);
        if (m.matches()) {
            justPush = true;
            echo = true;
            message = m.group(1);
        }
        if (message.startsWith("^")) { // line editing ^foo^fum^
            m = editPattern.matcher(message);
            if (m.matches()) {
                String orig = m.group(1);
                String sub = m.group(2);
                try {
                    Pattern pat = Pattern.compile(orig);
                    Matcher subMatcher = pat.matcher(history.getLast(0));
                    if (subMatcher.find()) {
                        message = subMatcher.replaceFirst(sub);
                        echo = true;
                    } else {
                        error = true;
                        putResponse(message + ": substitution failed");
                    }
                } catch (PatternSyntaxException pse) {
                    error = true;
                    putResponse("Bad regexp: " + pse.getDescription());
                }
            } else {
                error = true;
                putResponse("bad substitution sytax, use ^old^new^");
            }
        } else if ((m = bbPattern.matcher(message)).find()) {
            message = m.replaceAll(history.getLast(0));
            echo = true;
        } else if (message.startsWith("!")) {
            if (message.matches("!\\d+")) {
                int which = Integer.parseInt(message.substring(1));
                message = history.get(which);
            } else if (message.matches("!-\\d+")) {
                int which = Integer.parseInt(message.substring(2));
                message = history.getLast(which - 1);
            } else {
                message = history.findLast(message.substring(1));
            }
            echo = true;
        }

        if (error) {
            return "";
        }

        if (message.length() > 0) {
            history.add(message);
        }

        if (echo) {
            putResponse(message);
        }
        return justPush ? "" : message;
    }

    public void close() {
        done = true;
    }

    /**
     * Prints the prompt.
     */
    private void printPrompt() {
        if (prompt != null) {
            System.out.print(prompt);
        }
    }

    public boolean load(String filename) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String inputLine;

            while ((inputLine = br.readLine()) != null) {
                String response = CommandInterpreter.this.execute(inputLine);
                if (!response.equals("OK")) {
                    putResponse(response);
                }
            }
            fr.close();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Sets the prompt for the interpreter
     *
     * @param prompt the prompt.
     *
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Gets the prompt for the interpreter
     *
     * @return the prompt.
     *
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * manual tester for the command interpreter.
     *
     */
    public static void main(String[] args) {
        CommandInterpreter ci = new CommandInterpreter();

        try {
            System.out.println("Welcome to the Command interpreter test program");
            ci.setPrompt("CI> ");
            ci.run();
            System.out.println("Goodbye!");
        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    class CommandHistory {

        private List history = new ArrayList(100);

        /**
         * Adds a command to the history
         *
         * @param command the command to add
         */
        public void add(String command) {
            history.add(command);
        }

        /**
         * Gets the most recent element in the history
         *
         * @param offset the offset from the most recent command
         * @return the last command executed
         */
        public String getLast(int offset) {
            if (history.size() > offset) {
                return (String) history.get((history.size() - 1) - offset);
            } else {
                putResponse("command not found");
                return "";
            }
        }

        /**
         * Gets the most recent element in the history
         *
         * @param which the offset from the most recent command
         * @return the last command executed
         */
        public String get(int which) {
            if (history.size() > which) {
                return (String) history.get(which);
            } else {
                putResponse("command not found");
                return "";
            }
        }

        /**
         * Finds the most recent message that starts with
         * the given string
         *
         * @param match the string to match
         * @return the last command executed that matches match
         */
        public String findLast(String match) {
            for (int i = history.size() - 1; i >= 0; i--) {
                String cmd = get(i);
                if (cmd.startsWith(match)) {
                    return cmd;
                }
            }
            putResponse("command not found");
            return "";
        }

        /**
         * Dumps the current history
         *
         */
        public void dump() {
            for (int i = 0; i < history.size(); i++) {
                String cmd = get(i);
                putResponse(i + " " + cmd);
            }
        }
    }
}
