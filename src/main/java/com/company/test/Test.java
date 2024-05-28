package com.company.test;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Test {
    private static final String DIR = "U:\\_Global\\Seeburger_Queries\\_SAP_XML_FILES_JAVA\\sap\\Stibo\\11042024_131205";

    public static void main(String[] args) {
      //  List<File> ordners = getOrdners();
        System.out.println("A");
        removeAllFilesLocal(DIR, "");
       /* for (File ordner : ordners) {
            removeAllFilesLocal(DIR + "\\" + ordner.getName(), ordner.getName());
            deleteDirectory(new File(DIR + "\\" + ordner.getName()));
        }*/
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            // Verzeichnis löschen
            directory.delete();
        }
    }

    private static void removeAllFilesLocal(String dirPath, String name) {
        File file = new File(dirPath);
        System.out.println("Loading...");
        File[] files = file.listFiles();
        System.out.println("Let's go! ");
        Thread[] threads = new Thread[6];
        int m = files.length / threads.length;

        for (int i = 0; i < threads.length; ++i) {
            int from = m * i;
            int to = i + 1 == threads.length ? files.length : from + m;
            threads[i] = new Thread(() -> {
                for (int k = from; k < to; ++k) {
                    System.out.println(Thread.currentThread().getName() + " : " + k + " : " + files[k].getName());
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

    public static List<File> getOrdners() {
        File directory = new File(DIR);
        File[] directories = directory.listFiles();
        List<File> ordners = new ArrayList<>();
        if (directories.length > 0) {
            for (File f : directories) {
                if (f.isDirectory() && !f.getName().equals("output") &&
                        !f.getName().split("_")[0].trim().equals(getDate().trim())) {
                    ordners.add(f);
                }
            }
        }
        return ordners;
    }

    private static String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
}
