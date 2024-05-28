package com.company;

import com.jcraft.jsch.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMLReadWriter111 {
    private static final String MATERIAL = "Material";
    private static final String ERP_MARKE = "ERP_MARKE";
    private static final String CROSSREFERENCE = "Crossreference";
    private static final String DOKUINFOSATZ = "Dokuinfosatz";
    private final FileWriter writer;
    private final List<File> rest = new ArrayList<>();

    public XMLReadWriter111(FileWriter writer) {
        this.writer = writer;
    }

    /**
     * XPaht wird zum Erzeugen der XPaht-Objekten erstellt. Danach wird XPath-Objekt aus XPathFactory erstellt.
     * XPhat-Ausdruck wird kompiliert.
     *
     * @param expression XPhat-Ausdruck
     * @param files      Files
     * @return Gibt die Document zurück
     */
    public Document merge(String expression,
                          File... files) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression compiledExpression = xpath
                    .compile(expression);
            return merge(compiledExpression, files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Wird neue DocumentBuildFactory erstellt, um das Document geparst zu werden.
     */
    public Document merge(XPathExpression expression,
                          File... files) {
        Document base = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            docBuilderFactory
                    .setIgnoringElementContentWhitespace(true);
            DocumentBuilder docBuilder = docBuilderFactory
                    .newDocumentBuilder();
            String fileContent = Files.readString(Path.of(files[0].getPath()), StandardCharsets.UTF_8);
            if (!fileContent.isEmpty()) {
                base = docBuilder.parse(files[0]);
                Node results = (Node) expression.evaluate(base,
                        XPathConstants.NODE);
                if (results == null) {
                    throw new IOException(files[0]
                            + ": Fehlschlag" + files.length);
                }

                for (int i = 1; i < files.length; i++) {
                    fileContent = Files.readString(Path.of(files[i].getPath()), StandardCharsets.UTF_8);
                    if (!fileContent.isEmpty()) {
                        Document merge = docBuilder.parse(files[i]);
                        Node nextResults = (Node) expression.evaluate(merge,
                                XPathConstants.NODE);
                        while (nextResults.hasChildNodes()) {
                            Node kid = nextResults.getFirstChild();
                            nextResults.removeChild(kid);
                            kid = base.importNode(kid, true);
                            results.appendChild(kid);
                        }
                    }
                }
            }
        } catch (XPathExpressionException | IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return base;
    }

    /**
     * Zeigt Start- und Maxwerten
     */
    private long calculate() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed() / (1024 * 1024);
        long maxMemory = heapUsage.getMax() / (1024 * 1024);
        long percentageUsed = (long) (100 * ((1.0 * usedMemory) / (1.0 * maxMemory)));
        System.out.println("Used vs. Max Memory: " + usedMemory + "M/" + maxMemory + "M " + percentageUsed);
        return percentageUsed;
    }

    /**
     * Document anzeigen lassen
     */
    public void print(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory
                    .newTransformer();
            DOMSource source = new DOMSource(doc);
            Result result = new StreamResult(System.out);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Das erstellte Document wird in neue Datei geschrieben.
     */
    public void write(Document doc, String dest, String tag, String fileName, int size) {
        String uuid = UUID.randomUUID().toString();
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dest + "/" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid + ".xml")
                , StandardCharsets.UTF_8))) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            String xmlOutput = result.getWriter().toString();
            output.write(xmlOutput);
            writeTXT("Datei wurde erstell: " + dest + "" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid +
                    ".xml " + "Size: " + size + "\r\n\n");
            System.out.println("Ready");
        } catch (TransformerException | IOException e) {
            writeTXT("Fehler beim Documentschreiben " + tag + " : " + fileName);
            e.printStackTrace();
        }
    }

    public void uploadToSFTP(Document doc, String dest, String tag, Session session, String fileName) {
        ChannelSftp sftp = null;
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            String uuid = UUID.randomUUID().toString();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                transformer.transform(source, new StreamResult(output));
                byte[] xmlBytes = output.toByteArray();
                ByteArrayInputStream input = new ByteArrayInputStream(xmlBytes);
                sftp.put(input, dest + "/" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid + ".xml");
                System.out.println(
                        "Upload complete: " + dest + "/" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid +
                                ".xml");
            } catch (TransformerException | IOException e) {
                e.printStackTrace();
            }
        } catch (JSchException | SftpException | TransformerConfigurationException e) {
            e.printStackTrace();
        } finally {
            if (sftp != null) {
                sftp.disconnect();
            }
        }
    }

    private String printSimpleDateFormat(String fileName) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "ddMMyyyy_HHmmss ");
        Date currentTime = new Date();
        return fileName.split("_").length > 3 ?
                fileName.split("_")[2] + "_" +
                        fileName.split("_")[3].substring(0, fileName.split("_")[3].length() - 4) :
                formatter.format(currentTime);
    }

    /**
     * create for XML files
     */
    public void createForXMLFile(List<File> materials, List<File> erpMark,
                                 List<File> crossreference, List<File> dokuinfosatz, String dest,
                                 Session session) {
        try {
            writeTXT("createMergeThread started: \r\n\n");
            Thread threadE =
                    createMergeThread(erpMark, Tags.PRODUCTS.value(), dest + "Marke\\in\\", ERP_MARKE, session);
            Thread threadC =
                    createMergeThread(crossreference, Tags.PRODUCTS.value(), dest + "Reference\\in\\", CROSSREFERENCE,
                            session);
            Thread threadD =
                    createMergeThread(dokuinfosatz, Tags.ASSETS.value(), dest + "Doc\\in\\", DOKUINFOSATZ, session);
            threadE.start();
            threadC.start();
            threadD.start();

            threadE.join();
            threadC.join();
            threadD.join();
            erpMark.clear();
            crossreference.clear();
            dokuinfosatz.clear();

            materials.sort(Collections.reverseOrder());
            //Hier werden Materials separat verarbeitet und zusammengeführt, da sie viel mehr sind, als die anderen
            createXMLForMaterials(materials, dest, session);
            materials.clear();
            if (!rest.isEmpty()) {
                writeTXT("GesamtRestSize: " + rest.size());
                Thread threadM =
                        createMergeThread(rest, Tags.PRODUCTS.value(), dest + "Materials\\\\in\\\\", MATERIAL,
                                session);
                threadM.start();
                threadM.join();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            writeTXT("Thread join wurde in 'createForXMLFile' unterbrochen: " + e.getMessage() + "\r\n\n");
            Thread.currentThread().interrupt();
        }
    }

    private void createXMLForMaterials(List<File> materials, String dest, Session session) {
        int numberOfThreads = getNumberOfThreads(materials);
        writeTXT("Anzahl an Thread für Materials: " + numberOfThreads + " Size: " + materials.size() + "\r\n\n");
        Thread[] threads = new Thread[numberOfThreads];
        int m = materials.size() / threads.length;
        for (int i = 0; i < threads.length; i++) {
            int from = m * i;
            int to = (i + 1 == threads.length) ? materials.size() : from + m;
            threads[i] = new Thread(() -> {
                List<File> mergeList = new ArrayList<>();
                for (int k = from; k < to; k++) {
                    mergeList.add(materials.get(k));
                    if (mergeList.size() == 25000 || (k == to - 1 && threads.length == 1)) {
                        performMergeAndWrite(Tags.PRODUCTS.value(), mergeList, dest + "Materials\\in\\", MATERIAL,
                                session);
                        mergeList.clear();
                    }
                    if (!mergeList.isEmpty() && k == to - 1) {
                        writeTXT("Thread: " + Thread.currentThread().getName() + " : hat folgende Restsize: " +
                                mergeList.size());
                        fillRestData(mergeList);
                    }
                }
            });
            threads[i].start();
        }
        joinThreads(threads);
    }

    private void fillRestData(List<File> mergeList) {
        synchronized (rest) {
            rest.addAll(mergeList);
        }
    }

    private int getNumberOfThreads(List<File> materials) {
        int numberOfThreads;
        // Berechnen der Anzahl der Threads basierend auf der Anzahl der Materialien
        if (materials.isEmpty()) {
            numberOfThreads = 1; // Mindestens ein Thread wird immer benötigt
        } else {
            // Berechnen der Anzahl der Threads basierend auf der Anzahl der Materialien
            numberOfThreads = (int) Math.ceil(materials.size() / 25000.0);

            // Begrenzung der maximalen Anzahl von Threads auf 4
            numberOfThreads = Math.min(numberOfThreads, 6);
        }

        return numberOfThreads;
    }

    public Thread createMergeThread(List<File> filesList, String tag, String destination, String name,
                                    Session session) {
        return new Thread(() -> {
            filesList.sort(Collections.reverseOrder());
            List<File> mergeList = new ArrayList<>();
            writeTXT("Thread: " + Thread.currentThread().getName() + "_ " + name + " ist für Rest bereit => Size: " +
                    filesList.size());
            for (File file : filesList) {
                mergeList.add(file);
                if (mergeList.size() == 25000) {
                    writeTXT("Size von " + name + ": " + mergeList.size());
                    performMergeAndWrite(tag, mergeList, destination, name, session);
                    mergeList.clear();
                }
            }
            if (!mergeList.isEmpty()) {
                writeTXT("Rest von " + name + ": " + mergeList.size());
                performMergeAndWrite(tag, mergeList, destination, name, session);
                mergeList.clear();
            }
        });
    }

    private void performMergeAndWrite(String tag, List<File> fileList, String destination, String name,
                                      Session session) {
        System.out.println("Seesion: " + session.isConnected());
        try {
            writeTXT(Thread.currentThread().getName() + " Merge started: " + name + "\r\n\n");
            Document mergedDoc = merge("/STEP-ProductInformation/" + tag, fileList.toArray(File[]::new));
            String fileName = "";
            if (!fileList.isEmpty()) {
                fileName = fileList.get(0).getName();
            }
            write(mergedDoc, destination, name, fileName, fileList.size());
        } catch (Exception e) {
            writeTXT("Fehler beim Mergen: " + name);
            e.printStackTrace();
        }
    }

    private void joinThreads(Thread... threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                writeTXT("Thread join wurde in 'joinThreads' unterbrochen: " + e.getMessage() + "\r\n\n");
                Thread.currentThread().interrupt();
            }
        }
    }

    private void writeTXT(String text) {
        try {
            writer.write(text + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}