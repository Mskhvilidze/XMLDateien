package com.company.test;

import com.jcraft.jsch.ChannelSftp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class Worker {

    public static void main(String[] args) {

      /*  File filePath = new File("\\\\Dezan1vstiddb1\\sap");
        File[] files = filePath.listFiles();
        for (File f : files) {
            System.out.println(f.getName());
        }*/
        deleteAllFiles();
    }
    private static String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy_HHmmss ");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    public static void deleteAllFiles() {
        File file = new File("C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\xml\\");
        File[] files = file.listFiles();
        System.out.println(files.length);
        Thread[] threads = new Thread[6];
        int m = files.length / threads.length;
        for (int i = 0; i < threads.length; i++) {
            int from = m * i;
            int to = (i + 1 == threads.length) ? files.length : from + m;
            threads[i] = new Thread(() -> {
                for (int k = from; k < to; k++) {
                    System.out.println(Thread.currentThread().getName() + " Delete: " + files[k].getName());
                    files[k].delete();
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
/*
        Runtime rt = Runtime.getRuntime();
        System.out.println(rt.totalMemory() - rt.freeMemory()); 333493 269948
 */