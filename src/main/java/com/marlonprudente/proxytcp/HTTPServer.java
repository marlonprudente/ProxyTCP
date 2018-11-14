/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marlonprudente.proxytcp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;

/**
 *
 * @author Marlon Prudente <marlon.oliveira at alunos.utfpr.edu.br>
 */
public class HTTPServer {

    public static void main(String args[]) {
        try {
            int serverPort = 8080; // the server port
            ServerSocket listenSocket = new ServerSocket(serverPort);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Connection c = new Connection(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen socket:" + e.getMessage());
        }
    }
}

class Connection extends Thread {

    Socket clientSocket;
    BufferedReader brIn;
    String urlPermitida = "portal.utfpr.edu.br";
    int portaPermitida = 80;
    Socket conexaoPermitida = null;

    public Connection(Socket aClientSocket) {
        clientSocket = aClientSocket;

        this.start();
    }

    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];

            final InputStream inFromClient = clientSocket.getInputStream();
            final OutputStream outToClient = clientSocket.getOutputStream();

            conexaoPermitida = new Socket(urlPermitida, portaPermitida);
            final InputStream inFromServer = conexaoPermitida.getInputStream();
            final OutputStream outToServer = conexaoPermitida.getOutputStream();

            brIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            while (!(inputLine = brIn.readLine()).equals("")) {
                if (inputLine.startsWith("Host:")) {
                    if (true) {

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
                        int bytes_read;

                        while ((bytes_read = inFromServer.read(reply)) != -1) {
                            outToClient.write(reply, 0, bytes_read);
                            outToClient.flush();
                            //TODO CREATE YOUR LOGIC HERE
                        }

                    } else {
                        PrintWriter out = new PrintWriter(outToClient);
                        out.println("GET / HTTP/1.1");
                        out.println("Não permitido!");
                        out.println("");
                        out.flush();
                    }
                }
                System.out.println(inputLine);
            }

        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {/*close failed*/
            }
        }

    }
}
