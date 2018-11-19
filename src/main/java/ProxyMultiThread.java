import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author jcgonzalez.com
 * @author  Marlon Prudente
 *
 */
public class ProxyMultiThread {
    public static void main(String[] args) {
        try {
//            if (args.length != 3)
//                throw new IllegalArgumentException("insuficient arguments");
            // and the local port that we listen for connections on
            String host = "portal.utfpr.edu.br";
            int remoteport = 80;
            int localport = 9999;
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remoteport
                    + " on port " + localport);
            ServerSocket server = new ServerSocket(localport);
            while (true) {
                new ThreadProxy(server.accept(), host, remoteport);
            }
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java ProxyMultiThread "
                    + "<host> <remoteport> <localport>");
        }
    }
}
/**
 * Handles a socket connection to the proxy server from the client and uses 2
 * threads to proxy between server and client
 *
 * @author Marlon Prudente
 *
 */
class ThreadProxy extends Thread {
    private Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;
    ThreadProxy(Socket sClient, String ServerUrl, int ServerPort) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        
        this.start();
    }
    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];
            sClient.setSoTimeout(2000);
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();
            Socket client = null, server = null;            
           BufferedReader brIn = new BufferedReader(new InputStreamReader(inFromClient));
            String inputLine;
            String host = "";
            if((inputLine = brIn.readLine()) != null && !inputLine.equals("")) {
                host = brIn.readLine();                
            }
//            brIn.close();
            // connects a socket to the server
            try {
                server = new Socket(SERVER_URL, SERVER_PORT);
                System.out.println(host);
//                if(host.contains("Host")){
//                    if(!(host.contains("utfpr.edu.br") || host.contains("firefox.com"))){
//                        System.out.println("Erro...");
//                        throw new Exception("Site não permitido");
//                   }
//                }

            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        outToClient));
                out.flush();
                throw new RuntimeException(e);
            } catch (Exception ex) {
                PrintWriter out = new PrintWriter(outToClient);
                out.println("GET / HTTP/1.1");
                out.println(ex);
                out.println("");
                out.flush();
            }
            // a new thread to manage streams from server to client (DOWNLOAD)
            final InputStream inFromServer = server.getInputStream();
            final OutputStream outToServer = server.getOutputStream();
            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            outToServer.write(request, 0, bytes_read);
                            outToServer.flush();
                            //TODO CREATE YOUR LOGIC HERE
                        }
                    } catch (IOException e) {
                    }
                    try {
                        outToServer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            // current thread manages streams from server to client (DOWNLOAD)
            int bytes_read;
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outToClient.write(reply, 0, bytes_read);
                    outToClient.flush();
                    //TODO CREATE YOUR LOGIC HERE
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (server != null)
                        server.close();
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outToClient.close();
            sClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}