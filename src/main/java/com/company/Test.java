package com.company;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class Test {
    static List<File> materials = new ArrayList<>();
    static List<File> erp_mark = new ArrayList<>();
    static List<File> crossreference = new ArrayList<>();
    static List<File> dokuinfosatz = new ArrayList<>();
    public static void main(String[] args) throws InterruptedException {
      File file = new File("C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\xml");
      File[] files = file.listFiles();
      System.out.println("File length: " + files.length);

        System.out.println("START: ");
        final long timeStart = System.currentTimeMillis();
        //seq(files);
        parallel(files);
        final long timeEnd = System.currentTimeMillis();
        System.out.println("Verlaufszeit der Schleife: " + (timeEnd - timeStart) + " Millisek.");
        System.out.println("Materials: " + materials.size());
        System.out.println("Crossreference: " + crossreference.size());
        System.out.println("Erp_Mark: " + erp_mark.size());
        System.out.println("Dokuinfosatz: " + dokuinfosatz.size());
        System.out.println("Size: " + (materials.size() + crossreference.size() + erp_mark.size() + dokuinfosatz.size()));
    }

    private static void seq(File[] files) {
        //Heruntergeladene Dateienw werden in verschiedene Containers aufgeteilt
        for (File f : files) {
            if (f.getName().startsWith("Material")) {
                materials.add(f);
            } else if (f.getName().startsWith("ERP_MARKE")) {
                erp_mark.add(f);
            } else if (f.getName().startsWith("Crossreference")) {
                crossreference.add(f);
            } else {
                dokuinfosatz.add(f);
            }
        }
    }

    private static void parallel(File[] files) {
        materials = Arrays.asList(files).parallelStream().filter(f -> f.getName().startsWith("Material"))
        .collect(Collectors.toList());
        erp_mark = Arrays.asList(files).parallelStream().filter(f -> f.getName().startsWith("ERP_MARKE"))
        .collect(Collectors.toList());
        crossreference = Arrays.asList(files).parallelStream().filter(f -> f.getName().startsWith("Crossreference"))
        .collect(Collectors.toList());
    }
}
