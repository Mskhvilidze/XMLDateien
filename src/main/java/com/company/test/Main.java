package com.company.test;

import java.io.*;

public class Main {


    public static void main(String[] args) throws Exception {
        XMLReadWriter xmlReadWriter = new XMLReadWriter();
        File file2 = new File(getReplaceWhitespaces(args[1]));
        File file = new File(getReplaceWhitespaces(args[0]));
        if (!file2.exists()) {
            throw new IllegalArgumentException("Folder existiert nicht!");
        } else {
            File[] files = file2.listFiles();
            if (files != null) {
                if (files.length > 50000) {
                    xmlReadWriter.createForXMLFile(files, file.getPath());
                } else if (files.length < 50000 && files.length > 25000) {
                    xmlReadWriter.createTwoXMLFile(files, file.getPath());
                } else {
                    xmlReadWriter.createOneXMLFile(files, file.getPath());
                }
            } else {
                throw new IllegalArgumentException("Array ist leer");
            }
        }

        for (Thread thread : xmlReadWriter.getList()) {
            thread.join();
        }

        System.out.println("AAA");
    }

    private static String getReplaceWhitespaces(String path) {
        String[] basketForPath = path.split("SAP");
        return basketForPath[0].replaceAll("\\s+", "  ") + "SAP" + basketForPath[1];
    }
}