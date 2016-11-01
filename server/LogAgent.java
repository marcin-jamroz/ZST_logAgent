package server;


import com.sun.org.apache.xalan.internal.lib.ExsltStrings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Server side (logAgent) of the event handler application
 *
 * @author Marcin Jamroz
 */
public class LogAgent {

    /**
     * server socket
     */
    private final ServerSocket serverSocket;

    /**
     * variable which is used to establish connection with database
     */
    private Connection connection = null;

    /**
     * variable which is used to update database
     */
    //private Statement statement = null;


    /**
     * creates server socket at a given port
     *
     * @param portNumber port number
     * @throws IOException
     */
    public LogAgent(int portNumber) throws IOException {
        serverSocket = new ServerSocket(portNumber);
    }


    /**
     * method which implement communication protocol between logAgent and logClient
     */
    public void startServer() {

        openDatabase();

        while (true) {
            try {
                System.out.println("waiting for client on port " +
                        serverSocket.getLocalPort() + "...");
                Socket clientSocket = serverSocket.accept();

                System.out.println("Just connected to" +
                        clientSocket.getRemoteSocketAddress());


                new Thread() {

                    DataInputStream inStream;

                    int i = 0;
                    int x = 0;
                    public void run() {
                        try {
                            inStream = new DataInputStream(clientSocket.getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Out before readMessage" + ++i); //for testing
                        while (clientSocket.isClosed()==false) {

                            System.out.println(clientSocket.getPort());
                            String message = readMessage(clientSocket, inStream);
                            System.out.println("Out after readMessage" + ++x); //for testing
                            System.out.println(message);

                            if (Objects.equals(message, "exit")) {
                                try {
                                    sendData("ok", clientSocket);
                                    clientSocket.close();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            String[] splittedMessage = message.split(" ");
                            insertData(splittedMessage,clientSocket);


                        }
                    }


                }.start();


            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * method which receive input data from the specific socket
     *
     * @param clientSocket socket to read
     * @return String containing data
     */
    public synchronized String readMessage(Socket clientSocket, DataInputStream inData) {
        //DataInputStream inData;
        String data = "";
        try {
            //inData = new DataInputStream(clientSocket.getInputStream());
            data = inData.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Sudden socket close");
            try {
                clientSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return data;
    }

    public void openDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:log.db");
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {

            e.printStackTrace();

        }
        System.out.println("Opened database successfully");
    }

    public synchronized void insertData(String[] splittedMessage, Socket clientSocket){

        Statement statement = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String comments = "";
        if(splittedMessage.length == 1){
            comments = ",'-'";
        }
        else {
            for (int i = 1; i < splittedMessage.length; i++) {
                comments += (",'" + splittedMessage[i]) + "'";
            }
        }

        String sql = "insert into " + splittedMessage[0] + " values ('" +
                clientSocket.getRemoteSocketAddress().toString().split("/")[1]
                + "',current_timestamp" +
                comments + ");";
        try {
            statement.executeUpdate(sql);
            statement.close();
            connection.commit();
        } catch (SQLException e) {
            System.out.println("No such table");
            e.printStackTrace();
        }

    }


    public synchronized void sendData(String data, Socket clientSocket) {

        try {
            OutputStream outData = clientSocket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outData);
            out.writeUTF(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        int portNumber = 5555;


        try {
            LogAgent logAgent = new LogAgent(portNumber);
            logAgent.startServer();
            logAgent.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
