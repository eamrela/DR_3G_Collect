/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vodafone.dr.main;

import com.vodafone.dr.collector.Collector;
import com.vodafone.dr.configuration.AppConf;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Admin
 */
public class Processor {
    
    private static String workingDir = null;
    private static String printoutsDir = null;
    private static PrintWriter errPW = null;
    
    public static void initApp(String confPath){
        try {
            System.out.println("Initializing App");
            AppConf.configureApp(confPath);
            workingDir = AppConf.getWorkingDir()+"\\DR_3G_"+AppConf.getMydate();
            printoutsDir = workingDir+"\\printouts";
            new File(printoutsDir).mkdirs();
            errPW = new PrintWriter(new File(workingDir+"\\error.log"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static boolean collectPrintout(){
            ExecutorService printoutExecutor = Executors.newCachedThreadPool();
            System.out.println("Submitting Collection Tasks");
            for (Map.Entry<String, Integer> en : AppConf.getNodes().entrySet()) {
                System.out.println("Submitting Task for :"+en.getKey());
                printoutExecutor.submit(new Collector().init(en.getKey(), printoutsDir, errPW));
            }  
            printoutExecutor.shutdown();
            while(!printoutExecutor.isTerminated()){}
            System.out.println("Executer finished");
            return true;
    }

    public static void main(String[] args) {
        
        
        if(args.length!=1){
            System.out.println("Please set the input paramters");
            System.out.println("Configuration File");
            System.exit(1);
        }
        String conf = args[0];
        initApp(conf);
        System.out.println("Running in Collection mode");
        System.out.println("Calling Collector");
        collectPrintout();
        errPW.flush();
        errPW.close();
       
    }
}
