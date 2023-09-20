package com.company;

import com.jcraft.jsch.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SFTP {
    //-Xmx12288m
    private static final String remoteHost = "10.2.60.253";
    private static final String username = "sftp_transfer";
    private static final String password = "d5N5/fJap244";
    private static final String DEST = "\\\\Dezan1vstiddb1\\sap\\"; //"/export/transfer/Qas3/STEP_SAP/Items/test/";
    private static final String SOURCE1 = "/export/transfer/Qas3/STEP_SAP/Items/test/";
    private static final String SOURCE = "/export/transfer/Qas3/SAP_STEP/Items/";

    public static void main(String[] args) throws Exception {
        Session session = getConnection();
        session.connect();
        System.out.println("Established Session");
        //jeden Tag wird ein Verzeichnis erstellt, in dem aktuelle Dateien heruntergeladen werden
        File filePath = new File(args[0].trim() + getDate().trim() + "\\");
        if (!filePath.exists()) {
            filePath.mkdir();
        }
        //Hier werden dokumentierte Materials  geleert
        deleteTXT(args[0].trim() + "\\output");
        System.out.println("PATH " + filePath.getPath());

        //Files werden aus dem SFTP Server heruntergeladen
        downloadFiles(session, filePath);
        System.out.println("Herunterladen beendet");
        //in einer TXT wird tägliche Prozessausführung dokumentiert
        XMLReadWriter111 xmlReadWriter111 =
                new XMLReadWriter111(new FileWriter(ckeckDir(args[0].trim() + "output") + "\\output.txt"));
        //Nur Files werden berücksichtigt
        File[] files = filePath.listFiles(pathname -> {
            String name = pathname.getName().toLowerCase();
            return name.endsWith(".xml") && pathname.isFile();
        });
        List<File> materials = new ArrayList<>();
        List<File> erp_mark = new ArrayList<>();
        List<File> crossreference = new ArrayList<>();
        List<File> dokuinfosatz = new ArrayList<>();
        System.out.println("START: ");
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
        System.out.println("Aufteilen der Dateien in verschiedenen Containern beenden!");
        xmlReadWriter111.createForXMLFile(materials, erp_mark, crossreference, dokuinfosatz,
                DEST, session);

        System.out.println("Beenden!");
        session.disconnect();
    }

    private static Session getConnection() {
        JSch jsch = null;
        Session session = null;
        try {
            jsch = new JSch();
            session = jsch.getSession(username, remoteHost, 22);
            String knownHostPublicKey =
                    "10.2.60.253 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC3ylQz98ZHtlbf+3EZk5AdRNnrB7hxZgnIu+eOEmOdc6NvhKLC57WuCoHL1zy4HFOkiSlc6CHue7ir/eISryFbtHgg6HMv71auH84jLHV9A7XHBOm8cfYzggTdNaIQM44PdjuNjfN2It3i5DdfjEiqpTWNIjnI+cptdAb5zUYg0qibTVkraXdVVtiy/5Zc+Qh/3bI3/vpgTT7NzNEyYmRcHqX0YpFYSmmdO/tfm6DgiQmogHtHVHTBrw9k/yEB30oxT6bkrJeYY+u6NvCWi0NPlXudQhrHOitGSJLaGqhO56dfeQRtCTlPa4oYDCLnI7MjLpXes4ke7myKofolWMHT";
            jsch.setKnownHosts(new ByteArrayInputStream(knownHostPublicKey.getBytes()));
            session.setPassword(password);
            System.out.println("Connecting------");
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return session;
    }

    public static void downloadFiles(Session session, File filePath) {
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(SOURCE);
            Thread[] threads = new Thread[6];
            int m = entries.size() / threads.length;
            for (int i = 0; i < threads.length; i++) {
                int from = m * i;
                int to = (i + 1 == threads.length) ? entries.size() : from + m;
                threads[i] = new Thread(() -> {
                    ChannelSftp sftpThread = null;
                    try {
                        Channel channelThread = session.openChannel("sftp");
                        channelThread.connect();
                        sftpThread = (ChannelSftp) channelThread;
                        for (int k = from; k < to; k++) {
                            ChannelSftp.LsEntry entry = entries.get(k);
                            if (!entry.getAttrs().isDir()) {
                                sftpThread.get(SOURCE + entry.getFilename(),
                                        filePath.getPath().trim());
                                sftpThread.rm(SOURCE + entry.getFilename());
                            }
                        }
                    } catch (JSchException | SftpException e) {
                        e.printStackTrace();
                    } finally {
                        if (sftpThread != null) {
                            sftpThread.disconnect();
                        }
                        if (sftp != null) {
                            sftp.disconnect();
                        }
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
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
    }
    private static String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy_HHmmss ");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    private static String ckeckDir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdir();
        }
        return file.getPath();
    }

    private static void deleteTXT(String path) {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length > 0) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }
}
