import java.net.*;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.io.*;

public class Server {

    // Server information
    private static ServerSocket serverSocket;
    private static Integer serverPort;
    private static Integer loginAttempts;

    private static List<String> blockList = new LinkedList<>();
    private static List<String> AEDlist = new LinkedList<>();

    // define ClientThread for handling multi-threading issue
    // ClientThread needs to extend Thread and override run() method
    private static class ClientThread extends Thread {
        private final Socket clientSocket;
        private boolean clientAlive = false;

        ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            super.run();
            // get client Internet Address and port number
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();
            String clientID = clientAddress + "; " + clientPort;
            String status = "login";
            String account = "";

            System.out.println("===== New connection created for user - " + clientID);
            clientAlive = true;

            // define the dataInputStream to get message (input) from client
            // DataInputStream - used to acquire input from client
            // DataOutputStream - used to send data to client
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            try {
                dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (clientAlive) {
                try {

                    // get input from client
                    // socket like a door/pipe which connects client and server together
                    // data from client would be read from clientSocket
                    assert dataInputStream != null;
                    assert dataOutputStream != null;
                    String message;
                    if (account.equals("")) {
                        message = null;
                    } else {
                        message = (String) dataInputStream.readUTF();
                    }

                    if (status.equals("login")) {

                        String responseMessage = "You need to provide credentials" + "-" + loginAttempts;

                        dataOutputStream.writeUTF(responseMessage);
                        dataOutputStream.flush();

                        message = (String) dataInputStream.readUTF();

                        account = message.split(" ")[0];

                        String password = message.split(" ")[1];

                        for (String str : blockList) {
                            if (str.split(" ")[0].equals(account)) {
                                Calendar cal = Calendar.getInstance();
                                long currentTime = cal.getTimeInMillis();
                                if (currentTime - Long.parseLong(str.split(" ")[1]) > 1000 * 10) {
                                    blockList.remove(str);
                                    break;
                                } else {
                                    responseMessage = "Block";
                                    dataOutputStream.writeUTF(responseMessage);
                                    dataOutputStream.flush();
                                    System.out.println("Your account: " + account
                                            + "is blocked due to multiple authentication failures. Please try again later");

                                    break;
                                }

                            }
                        }
                        if (checkAccount(account) == true) {
                            if (checkPassword(account, password) == false) {
                                while (checkPassword(account, password) == false) {
                                    responseMessage = "Retry";
                                    dataOutputStream.writeUTF(responseMessage);

                                    dataOutputStream.flush();
                                    message = (String) dataInputStream.readUTF();
                                    if (message.equals("Block")) {

                                        Calendar cal = Calendar.getInstance();
                                        long blockTime = cal.getTimeInMillis();
                                        blockList.add(account + " " + blockTime);
                                        System.out.println("Invalid number of allowed failed consecutive attempts: "
                                                + loginAttempts);
                                        break;
                                    }
                                    if (checkPassword(account, message) == true) {
                                        successLogin(clientAddress, dataInputStream, dataOutputStream, account);
                                        status = "command";
                                        break;
                                    }
                                }
                            } else {
                                successLogin(clientAddress, dataInputStream, dataOutputStream, account);
                                status = "command";

                            }

                        } else {
                            register(message);
                            successLogin(clientAddress, dataInputStream, dataOutputStream, account);
                            status = "command";

                        }

                        // make corresponding response i.e., require user to provide username and
                        // password for further authentication
                        // dataOutputStream would be used to send the data to client side

                        // finally call flush() which would flush all data to client side

                    } else if (status.equals("command") && message.split(" ").length == 3
                            && message.split(" ")[0].equals("EDG")) {
                        if (isInteger(message.split(" ")[1]) && isInteger(message.split(" ")[2])) {
                            String fileID = message.split(" ")[1];
                            System.out.println(">Edge device");
                            System.out.println(account + " issued EDG command");
                            int dataAmount = Integer.parseInt(message.split(" ")[2]);

                            dataOutputStream.writeUTF("EDGing " + account + " " + fileID + " " + dataAmount);
                            dataOutputStream.flush();
                        } else {
                            String responseMessage = "EDGerror";
                            dataOutputStream.writeUTF(responseMessage);
                            dataOutputStream.flush();

                        }

                    } else if (status.equals("command") && message.split(" ").length == 3
                            && message.split(" ")[0].equals("UVF")) {
                        System.out.println(">Edge device");
                        System.out.println(account + " issued UVF command");
                        String reciver = message.split(" ")[1];
                        String filename = message.split(" ")[2];
                        String activeList[] = readfile("edge-device-log.txt");
                        int count = 0;
                        String destIP = "";
                        String destPort = "";
                        Boolean exist = false;
                        while (activeList[count] != null && !activeList.equals("")) {
                            if (activeList[count].split("; ")[2].equals(reciver)) {
                                exist = true;
                                destIP = activeList[count].split("; ")[3];
                                destPort = activeList[count].split("; ")[4];
                                break;
                            }
                            count++;
                        }

                        if (!destIP.equals("") && !destPort.equals("") && exist) {
                            dataOutputStream.writeUTF("UVFing " + destIP + " " + destPort + " " + account);
                            dataOutputStream.flush();
                        } else {
                            dataOutputStream.writeUTF("UVFerror");
                            dataOutputStream.flush();
                        }

                    } else if (status.equals("command") && message.split(" ").length == 1
                            && message.split(" ")[0].equals("OUT")) {
                        System.out.println(">Edge device");
                        System.out.println(account + " issued OUT command");

                        System.out.println("Disconected");
                        Logout(account);

                        dataOutputStream.writeUTF("Goodbye");
                        dataOutputStream.flush();
                        clientSocket.close();
                        dataOutputStream.close();
                        dataInputStream.close();
                        break;

                    } else if (status.equals("command") && message.split(" ").length == 2
                            && message.split(" ")[0].equals("UED")) {

                        if (isInteger(message.split(" ")[1])) {
                            System.out.println(">Edge device");
                            System.out.println(account + " issued UED command");
                            String fileID = message.split(" ")[1];
                            dataOutputStream.writeUTF("UEDing " + account + " " + fileID);
                            dataOutputStream.flush();
                            message = (String) dataInputStream.readUTF();
                            if (message.equals("OK")) {

                                InputStream in = clientSocket.getInputStream();
                                OutputStream out = clientSocket.getOutputStream();
                                FileOutputStream fos = new FileOutputStream(new File(account + "-" + fileID + ".txt"));

                                byte[] buffer = new byte[1024];
                                int len;
                                System.out.print(in.available());
                                while ((len = in.read(buffer)) != -1) {
                                    System.out.print(in.available());
                                    fos.write(buffer, 0, len);
                                    if (in.available() == 0) {
                                        break;
                                    }

                                }

                                /*
                                 * BufferedReader bufferedReader = new BufferedReader(
                                 * new InputStreamReader(clientSocket.getInputStream()));
                                 * BufferedWriter bufferedWriter = new BufferedWriter(
                                 * new FileWriter(account + "-" + fileID + ".txt"));
                                 * String line = null;
                                 * while ((line = bufferedReader.readLine())!=null) {
                                 * bufferedWriter.write(line);
                                 * bufferedWriter.newLine();
                                 * bufferedWriter.flush();
                                 * System.out.println(line);
                                 * }
                                 */

                                dataOutputStream.writeUTF("OK");
                                dataOutputStream.flush();
                                UED_DTE_Log(account, fileID, "upload-log.txt");
                                System.out.println("File uploaded ");

                            } else {
                                System.out.println("Not OK");
                            }

                        } else {
                            String responseMessage = "UEDerror";
                            dataOutputStream.writeUTF(responseMessage);
                            dataOutputStream.flush();
                        }

                    }

                    else if (status.equals("command") && message.split(" ").length == 3
                            && message.split(" ")[0].equals("SCS")) {
                        if (isInteger(message.split(" ")[1])) {
                            System.out.println(">Edge device");
                            System.out.println(account + " issued SCS command");
                            dataOutputStream.writeUTF("SCSing " + account);
                            dataOutputStream.flush();
                            String operation = message.split(" ")[2];

                            message = (String) dataInputStream.readUTF();
                            String num[] = message.split(" ");
                            if (num[0].equals("")) {
                                dataOutputStream.writeUTF("NO");
                                dataOutputStream.flush();
                            } else {
                                if (operation.equals("AVERAGE")) {

                                    float Aver = 0;
                                    float sum = 0;

                                    for (int i = 0; i < num.length; i++) {

                                        sum = sum + Integer.parseInt(num[i]);
                                        Aver = sum / (i + 1);
                                    }
                                    dataOutputStream.writeUTF("The result is: " + String.valueOf(Aver));
                                    dataOutputStream.flush();
                                } else if (operation.equals("SUM")) {

                                    int sum = 0;

                                    for (int i = 0; i < num.length; i++) {

                                        sum = sum + Integer.parseInt(num[i]);

                                    }
                                    dataOutputStream.writeUTF("The result is: " + String.valueOf(sum));
                                    dataOutputStream.flush();
                                }

                                else if (operation.equals("MAX")) {
                                    int max = Integer.parseInt(num[0]);

                                    for (int i = 0; i < num.length; i++) {
                                        if (Integer.parseInt(num[i]) > max) {
                                            max = Integer.parseInt(num[i]);
                                        }

                                    }
                                    dataOutputStream.writeUTF("The result is: " + String.valueOf(max));
                                    dataOutputStream.flush();
                                } else if (operation.equals("MIN")) {
                                    int min = Integer.parseInt(num[0]);

                                    for (int i = 0; i < num.length; i++) {
                                        if (Integer.parseInt(num[i]) < min) {
                                            min = Integer.parseInt(num[i]);
                                        }

                                    }
                                    dataOutputStream.writeUTF("The result is: " + String.valueOf(min));
                                    dataOutputStream.flush();
                                } else {
                                    String responseMessage = "Unknow request!!!";
                                    dataOutputStream.writeUTF(responseMessage);
                                    dataOutputStream.flush();
                                }
                            }

                        } else {
                            String responseMessage = "SCSerror";
                            dataOutputStream.writeUTF(responseMessage);
                            dataOutputStream.flush();
                        }
                    }

                    else if (status.equals("command") && message.split(" ").length == 2
                            && message.split(" ")[0].equals("DTE")) {
                        if (isInteger(message.split(" ")[1])) {
                            System.out.println(">Edge device");
                            System.out.println(account + " issued DTE command");
                            String fileID = message.split(" ")[1];
                            dataOutputStream.writeUTF("DTEing " + fileID);
                            dataOutputStream.flush();
                            File file = new File(account + "-" + fileID + ".txt");
                            if (!file.exists()) {
                                dataOutputStream.writeUTF("NotExist");
                                dataOutputStream.flush();
                            } else {
                                UED_DTE_Log(account, fileID, "deletion-log.txt");
                                file.delete();
                                dataOutputStream.writeUTF("OK");
                                dataOutputStream.flush();
                            }

                        } else {
                            dataOutputStream.writeUTF("DTEerror");
                            dataOutputStream.flush();

                        }

                    }

                    else if (status.equals("command") && message.equals("AED")) {
                        System.out.println(">Edge device");
                        System.out.println(account + " issued AED command");
                        File file = new File("edge-device-log.txt");
                        String accountList[] = readfile("edge-device-log.txt");
                        dataOutputStream.writeUTF("AEDing ");
                        dataOutputStream.flush();
                        int count = 0;
                        String response = "";
                        while (accountList[count] != null) {
                            if (!accountList[count].split("; ")[2].equals(account)) {
                                response = response + accountList[count].split("; ")[2] + "; "
                                        + accountList[count].split("; ")[3]
                                        + "; " + accountList[count].split("; ")[4] + "; " + "active since "
                                        + accountList[count].split("; ")[1] + ".\n";
                            }
                            count++;
                        }
                        dataOutputStream.writeUTF(response);
                        dataOutputStream.flush();

                    } else {
                        System.out.println(">Edge device");
                        System.out.println(account + " issued Unknown request");
                        String responseMessage = "Unknown Request!!!!";

                        dataOutputStream.writeUTF(responseMessage);
                        dataOutputStream.flush();
                    }
                } catch (EOFException e) {
                    try {
                        Logout(account);
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    System.out.println("===== the user disconnected, user - " + clientID);
                    clientAlive = false;

                } catch (IOException e) {
                    try {
                        Logout(account);
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    System.out.println("===== the user disconnected, user - " + clientID);
                    clientAlive = false;
                }
            }
        }

        private void Logout(String account) throws IOException, FileNotFoundException {
            String accountList[] = readfile("edge-device-log.txt");
            int count = 0;
            FileOutputStream fos = new FileOutputStream("edge-device-log.txt");
            String content = "";
            while (accountList[count] != null) {

                if (accountList[count].split("; ")[2].equals(account)) {

                    while (accountList[count + 1] != null && !accountList[count + 1].equals("")) {

                        accountList[count] = ((Integer.parseInt(accountList[count + 1].split("; ")[0]) - 1) + "; ")
                                + accountList[count + 1].split("; ")[1] + "; " + accountList[count + 1].split("; ")[2]
                                + "; " + accountList[count + 1].split("; ")[3] + "; "
                                + accountList[count + 1].split("; ")[4];

                        count++;
                    }
                    if (accountList[count + 1] == null || accountList[count + 1].equals("")) {

                        accountList[count] = null;
                        break;
                    }
                }
                count++;
            }
            for (int i = 0; accountList[i] != null && !accountList[i].equals(""); i++) {
                if (i == 0) {
                    fos.write((accountList[i]).getBytes());
                } else {
                    fos.write(("\r\n" + accountList[i]).getBytes());
                }
            }

            fos.flush();
            fos.close();
            System.out.println(">The file");
            System.out.println("edge-device-log.txt is updated");
        }

        private void successLogin(String clientAddr, DataInputStream dataInputStream, DataOutputStream dataOutputStream,
                String account) throws IOException {
            String status;
            String responseMessage;
            responseMessage = "Welcome";
            dataOutputStream.writeUTF(responseMessage);
            dataOutputStream.flush();
            String UDPServerPort = (String) dataInputStream.readUTF();

            try {
                recordLog(account, clientAddr, UDPServerPort, "edge-device-log.txt");
                System.out.println(">The file");
                System.out.println("edge-device-log.txt is updated");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            status = "command";
        }

    }

    public static String[] readfile(String filePath) throws IOException {
        int count = 0;
        File file = new File(filePath);
        String[] accountList = new String[999];
        if (!file.exists()) {
            file.createNewFile();

            return accountList;
        } else {

            FileInputStream fin = new FileInputStream(filePath);
            InputStreamReader reader = new InputStreamReader(fin);
            BufferedReader buffReader = new BufferedReader(reader);
            String strTmp = "";
            while ((strTmp = buffReader.readLine()) != null) {
                accountList[count] = strTmp;

                count++;

            }
            buffReader.close();
        }
        return accountList;

    }

    public static boolean checkAccount(String account) throws IOException {
        boolean exist = false;
        String[] accountList = readfile("credentials.txt");
        for (String str : accountList) {

            if (str != null) {
                if (str.split(" ")[0].equals(account)) {
                    exist = true;
                    break;
                }
            } else {
                break;
            }

        }
        return exist;
    }

    public static boolean checkPassword(String account, String password) throws IOException {
        boolean correctPwd = false;
        for (String str : readfile("credentials.txt")) {
            if (str != null && str.split(" ")[0].equals(account) && str.split(" ")[1].equals(password)) {
                correctPwd = true;
            }
        }
        return correctPwd;
    }

    public static void register(String info) throws IOException {

        String filePath = "credentials.txt";
        FileOutputStream fos = new FileOutputStream(filePath, true);
        if (readfile(filePath)[0] == null) {
            String registerinfo = info;
            fos.write(registerinfo.getBytes());
        } else {
            String registerinfo = "\r" + info;
            fos.write(registerinfo.getBytes());
        }

        fos.close();
        System.out.println(">The file");
        System.out.println("credentials.txt is updated");
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void recordLog(String account, String ClientAddr, String UDPServerPort, String filePath)
            throws IOException {

        int count = 0;
        Date date = new Date();
        String first = "\r";
        // String time = String.format("%s %tB %<te, %<tY",
        // "Due date:", date);
        String time = String.format(Locale.US, "%te %<tB %<tY %<tT", date);
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
            first = "";
        } else {
            while (readfile(filePath)[count] != null) {

                count++;
            }
            if (count == 0) {
                first = "";
            }

        }

        FileOutputStream fos = new FileOutputStream(filePath, true);

        String registerinfo = first + (count + 1) + "; " + time + "; " + account + "; " + ClientAddr + "; "
                + UDPServerPort;
        fos.write(registerinfo.getBytes());
        fos.close();
    }

    public static void UED_DTE_Log(String account, String fileID, String filePath) throws IOException {
        int count = 0;
        int dataAmount = 0;
        String first = "\r";
        Date date = new Date();
        // String time = String.format("%s %tB %<te, %<tY",
        // "Due date:", date);
        String time = String.format(Locale.US, "%te %<tB %<tY %<tT", date);
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
            first = "";
        } else {
            while (readfile(filePath)[count] != null) {

                count++;
            }
            if (count == 0)
                first = "";

        }
        while (readfile(account + "-" + fileID + ".txt")[dataAmount] != null) {

            dataAmount++;
        }

        FileOutputStream fos = new FileOutputStream(filePath, true);

        String registerinfo = first + account + "; " + time + "; " + fileID + "; "
                + dataAmount;
        fos.write(registerinfo.getBytes());
        fos.close();
        System.out.println(">The file");
        System.out.println(filePath + " is updated");
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out
                    .println("===== Error usage: java Server server_port number_of_consecutive_failed_attempts =====");
            return;
        }

        // acquire port number from command line parameter
        // serverPort = Integer.parseInt(args[0]);
        serverPort = Integer.parseInt(args[0]);
        loginAttempts = Integer.parseInt(args[1]);

        // define server socket with the input port number, by default the host would be
        // localhost i.e., 127.0.0.1
        serverSocket = new ServerSocket(serverPort);
        // make serverSocket listen connection request from clients
        System.out.println("===== Server is running =====");
        System.out.println("===== Waiting for connection request from clients...=====");

        while (true) {
            // when new connection request reaches the server, then server socket
            // establishes connection
            Socket clientSocket = serverSocket.accept();
            // for each user there would be one thread, all the request/response for that
            // user would be processed in that thread
            // different users will be working in different thread which is multi-threading
            // (i.e., concurrent)
            ClientThread clientThread = new ClientThread(clientSocket);
            clientThread.start();
        }
    }
}
