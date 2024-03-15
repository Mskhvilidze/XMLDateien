package com.company;

import com.jcraft.jsch.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SFTP {
    //-Xmx12288m
    private static final String REMOTE_HOST = "10.2.60.253";
    private static final String USERNAME = "sftp_transfer";
    private static final String PASSWORD = "d5N5/fJap244";
    private static final String DEST = "\\\\Dezan1vstiddb1\\sap\\";
    //private static final String SOURCE1 = "/export/transfer/Qas3/STEP_SAP/Items/test/";
    private static final String SOURCE = "/export/transfer/Qas3/SAP_STEP/Items/";
    //private static final String ARCHIVE = "\\\\fileserver\\\\FrickeData\\\\_Global\\\\Seeburger_Queries\\\\_SAP_XML_FILES_JAVA\\\\sap\\\\IN\\\\";

    public static void main(String[] args) {
        try {
            Session session = establishSession();
            System.out.println("Established Session");

            //jeden Tag wird ein Verzeichnis erstellt, in dem aktuelle Dateien heruntergeladen werden
            File filePath = createDirectory(args[0]);
            if (filePath == null) {
                throw new IllegalArgumentException("Directory path cannot be null");
            }
            System.out.println("Created directory: " + filePath.getAbsolutePath());

            if (session == null) {
                throw new IllegalArgumentException("Session cannot be null");
            }

            //Files werden aus dem SFTP Server heruntergeladen
            downloadFiles(session, filePath);
            System.out.println("Herunterladen beendet");

            //in einer TXT wird tägliche Prozessausführung dokumentiert
            XMLReadWriter111 xmlReadWriter111 = createXMLReadWriter(args[0]);
            List<File> materials = getFilesByPrefix(filePath, "Material");
            List<File> erpMarke = getFilesByPrefix(filePath, "ERP_MARKE");
            List<File> crossreference = getFilesByPrefix(filePath, "Crossreference");
            List<File> dokuinfosatz = getFilesByPrefix(filePath, "Dokuinfosatz");

            System.out.println("Aufteilen der Dateien in verschiedenen Containern beenden!");
            xmlReadWriter111.createForXMLFile(materials, erpMarke, crossreference, dokuinfosatz,
                    DEST, session);

            System.out.println("Beenden!");
            session.disconnect();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves files from the specified directory that start with the given prefix.
     *
     * @param directory The directory from which to retrieve files.
     * @param prefix    The prefix to match with file names.
     * @return A list of files matching the specified prefix.
     */
    private static List<File> getFilesByPrefix(File directory, String prefix) {
        List<File> files = new ArrayList<>();
        File[] fileList = directory.listFiles(pathname -> {
            String name = pathname.getName().toLowerCase();
            return name.endsWith(".xml") && pathname.isFile();
        });
        if (fileList != null) {
            for (File file : fileList) {
                if (file.getName().startsWith(prefix)) {
                    files.add(file); // Add files starting with the specified prefix to the list
                }
            }
        }
        return files;
    }

    /**
     * Creates an XMLReadWriter instance for writing process logs.
     *
     * @param arg The base directory path.
     * @return The created XMLReadWriter instance.
     * @throws IOException If an I/O error occurs.
     */
    private static XMLReadWriter111 createXMLReadWriter(String arg) throws IOException {
        return new XMLReadWriter111(new FileWriter(
                checkDir(arg.trim() + "output") + "\\output_" + getDate() + ".txt", true));
    }

    /**
     * Creates a directory at the specified location if it does not exist already.
     *
     * @param arg The directory path.
     * @return The created directory.
     */
    private static File createDirectory(String arg) {
        File filePath = new File(arg.trim() + getDate().trim() + File.separator); // Konstruiere den Verzeichnispfad
        if (!filePath.exists()) {
            boolean created = filePath.mkdir(); // Verzeichnis erstellen, wenn es nicht existiert
            if (!created) {
                System.err.println("Fehler: Verzeichnis konnte nicht erstellt werden.");
                return null; // Rückgabe von null, wenn das Verzeichnis nicht erstellt werden konnte
            }
        }
        return filePath; // Rückgabe des Verzeichnisses
    }


    /**
     * Establishes a session connection using the provided connection details.
     *
     * @return The established session connection.
     * If an error occurs during the session establishment.
     */
    private static Session establishSession() {
        Session session = getConnection();
        try {
            if (session != null) {
                session.connect();
                return session;
            }
        } catch (JSchException e) {
            System.err.println("Error establishing session: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static Session getConnection() {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USERNAME, REMOTE_HOST, 22);
            String knownHostPublicKey =
                    "10.2.60.253 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC3ylQz98ZHtlbf+3EZk5AdRNnrB7hxZgnIu+eOEmOdc6NvhKLC57WuCoHL1zy4HFOkiSlc6CHue7ir/eISryFbtHgg6HMv71auH84jLHV9A7XHBOm8cfYzggTdNaIQM44PdjuNjfN2It3i5DdfjEiqpTWNIjnI+cptdAb5zUYg0qibTVkraXdVVtiy/5Zc+Qh/3bI3/vpgTT7NzNEyYmRcHqX0YpFYSmmdO/tfm6DgiQmogHtHVHTBrw9k/yEB30oxT6bkrJeYY+u6NvCWi0NPlXudQhrHOitGSJLaGqhO56dfeQRtCTlPa4oYDCLnI7MjLpXes4ke7myKofolWMHT";
            jsch.setKnownHosts(new ByteArrayInputStream(knownHostPublicKey.getBytes()));
            session.setPassword(PASSWORD);
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
            List<ChannelSftp.LsEntry> entries = new ArrayList<>(sftp.ls(SOURCE));
            System.out.println("Size: " + entries.size());
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
                        disconnectAndExit(sftpThread, sftp);
                    }
                });
                threads[i].start();
            }
            joinThreads(threads);
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
    }

    private static void disconnectAndExit(ChannelSftp sftpThread, ChannelSftp sftp) {
        if (sftpThread != null) {
            sftpThread.disconnect();
            sftpThread.exit();
        }
        sftp.disconnect();
        sftp.exit();
    }
    private static void joinThreads(Thread[] threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
    public static String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy_HHmmss ");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    private static String checkDir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            boolean created = file.mkdir();
            if (!created) {
                System.out.println("File konnte nicht erstellt werden!");
            }
        }
        return file.getPath();
    }
}
