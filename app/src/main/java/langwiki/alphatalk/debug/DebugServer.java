package org.langwiki.alphatalk.debug;

import org.langwiki.alphatalk.debug.cli.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Debug server provides a command line interface (CLI) <p>
 *
 * The commands are defined in {@link ShellCommandHandler}. For example,
 * you can use the command 'lua' to enter lua mode, and the command 'js'
 * to enter Javascript mode. Type 'exit' to exit from the script shell,
 * or the top-level shell. <p>
 *
 * To connect to the debug server, you can use telnet from Linux, or
 * putty from Windows. If Windows environment, you would need to configure
 * the terminal to add a CR (\r) for each LF (\n) for proper display.
 */
public class DebugServer implements Runnable {
    public static final int DEFAULT_DEBUG_PORT = 1868;
    public static final int NUM_CLIENTS = 2;
    public static final boolean SIMULATE_TELNET = true;

    private static final String PROMPT = "α-talk";
    private static final String APP_NAME = "LangWiki AlphaTalk Framework";

    // Need a way to stop the program...
    private boolean shuttingDown;
    private ServerSocket serverSocket;

    int port;
    int maxClients;

    class DebugConnection implements Callable<Object> {
        private Socket socket;

        public DebugConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public Object call() throws Exception {
            // Hand over to shell
            Shell shell;
            try {
                if (SIMULATE_TELNET) {
                    // Supporting editing and history
                    shell = ConsoleFactory.createTelnetConsoleShell(PROMPT, APP_NAME,
                            new ShellCommandHandler(),
                            socket.getInputStream(), socket.getOutputStream());
                } else {
                    // Simple console
                    PrintStream out = new PrintStream(socket.getOutputStream());
                    shell = ConsoleFactory.createConsoleShell(PROMPT, APP_NAME,
                            new ShellCommandHandler(),
                            new BufferedReader(new InputStreamReader(socket.getInputStream())),
                            out, out, null);
                }

                shell.commandLoop();
            } catch (Exception e) {
                e.printStackTrace();
                // socket streams are closed
            } finally {
                socket.close();
            }
            return null;
        }
    }

    /**
     * Constructor.
     *
     * To start a debug server, add the following line to your code:
     *
     * <pre>
     *     Threads.spawn(new DebugServer());
     * </pre>
     *
     * The default port is 1645, and the default maximum number of connections is 2.
     *
     */
    public DebugServer() {
        this(DEFAULT_DEBUG_PORT, NUM_CLIENTS);
    }

    /**
     * Constructor.
     *
     * @param port
     *     The port to override the default port 1645.
     * @param maxClients
     *     Maximum number of clients.
     */
    public DebugServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
    }

    /**
     * Shuts down the server. Active connections are not affected.
     */
    public void shutdown() {
        shuttingDown = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the server.
     */
    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(maxClients);
        try {
            serverSocket = new ServerSocket(port, maxClients);
            while (!shuttingDown) {
                try {
                    Socket socket = serverSocket.accept();
                    executorService.submit(new DebugConnection(socket));
                } catch (SocketException e) {
                    // closed
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (Exception e) {
            }
            executorService.shutdownNow();
        }
    }
}