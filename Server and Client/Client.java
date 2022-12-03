import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TCPClient {

    // server host and port number, which would be acquired from command line
    // parameter
    private static String serverHost;
    private static Integer serverPort;
    private static Integer UDPserverPort;
    private static boolean exit;

    public static class UDPServer extends Thread {
        public void run() {
            while (!exit) {
                try (DatagramSocket UDPserverSocket = new DatagramSocket(UDPserverPort)) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    int len;
                    int count = 0;
                    UDPserverSocket.receive(receivePacket);

                    while (receivePacket.getData()[count] != 0) {
                        count++;
                    }
                    byte[] temp = new byte[count];
                    for (int i = 0; i < count; i++) {
                        temp[i] = receivePacket.getData()[i];
                    }
                    String fileInfo = new String(temp);

                    System.out.println(fileInfo);
                    File file = new File(fileInfo);
                    if (!file.exists())
                        file.createNewFile();

                    FileOutputStream output = new FileOutputStream(fileInfo);
                    do {
                        UDPserverSocket.receive(receivePacket);
                        len = receivePacket.getLength();
                        output.write(receiveData, 0, receiveData.length);
                        output.flush();
                    } while (len > 0);
                    System.out.println(
                            "A file " + fileInfo.split("_")[1] + " has been received from " + fileInfo.split("_")[0]);
                    System.out.println("Enter one of the following Commands (EDG, UED, SCS, DTE, AED, OUT, UVF):");
                    System.out.print(">");

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 3) {
            System.out.println("===== Error usage: java Client server_IP server_port client_udp_server_port =====");
            return;
        }
        System.out.println("===== Client is running =====");

        serverHost = args[0];// serverHost = "127.0.0.1";
        serverPort = Integer.parseInt(args[1]);
        UDPserverPort = Integer.parseInt(args[2]);
        exit = false;

        UDPServer udpServer = new UDPServer();
        udpServer.start();
        // define socket for client
        Socket clientSocket = new Socket(serverHost, serverPort);

        // define DataInputStream instance which would be used to receive response from
        // the server
        // define DataOutputStream instance which would be used to send message to the
        // server
        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        while (true) {
            // define a BufferedReader to get input from command line i.e., standard input
            // from keyboard

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // read input from command line
            String message;
            // receive the server response from dataInputStream
            String responseMessage = (String) dataInputStream.readUTF();
            System.out.println(responseMessage.split("-")[0]);
            int count = 0;

            int loginAttempts = Integer.parseInt(responseMessage.split("-")[1]);
            if (responseMessage.contains("You need to provide credentials")) {

                System.out.print("UserName: ");
                String Name = reader.readLine();
                System.out.print("Password: ");
                String Password = reader.readLine();
                while (Name.contains(" ") || Password.contains(" ") || Password.equals("") || Name.equals("")) {
                    System.out.println("Invalid username or password!");
                    System.out.print("UserName: ");
                    Name = reader.readLine();
                    System.out.print("Password: ");
                    Password = reader.readLine();

                }
                dataOutputStream.writeUTF(Name + " " + Password);

                dataOutputStream.flush();
                responseMessage = (String) dataInputStream.readUTF();
            }
            if (responseMessage.equals("Block")) {
                System.out.println("This account has been blocked. Please try it later!");
                clientSocket.close();
                dataOutputStream.close();
                dataInputStream.close();
                exit = true;
                System.exit(0);
                return;

            }
            while (responseMessage.equals("Retry")) {
                count++;

                System.out.print("Wrong password!!!!!!!!!! \n");
                if (count >= loginAttempts) {

                    count = 0;

                    System.out.print("Your account is blocked!!!!!!!!!! \n");
                    dataOutputStream.writeUTF("Block");
                    clientSocket.close();
                    dataOutputStream.close();
                    dataInputStream.close();
                    exit = true;
                    System.exit(0);
                    return;

                }
                System.out.print("Password: ");
                String Password = reader.readLine();
                dataOutputStream.writeUTF(Password);
                responseMessage = (String) dataInputStream.readUTF();
            }
            if (responseMessage.equals("Welcome")) {
                message = UDPserverPort.toString();
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
                System.out.println("Welcome! " + UDPserverPort);
                System.out.println("Enter one of the following Commands (EDG, UED, SCS, DTE, AED, OUT, UVF):");
                System.out.print(">");
                while (true) {
                    message = "";
                    message = reader.readLine();

                    dataOutputStream.writeUTF(message);

                    dataOutputStream.flush();
                    responseMessage = (String) dataInputStream.readUTF();
                    if (responseMessage.equals("Unknown Request!!!!")) {
                        System.out.println(responseMessage);
                    }

                    if (responseMessage.equals("Goodbye")) {
                        System.out.println(responseMessage);
                        clientSocket.close();
                        dataOutputStream.close();
                        dataInputStream.close();
                        exit = true;
                        System.exit(0);
                        return;

                    }
                    if (responseMessage.split(" ")[0].equals("EDGing")) {
                        EDGfunction(responseMessage);

                    }
                    if (responseMessage.equals("EDGerror")) {
                        System.out
                                .println("EDG command requires fileID and dataAmount as arguments. Please try again!");

                    }
                    if (responseMessage.split(" ")[0].equals("AEDing")) {
                        responseMessage = (String) dataInputStream.readUTF();
                        if (responseMessage.equals("")) {
                            System.out.println("No other active edge devices");
                        } else {

                            System.out.println(responseMessage);
                        }
                    }
                    if (responseMessage.split(" ")[0].equals("DTEing")) {
                        String fileID = responseMessage.split(" ")[1];
                        responseMessage = (String) dataInputStream.readUTF();
                        if (responseMessage.equals("NotExist")) {
                            System.out.println("The file does not exist at the server side");
                        } else if (responseMessage.equals("OK")) {
                            System.out.println("The file with ID of " + fileID
                                    + " has been successfully removed from the central server!");

                        } else {
                            System.out.println("Unknown error!");
                        }

                    }

                    if (responseMessage.split(" ")[0].equals("UVFing")) {
                        int destPort = Integer.parseInt(responseMessage.split(" ")[2]);
                        String destIP = responseMessage.split(" ")[1];
                        String fileName = message.split(" ")[2];
                        String account = responseMessage.split(" ")[3];

                        InetAddress IPAddress = InetAddress.getByName(destIP);
                        DatagramSocket udpSocket = new DatagramSocket();
                        File file = new File(fileName);

                        if (file.exists()) {
                            byte[] sendData = new byte[1024];
                            FileInputStream fis = new FileInputStream(file);
                            sendData = (account + "_" + fileName).getBytes();
                            DatagramPacket pack = new DatagramPacket(sendData, sendData.length, IPAddress, destPort);
                            udpSocket.send(pack);
                            byte[] data = new byte[1024];
                            int len;
                            do {
                                len = fis.read(data);
                                pack = new DatagramPacket(data, data.length, IPAddress, destPort);
                                udpSocket.send(pack);
                                TimeUnit.MICROSECONDS.sleep(1);
                            } while (len != -1);
                            byte[] a = new byte[0];
                            pack = new DatagramPacket(a, a.length, IPAddress, destPort);
                            udpSocket.send(pack);
                            udpSocket.close();

                            System.out.println(fileName + " has been uploaded ");
                        } else {
                            System.out.println("The file does not exist!");
                        }

                    }
                    if (responseMessage.equals("UVFerror")) {
                        String reciver = message.split(" ")[1];
                        System.out.println(reciver + " is not offline!");
                    }
                    if (responseMessage.equals("DTEerror")) {
                        System.out.println("DTE command requires fileID as the argument.Please try again!");
                    }
                    if (responseMessage.split(" ")[0].equals("UEDing")) {

                        String response[] = responseMessage.split(" ");
                        String account = response[1];
                        String fileID = response[2];

                        File file = new File(account + "-" + fileID + ".txt");

                        if (!file.exists()) {
                            System.out.println("The file to be uploaded does not exist");
                            dataOutputStream.writeUTF("NotOK");
                            dataOutputStream.flush();
                        } else {

                            dataOutputStream.writeUTF("OK");
                            dataOutputStream.flush();

                            OutputStream out = clientSocket.getOutputStream();

                            FileInputStream fin = new FileInputStream(new File(account + "-" + fileID + ".txt"));

                            byte[] buffer = new byte[1024];
                            int len;

                            while ((len = fin.read(buffer)) != -1) {
                                out.write(buffer, 0, len);

                            }
                            responseMessage = (String) dataInputStream.readUTF();
                            if (responseMessage.equals("OK"))
                                System.out.println("The file has been uploaded successfully!");

                        }

                    }
                    if (responseMessage.equals("UEDerror")) {
                        System.out.println("UED command requires fileID as the argument.Please try again!");
                    }

                    if (responseMessage.split(" ")[0].equals("SCSing")) {
                        String fileID = message.split(" ")[1];
                        String account = responseMessage.split(" ")[1];
                        int num = 0;
                        String response = "";
                        while (readfile(account + "-" + fileID + ".txt")[num] != null) {
                            response = readfile(account + "-" + fileID + ".txt")[num] + " " + response;
                            num++;

                        }
                        dataOutputStream.writeUTF(response);

                        dataOutputStream.flush();

                        responseMessage = (String) dataInputStream.readUTF();

                        System.out.println(responseMessage);
                    }
                    if (responseMessage.equals("SCSerror")) {
                        System.out.println("SCS command requires fileID as the argument.Please try again: ");
                    }

                    System.out.println("Enter one of the following Commands (EDG, UED, SCS, DTE, AED, OUT, UVF):");
                    System.out.print(">");

                }
            }

        }

    }

    public static String[] readfile(String filePath) throws IOException {
        int count = 0;
        String[] accountList = new String[999];

        FileInputStream fin = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(fin);
        BufferedReader buffReader = new BufferedReader(reader);
        String strTmp = "";
        while ((strTmp = buffReader.readLine()) != null) {
            accountList[count] = strTmp;

            count++;

        }
        buffReader.close();
        return accountList;
    }

    private static void EDGfunction(String responseMessage) throws IOException, FileNotFoundException {
        String response[] = responseMessage.split(" ");
        String account = response[1];
        String fileID = response[2];
        int dataAmount = Integer.parseInt(response[3]);
        String filePath = account + "-" + fileID + ".txt";

        File file = new File(filePath);
        String content = "";
        if (!file.exists()) {
            file.createNewFile();
        }
        Random rd = new Random();
        FileOutputStream fos = new FileOutputStream(filePath);
        while (dataAmount > 0) {
            int temp = rd.nextInt(10);
            content = String.valueOf(temp) + "\r\n";
            fos.write(content.getBytes());
            dataAmount--;

        }
        System.out.println("Data generation done");
        fos.close();
    }

}
