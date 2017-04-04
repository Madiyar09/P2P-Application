package p2p_server;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import P2P_Client.Encryption;
import P2P_Client.FilePath;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import org.apache.commons.codec.binary.Base64;


class Info {
    public String Name;
    public String Type;
    public String Size;
    public String LastModified;
    public String Ip;
    public String Port;
    
    public Info() {
        
    }
    
    public Info(String name, String type, String size, 
            String lastmodified, String ip, String port) {
        Name = name;
        Type = type;
        Size = size;
        LastModified = lastmodified;
        Ip = ip;
        Port = port;
    }
    
    public String toString() {
        return "<" + Name + "?" + Type + "?" + Size + "?" + LastModified + "?" + Ip + "?" + Port + ">";
    }
}

public class P2P_Server implements Runnable {
    Socket csocket;
    static ArrayList <Info> list;
    File f = new File("C:\\Users\\aibek\\Documents\\NetBeansProjects\\P2P_Server\\Users.txt");
    
    P2P_Server(Socket csocket) {
        this.csocket = csocket;
    }
    
    public static void main(String args[]) throws Exception { 
        //create server
        ServerSocket ssock = new ServerSocket(6789);
        list = new ArrayList<Info>();
        //accept peers
        while (true) {
            Socket csocket = ssock.accept();
            System.out.println("Connected");
            new Thread(new P2P_Server(csocket)).start();
        }
    }
    
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            DataOutputStream output = new DataOutputStream(csocket.getOutputStream());
            DataInputStream dis = new DataInputStream(csocket.getInputStream());
            String Ip = "";
            String Port = "";
            String key = "";
            
            while(true){
                try {
                    //read from peer message
                    String s = input.readLine();
                    if (s.length() >= 5 && s.substring(0, 5).equals("login")){
                        String t = input.readLine();
                        String login = t;
                        String line, tempLog, tempPass;
                        int exist = 0;
                        try {
                                BufferedReader br = new BufferedReader(new FileReader(f));
                                
                                while((line = br.readLine()) != null){
                                        tempLog = line.substring(0, line.indexOf('\t'));
                                        if (login.equals(tempLog)){
                                            exist = 1;
                                            break;
                                        }
                                }
                                if (exist == 1){
                                    output.writeBytes("exists\n");
                                }else{
                                    output.writeBytes("Notexists\n");
                                }
                        }catch(Exception ex){
                            //System.out.println(ex.getMessage());
                        }
                        if (exist == 1){
                            try {
                                    BufferedReader br = new BufferedReader(new FileReader(f));
                                    String strToDecrypt = input.readLine();
                                    //read byte from server 
                                    
                                    while((line = br.readLine()) != null){
                                            tempLog = line.substring(0, line.indexOf('\t'));
                                            line = line.substring(line.indexOf('\t') + 1);
                                            if (login.equals(tempLog)){
                                                //decryption;
                                                //String strToDecrypt = "My text to encrypt";
                                                Encryption enc = new Encryption();
                                                key = line;
                                                String ans = Encryption.decrypt(strToDecrypt, key);
                                                
                                                output.writeBytes(ans + "\n");
                                            }
                                    }
                            }catch(Exception ex){
                                //System.out.println(ex.getMessage());
                            }
                        }
                    }else if (s.equals("Register")){
                        try{
                            
                            s = input.readLine();
                            String log = s;
                            s = input.readLine();
                            String pas = s, line, tempLog;
                            int exist = 0;
                            FileWriter fw = new FileWriter(f, true);
                            BufferedReader br = new BufferedReader(new FileReader(f));
                            BufferedWriter bw = new BufferedWriter(fw);
                            while( (line = br.readLine()) != null){
                                tempLog = line.substring(0, line.indexOf('\t'));
                                if (log.equals(tempLog)){
                                    exist = 1;
                                    break;
                                }
                            }
                            if (exist == 1){
                                output.writeBytes("exists\n");
                            }else{
                                output.writeBytes("good\n");
                                bw.write(log);
                                bw.write("\t");
                                bw.write(pas);
                                bw.write("\n");
                                bw.close();
                                fw.close();
                            }
                        }catch(Exception e){
                            
                        }
                    }
                    //if it is hello say hi
                    else if(s.equals("HELLO")) {
                        output.writeBytes("HI\n");
                    //then get info about all shared file
                    } else if(s.charAt(0) == '<') {
                        Info temp = new Info();
                        
                        temp.Name = s.substring(1, s.indexOf('?'));
                        s = s.substring(s.indexOf('?') + 1);
                        
                        temp.Type = s.substring(0, s.indexOf('?'));
                        s = s.substring(s.indexOf('?') + 1);
                        
                        temp.Size = s.substring(0, s.indexOf('?'));
                        s = s.substring(s.indexOf('?') + 1);
                        
                        temp.LastModified = s.substring(0, s.indexOf('?'));
                        s = s.substring(s.indexOf('?') + 1);
                        
                        temp.Ip = s.substring(0, s.indexOf('?'));
                        Ip = temp.Ip;
                        s = s.substring(s.indexOf('?') + 1);
                        
                        temp.Port = s.substring(0, s.indexOf('>'));
                        Port = temp.Port;
                        
                        list.add(temp);
                    //if peer wants to search, using substring know what exactly
                    } else if(s.length() > 5 && s.substring(0, 6).equals("SEARCH")) {
                        s = s.substring(7);
                        String temp = "";
                        
                        //search files from shared file names
                        for(Info file: list) {
                            if(file.Name.toLowerCase().contains(s.toLowerCase())) {
                                temp += file.toString() + "\n";
                            }
                        }
                        //if found send all the found file informations and say that search ended or say that not found
                        if(temp.length() > 0) {
                            output.writeBytes("FOUND:\n");
                            output.writeBytes(temp);
                            output.writeBytes("END SEARCH\n");
                        } else {
                            output.writeBytes("NOT FOUND\n");
                        }
                        
                        //if peer wants to leave, delete all peer's shared files' information from list
                    } else if(s.equals("BYE")) {
                        for(int i = list.size() - 1; i >= 0; i --) {
                            if(list.get(i).Ip.equals(Ip) && list.get(i).Port.equals(Port)) {
                                list.remove(i);
                            }
                        }
                        break;
                    }
                    
                } catch(Exception e){
                    
                }
            }
        } catch(Exception e){
            
        }
    }
}
