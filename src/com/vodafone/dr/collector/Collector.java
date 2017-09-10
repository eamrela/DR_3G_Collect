/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vodafone.dr.collector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jscape.inet.ssh.Ssh;
import com.jscape.inet.ssh.SshConnectedEvent;
import com.jscape.inet.ssh.SshDataReceivedEvent;
import com.jscape.inet.ssh.SshDisconnectedEvent;
import com.jscape.inet.ssh.SshException;
import com.jscape.inet.ssh.SshListener;
import com.jscape.inet.ssh.SshScript;
import com.jscape.inet.ssh.SshScriptListener;
import com.jscape.inet.ssh.SshTask;
import com.jscape.inet.ssh.SshTaskEndEvent;
import com.jscape.inet.ssh.SshTaskFailedEvent;
import com.jscape.inet.ssh.SshTaskStartEvent;
import com.jscape.inet.ssh.SshTaskTimeoutEvent;
import com.jscape.inet.ssh.util.SshParameters;
import com.vodafone.dr.configuration.AppConf;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eamrela
 */
public class Collector implements Runnable, SshListener, SshScriptListener{
    
    private String RNCName;
    private String Destination;
    private PrintWriter errorLog;

    
    private final String OSS_SHELL = ">";
    private final String CONNECT="amos ";
    private final String LT_ALL="lt all";

    
     public void collectPrintout(){
        try {
            String MAIN_COMMAND=  "l+mmmso /home/"+AppConf.getOSS_USER()+"/"+AppConf.getOSS_WORKING_DIR()+"/"+RNCName+".log\n" +
                                        "get ,UtranCell=\n" +
                                        "get ,ExternalUtranCell=\n" +
                                        "get ,Rach=\n" +
                                        "get ,Fach=\n" +
                                        "get ,Pch=\n" +
                                        "get ,Hsdsch=\n" +
                                        "get ,CoverageRelation=\n" +
                                        "get ,ExternalGsmCell=\n" +
                                        "get ,IubLink=\n" +
                                        "get ,ServiceArea=\n" +
                                        "get ,UtranRelation=\n" +
                                        "get ,GsmRelation=\n"+ 
                                        "get ,NodeSynch=\n" +
                                        "get ,IubEdch=1\n" +
                                        "get ,MultiCarrier=\n" +
                                        "get ,Eul=\n" +
                                        "get ,EutranFreqRelation=\n" +
                                        "pr IurLink=\n" +
                                        "l-";
            Ssh ssh = null;
            SshParameters sshParams = new SshParameters(AppConf.getOSS_IP(),AppConf.getOSS_USER(),AppConf.getOSS_PASS());
            ssh = new Ssh(sshParams);
            ssh.setEcho(true);
            ssh.addSshListener(this);
            ssh.setTimeout(5400000);
            ssh.setReadTimeout(5400000);
            SshScript script = new SshScript(ssh);
            script.addSshScriptListener(this);
            
            SshTask TunellingUser = new SshTask("Tunneling1",OSS_SHELL,"ssh "+AppConf.getOSS_UAS_USER()+"@"+AppConf.getOSS_UAS_IP(),":");
            script.addTask(TunellingUser);
            
            SshTask TunellingPass = new SshTask("Tunneling2",":",AppConf.getOSS_UAS_PASS(),OSS_SHELL);
            script.addTask(TunellingPass);
            
            SshTask File = new SshTask("File check",OSS_SHELL,"mkdir -p /home/"+AppConf.getOSS_USER()+"/"+AppConf.getOSS_WORKING_DIR(),OSS_SHELL);
            script.addTask(File);
            
            SshTask connectToRNC = new SshTask("connectToRNC",OSS_SHELL,CONNECT+RNCName,OSS_SHELL);
            script.addTask(connectToRNC);
            //lt all
            SshTask LTALL = new SshTask("LTALL",OSS_SHELL,LT_ALL,OSS_SHELL);
            script.addTask(LTALL);
            //MAIN_COMMAND
            SshTask MAIN = new SshTask("MAIN",OSS_SHELL,MAIN_COMMAND,"l-");
            MAIN.setEndPromptTimeout(5400000);
            script.addTask(MAIN);
            SshTask CHECK = new SshTask("L+",OSS_SHELL,"l?",OSS_SHELL);
            CHECK.setEndPromptTimeout(5400000);
            script.addTask(CHECK);
            
            ssh.connect();
            
            while(!CHECK.isComplete()) {
                Thread.sleep(100);
              }
            
        } catch (SshException ex) {
            Logger.getLogger(Collector.class.getName()).log(Level.SEVERE, null, ex);
            errorLog.println("Couldn't connect to "+RNCName);
        } catch (InterruptedException ex) {
            Logger.getLogger(Collector.class.getName()).log(Level.SEVERE, null, ex);
            errorLog.println("Couldn't connect to "+RNCName);
        } 
        
//        return builder.toString();
    }

    public Collector init(String rncName,String destination,PrintWriter error){
        this.RNCName = rncName;
        this.Destination = destination;
        this.errorLog = error;
        return this;
    }
    
    @Override
    public void run() {   
            collectPrintout();       
    }
    
    @Override
    public void connected(SshConnectedEvent sce) {
        System.out.println("Connected to "+RNCName);
    }

    @Override
    public void disconnected(SshDisconnectedEvent sde) {
        
        System.out.println("Disconnected from "+RNCName +"  "+sde.toString());
    }

    @Override
    public void dataReceived(SshDataReceivedEvent sdre) {
//            System.out.print(sdre.getData());
            
    }
    
    public void WriteTheFile(String FileContent,String Destination,boolean Append){
        
      File file = new File(Destination);

		boolean b = false;


		if (!file.exists()) {
            try {
                
                b = file.createNewFile();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, Append)));
                
                
                out.println(FileContent);
                out.flush();
                  
                
                
                out.close();
            
            } catch (IOException ex) {
                ex.printStackTrace();
            }
		}else{
            PrintWriter out = null;
            try {
                out = new PrintWriter(new BufferedWriter(new FileWriter(file, Append)));
               
                
                out.println(FileContent);
                
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                out.close();
            }
                    
                } 
    }

    @Override
    public void taskStart(SshTaskStartEvent stse) {
        System.out.println(stse.getTask().getName()+" Started "+new Date()+" "+RNCName);
    }

    @Override
    public void taskEnd(SshTaskEndEvent stee) {
        System.out.println(stee.getTask().getName()+" Ended "+new Date()+" "+RNCName);
    }

    @Override
    public void taskFailed(SshTaskFailedEvent stfe) {
        System.out.println(stfe.getTask().getName()+" Failed "+new Date()+" "+RNCName);
    }

    @Override
    public void taskTimeout(SshTaskTimeoutEvent stte) {
        System.out.println(stte.getTask().getName()+" Timed out "+new Date()+" "+RNCName);
    }

    
}
