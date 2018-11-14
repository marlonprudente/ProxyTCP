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
    InputStreamReader isr;
    BufferedReader reader;
    byte[] request = new byte[1024];
    Socket clientSocket;
    public Connection(Socket aClientSocket) {
        clientSocket = aClientSocket;
        this.start();
    }

    public void run() {
        try {			                 // an echo server
            System.out.println("Antes da conexao");
            Socket s = new Socket("portal.utfpr.edu.br", 80);
            PrintWriter out = new PrintWriter(s.getOutputStream());            
            out.println("GET / HTTP/1.1");
            out.println("Host: portal.utfpr.edu.br");
            out.println("");
            out.flush();
            OutputStream os = s.getOutputStream();
            int bytes_read;
                    try {
                        while ((bytes_read = s.getInputStream().read(request)) != -1) {
                            os.write(request, 0, bytes_read);
                            os.flush();
                            //TODO CREATE YOUR LOGIC HERE
                        }
                    } catch (IOException e) {
                    }
            //reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
//            String inputLine;
//            while (((inputLine = reader.readLine()) != null)){
//                System.out.println("====>" + inputLine);
//                in.println(inputLine);
//            }
            
            //reader.close();
//            out.println("HTTP/1.1 200 OK");
//            out.println("Content-Type: text/html");
//            out.println("\r\n");
//            out.println("<p> Hello world </p>");

//            
//            System.out.println("Depois da conexao");
//            if (ClientIs == null || ClientOs == null) {
//                System.out.println("Falhou!");
//                return;
//            }
//            byte[] reply = new byte[4096];
//            int bytesRead;
//            while (-1 != (bytesRead = ClientIs.read(reply))) {
//                System.out.println("Enviando...");
//                ClientOs.write(reply, 0, bytesRead);
//            }
        }
         catch (EOFException e) {
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
