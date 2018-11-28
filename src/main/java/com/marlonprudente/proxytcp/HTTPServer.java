/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marlonprudente.proxytcp;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.logging.*;
import java.net.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;

/**
 *
 * @author Marlon Prudente <marlon.oliveira at alunos.utfpr.edu.br>
 */
public class HTTPServer {

    private static final Logger log = Logger.getLogger(HTTPServer.class.getName());

    public static void main(String args[]) {

        try {
            Handler fileHandler = new FileHandler("./logs/logMain.log");
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            log.addHandler(fileHandler);
            log.info("Logger name: " + log.getName());
            int serverPort = 9999; // the server port
            ServerSocket listenSocket = new ServerSocket(serverPort);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                log.info("Nova conexão iniciada -" + clientSocket.getRemoteSocketAddress());
                new Connection(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen socket:" + e.getMessage());
        }
    }
}

class Connection extends Thread {

    /**
     * Client input stream for reading request
     */
    protected DataInputStream clientInputStream;
    /**
     * Client output stream for rendering response
     */
    protected OutputStream clientOutputStream;
    /**
     * Remote output stream to send in client's request
     */
    protected OutputStream remoteOutputStream;
    /**
     * Remote input stream to read back response to client
     */
    protected InputStream remoteInputStream;
    /**
     * Client socket object
     */
    protected Socket clientSocket;
    /**
     * Remote socket object
     */
    protected Socket remoteSocket;
    /**
     * Client request type (Only "GET" or "POST" are handled)
     */
    protected String requestType;
    /**
     * Client request url (e.g. http://www.google.com)
     */
    protected String url;
    /**
     * Client request uri parsed from url (e.g. /index.html)
     */
    protected String uri;
    /**
     * Client request version (e.g. HTTP/1.1)
     */
    protected String httpVersion;
    /**
     * Data structure to hold all client request handers (e.g. proxy-connection:
     * keep-alive)
     */
    protected HashMap<String, String> header;
    /**
     * End of line character
     */
    static String endOfLine = "\r\n";
    /**
     * websites allowed to connect
     */
    protected List<String> allowed = new ArrayList();

    //private static final Logger log = Logger.getLogger(HTTPServer.class.getName());
    public Connection(Socket aClientSocket) {
        clientSocket = aClientSocket;
        try {
//            Handler fileHandler = new FileHandler("./logs/logThread.log");
//            fileHandler.setFormatter(new SimpleFormatter());
//            fileHandler.setLevel(Level.ALL);
//            log.addHandler(fileHandler);
            allowed.add("portal.utfpr.edu.br");
            allowed.add("utfpr.edu.br");
            header = new HashMap<String, String>();
        } catch (Exception e) {
            //log.severe("Erro ao Criar Connection: " + e);
        }
        this.start();
    }

    public void run() {
        try {
            clientInputStream = new DataInputStream(clientSocket.getInputStream());
            clientOutputStream = clientSocket.getOutputStream();
            boolean siteBloqueado = true;
            
            // step 1) get request from client
            clientToProxy();
            System.out.println("Host: " + header.get("host"));
            for (String site : allowed) {
                if (site.startsWith(header.get("host"))) {
                    System.out.println("EqualsMethod: " + header.get("host"));
                    siteBloqueado = false;
                }
            }
            if (siteBloqueado) {
                SendResponseBlockedSite(200, "<p>Site bloqueado!</p>", false);
            } else {
                // step 2) forward request to remote host
                proxyToRemote();

                // step 3) read response from remote back to client
                remoteToClient();
            }

            if (remoteOutputStream != null) {
                remoteOutputStream.close();
            }
            if (remoteInputStream != null) {
                remoteInputStream.close();
            }
            if (remoteSocket != null) {
                remoteSocket.close();
            }

            if (clientOutputStream != null) {
                clientOutputStream.close();
            }
            if (clientInputStream != null) {
                clientInputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }

        } catch (Exception e) {
            //log.severe("EOF:" + e.getMessage());
        } finally {
//            try {
//
//                
//            } catch (IOException e) {/*close failed*/
//                log.severe("Falha ao fechar socket.");
//            }
        }

    }

    /**
     * Receive and pre-process client's request headers before redirecting to
     * remote server
     *
     */
    @SuppressWarnings("deprecation")
    private void clientToProxy() {
        String line, key, value;
        StringTokenizer tokens;
        try {
            // HTTP Command
            if ((line = clientInputStream.readLine()) != null) {
                tokens = new StringTokenizer(line);
                requestType = tokens.nextToken();
                url = tokens.nextToken();
                httpVersion = tokens.nextToken();
            }

            // Header Info
            while ((line = clientInputStream.readLine()) != null) {
                // check for empty line
                if (line.trim().length() == 0) {
                    break;
                }
                // tokenize every header as key and value pair
                tokens = new StringTokenizer(line);
                key = tokens.nextToken(":");
                value = line.replaceAll(key, "").replace(": ", "");
                header.put(key.toLowerCase(), value);
            }
            stripUnwantedHeaders();
            getUri();
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Sending pre-processed client request to remote server
     *
     */
    private void proxyToRemote() {

        try {
            if (header.get("host") == null) {
                return;
            }
            if (!requestType.startsWith("GET") && !requestType.startsWith("POST")) {
                return;
            }
            remoteSocket = new Socket(header.get("host"), Integer.valueOf(header.get("porta")));
            remoteOutputStream = remoteSocket.getOutputStream();

            // make sure streams are still open
            checkRemoteStreams();
            checkClientStreams();

            // make request from client to remote server
            String request = requestType + " " + uri + " HTTP/1.0";
            remoteOutputStream.write(request.getBytes());
            remoteOutputStream.write(endOfLine.getBytes());
            System.out.println(request);
            // send hostname
            String command = "host: " + header.get("host");
            remoteOutputStream.write(command.getBytes());
            remoteOutputStream.write(endOfLine.getBytes());
            System.out.println(command);
            // send rest of the headers
            for (String key : header.keySet()) {
                if (!key.equals("host")) {
                    command = key + ": " + header.get(key);
                    remoteOutputStream.write(command.getBytes());
                    remoteOutputStream.write(endOfLine.getBytes());
                    System.out.println(command);
                }
            }

            remoteOutputStream.write(endOfLine.getBytes());
            remoteOutputStream.flush();
            // send client request data if its a POST request
            if (requestType.startsWith("POST")) {
                int contentLength = Integer.parseInt(header.get("content-length"));
                for (int i = 0; i < contentLength; i++) {
                    remoteOutputStream.write(clientInputStream.read());
                }
            }
            // complete remote server request
            remoteOutputStream.write(endOfLine.getBytes());
            remoteOutputStream.flush();
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Sending buffered remote server response back to client with minor header
     * processing
     *
     */
    @SuppressWarnings("deprecation")
    private void remoteToClient() {

        try {

            // If socket is closed, return
            if (remoteSocket == null) {
                return;
            }

            String line;
//            String blockedMensagemHTML = "<html>\n"
//                    + "  <head>\n"
//                    + "    <title>Exemplo de resposta HTTP </title>\n"
//                    + "  </head>\n"
//                    + "    <body>\n"
//                    + "    Acesso não autorizado!\n"
//                    + "    </body>\n"
//                    + "  </html>";
            DataInputStream remoteOutHeader = new DataInputStream(remoteSocket.getInputStream());
//            boolean blocked = false;
//            for (String sitePermitido : allowed) {
//                if (!header.get("host").contains(sitePermitido)) {
//                    blocked = true;
//                }
//            }
            // get remote response header
            while ((line = remoteOutHeader.readLine()) != null) {
                // check for end of header blank line
                if (line.trim().length() == 0) {
                    break;
                }

                // check for proxy-connection: keep-alive
                if (line.toLowerCase().startsWith("proxy")) {
                    continue;
                }
                if (line.contains("keep-alive")) {
                    continue;
                }

                // write remote response to client
                System.out.println(line);

                //colocar condicao de bloqueio aqui
                clientOutputStream.write(line.getBytes());
                clientOutputStream.write(endOfLine.getBytes());
            }
//            if (blocked) {
//                // check for end of header blank line
//                if (line.trim().length() == 0) {
//                    return;
//                }
//
//                //colocar condicao de bloqueio aqui
//                clientOutputStream.write("HTTP/1.1 200 OK".getBytes());
//                clientOutputStream.write("Server: Microsoft-IIS/4.0".getBytes());
//                clientOutputStream.write("Date: Mon, 3 Jan 2016 17:13:34 GMT".getBytes());
//                clientOutputStream.write("Content-Type: text/html".getBytes());
//                clientOutputStream.write("Last-Modified: Mon, 11 Jan 2016 17:24:42 GMT".getBytes());
//                clientOutputStream.write("Content-Length: 112".getBytes());
//                clientOutputStream.write(blockedMensagemHTML.getBytes());
//                clientOutputStream.write(endOfLine.getBytes());
//            }
            // complete remote header response
            clientOutputStream.write(endOfLine.getBytes());
            clientOutputStream.flush();

            // get remote response body
            remoteInputStream = remoteSocket.getInputStream();
            byte[] buffer = new byte[1024];

            // buffer remote response then write it back to client
            for (int i; (i = remoteInputStream.read(buffer)) != -1;) {
                clientOutputStream.write(buffer, 0, i);
                clientOutputStream.flush();
            }
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Helper function to strip out unwanted request header from client
     *
     */
    private void stripUnwantedHeaders() {

        if (header.get("host").contains(":")) {
            String site = header.get("host").split(":")[0];
            String port = header.get("host").split(":")[1];
            header.remove("host");
            header.put("host", site);
            header.put("porta", port);
        } else {
            header.put("porta", "80");
        }

        if (header.containsKey("user-agent")) {
            header.remove("user-agent");
        }
        if (header.containsKey("referer")) {
            header.remove("referer");
        }
        if (header.containsKey("proxy-connection")) {
            header.remove("proxy-connection");
        }
        if (header.containsKey("connection") && header.get("connection").equalsIgnoreCase("keep-alive")) {
            header.remove("connection");
        }
    }

    /**
     * Helper function to check for client input and output stream, reconnect if
     * closed
     *
     */
    private void checkClientStreams() {

        try {
            if (clientSocket.isOutputShutdown()) {
                clientOutputStream = clientSocket.getOutputStream();
            }
            if (clientSocket.isInputShutdown()) {
                clientInputStream = new DataInputStream(clientSocket.getInputStream());
            }
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Helper function to check for remote input and output stream, reconnect if
     * closed
     *
     */
    private void checkRemoteStreams() {

        try {
            if (remoteSocket.isOutputShutdown()) {
                remoteOutputStream = remoteSocket.getOutputStream();
            }
            if (remoteSocket.isInputShutdown()) {
                remoteInputStream = new DataInputStream(remoteSocket.getInputStream());
            }
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Helper function to parse URI from full URL
     *
     */
    private void getUri() {

        if (header.containsKey("host")) {
            int temp = url.indexOf(header.get("host"));
            temp += header.get("host").length();

            if (temp < 0) {
                // prevent index out of bound, use entire url instead
                uri = url;
            } else {
                // get uri from part of the url
                uri = url.substring(temp);
            }
        }
    }

    public void SendResponseBlockedSite(int statusCode, String responseString, boolean isFile) throws Exception {
        String statusLine = null;
        String serverdetails = "Proxy: Java Blocking Proxy";
        String contentLengthLine = null;
        String fileName = null;
        String contentTypeLine = "Content-Type: text/html" + "\r\n";
        FileInputStream fin = null;
        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
        String HTML_START
                = "<html>"
                + "<title>HTTP Server in java</title>"
                + "<body>";

        String HTML_END
                = "</body>"
                + "</html>";
        if (statusCode == 200) {
            statusLine = "HTTP/1.1 200 OK" + "\r\n";
        } else {
            statusLine = "HTTP/1.1 404 Not Found" + "\r\n";
        }

        if (isFile) {
            fileName = responseString;
            fin = new FileInputStream(fileName);
            contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
            if (!fileName.endsWith(".htm") && !fileName.endsWith(".html")) {
                contentTypeLine = "Content-Type: \r\n";
            }
        } else {
            responseString = HTML_START + responseString + HTML_END;
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }

        dos.writeBytes(statusLine);
        dos.writeBytes(serverdetails);
        dos.writeBytes(contentTypeLine);
        dos.writeBytes(contentLengthLine);
        dos.writeBytes("Connection: close\r\n");
        dos.writeBytes("\r\n");

        dos.writeBytes(responseString);

        dos.close();
    }

}
