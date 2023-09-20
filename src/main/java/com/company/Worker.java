package com.company;

import com.jcraft.jsch.ChannelSftp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class Worker {

    public static void main(String[] args) {

        File filePath = new File("\\\\Dezan1vstiddb1\\sap");
        File[] files = filePath.listFiles();
        for (File f : files) {
            System.out.println(f.getName());
        }
    }
    private static String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy_HHmmss ");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
}
/*
        Runtime rt = Runtime.getRuntime();
        System.out.println(rt.totalMemory() - rt.freeMemory()); 333493 269948
 */