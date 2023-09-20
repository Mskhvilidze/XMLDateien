package com.company;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMLReadWriter111 {
    private final String MATERIAL = "Material";
    private final String ERP_MARKE = "ERP_MARKE";
    private final String CROSSREFERENCE = "Crossreference";
    private final String DOKUINFOSATZ = "Dokuinfosatz";
    private FileWriter writer;

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
     * @throws Exception
     */
    public Document merge(String expression,
                          File... files) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression compiledExpression = xpath
                .compile(expression);
        return merge(compiledExpression, files);
    }

    /**
     * Wird neue DocumentBuildFactory erstellt, um das Document geparst zu werden.
     *
     * @param expression
     * @param files
     * @return
     * @throws Exception
     */
    public Document merge(XPathExpression expression,
                          File... files) {
        Document base = null;
        String fileContent = "";
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            docBuilderFactory
                    .setIgnoringElementContentWhitespace(true);
            DocumentBuilder docBuilder = docBuilderFactory
                    .newDocumentBuilder();
            fileContent = Files.readString(Path.of(files[0].getPath()), StandardCharsets.UTF_8);
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
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return base;
    }

    /**
     * Zeigt Start- und Maxwerten
     *
     * @return
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
     *
     * @param doc
     * @throws Exception
     */
    public void print(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory
                .newTransformer();
        DOMSource source = new DOMSource(doc);
        Result result = new StreamResult(System.out);
        transformer.transform(source, result);
    }

    /**
     * Das erstellte Document wird in neue Datei geschrieben.
     *
     * @param doc
     * @throws TransformerConfigurationException
     * @throws IOException
     */
    public void write(Document doc, String dest, String tag, String fileName) throws TransformerConfigurationException, IOException {
        String uuid = UUID.randomUUID().toString();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new StringWriter());
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dest + "/" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid + ".xml")
                , StandardCharsets.UTF_8))) {
            transformer.transform(source, result);
            String xmlOutput = result.getWriter().toString();
            output.write(xmlOutput);
            output.close();
            System.out.println("Ready");
        } catch (TransformerException | IOException e) {
            writeTXT("Fehler beim Documentschreiben " + tag + " : " + fileName);
            e.printStackTrace();
        }
    }

    public void uploadToSFTP(Document doc, String dest, String tag, Session session, String fileName) throws
            TransformerConfigurationException, IOException {
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
            StreamResult result = new StreamResult(new StringWriter());
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                transformer.transform(source, new StreamResult(output));
                byte[] xmlBytes = output.toByteArray();
                ByteArrayInputStream input = new ByteArrayInputStream(xmlBytes);
                sftp.put(input, dest + "/" + tag + "_" + printSimpleDateFormat(fileName) + "_" + uuid + ".xml");
                System.out.println("Upload complete");
            } catch (TransformerException | IOException e) {
                e.printStackTrace();
            }
        } catch (JSchException | SftpException e) {
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
        // System.out.println(formatter.format(currentTime));        // 2012.04.14 - 21:34:07
        return fileName.split("_").length > 3 ?
                fileName.split("_")[2] + "_" +
                        fileName.split("_")[3].substring(0, fileName.split("_")[3].length() - 4) :
                formatter.format(currentTime);
    }

    /**
     * create for XML files
     *
     * @throws Exception
     */
    public void createForXMLFile(List<File> materials, List<File> erp_mark,
                                 List<File> crossreference, List<File> dokuinfosatz, String dest,
                                 Session session) throws Exception {
        Thread threadE = createMergeThread(erp_mark, Tags.PRODUCTS.value(), dest + "Marke\\in\\", ERP_MARKE, session);
        Thread threadC = createMergeThread(crossreference, Tags.PRODUCTS.value(), dest + "Reference\\in\\", CROSSREFERENCE, session);
        Thread threadD = createMergeThread(dokuinfosatz, Tags.ASSETS.value(), dest + "Doc\\in\\", DOKUINFOSATZ, session);
        threadE.start();
        threadC.start();
        threadD.start();

        threadE.join();
        threadC.join();
        threadD.join();
        erp_mark.clear();
        crossreference.clear();
        dokuinfosatz.clear();

        //Hier werden Materials separat verarbeitet und zusammengeführt, da sie viel mehr sind, als die anderen
        Collections.sort(materials, Collections.reverseOrder());
        Thread[] threads = new Thread[4];
        int m = materials.size() / threads.length;
        for (int i = 0; i < threads.length; i++) {
            int from = m * i;
            int to = (i + 1 == threads.length) ? materials.size() : from + m;
            threads[i] = new Thread(() -> {
                List<File> mergeList = new ArrayList<>();
                for (int k = from; k < to; k++) {
                    mergeList.add(materials.get(k));
                    if (mergeList.size() == 25000) {
                        writeTXT(Thread.currentThread().getName() + " : " + k);
                        performMergeAndWrite(Tags.PRODUCTS.value(), mergeList, dest + "Materials\\in\\", MATERIAL, session);
                        mergeList.clear();
                    }
                }
                if (!mergeList.isEmpty()) {
                    performMergeAndWrite(Tags.PRODUCTS.value(), mergeList, dest + "Materials\\in\\", MATERIAL, session);
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
        writer.close();
    }

    public Thread createMergeThread(List<File> filesList, String tag, String destination, String name,
                                    Session session) {
        return new Thread(() -> {
            Collections.sort(filesList, Collections.reverseOrder());
            List<File> mergeList = new ArrayList<>();
            for (int i = 0; i < filesList.size(); i++) {
                mergeList.add(filesList.get(i));
                if (mergeList.size() == 25000) {
                    performMergeAndWrite(tag, mergeList, destination, name, session);
                    mergeList.clear();
                }
            }
            if (!mergeList.isEmpty()) {
                performMergeAndWrite(tag, mergeList, destination, name, session);
            }
        });
    }

    private void performMergeAndWrite(String tag, List<File> fileList, String destination, String name,
                                      Session session) {
        Document mergedDoc = null;
        try {
            mergedDoc = merge("/STEP-ProductInformation/" + tag, fileList.toArray(File[]::new));
        } catch (Exception e) {
            writeTXT("Fehler beim Mergen: " + name);
            e.printStackTrace();
        }

        try {
            String fileName = "";
            if (fileList.size() > 0) {
                fileName = fileList.get(0).getName();
            }
            //uploadToSFTP(mergedDoc, destination, name, session, fileName);
            write(mergedDoc, destination, name, fileName);
        } catch (TransformerConfigurationException | IOException e) {
            e.printStackTrace();
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
    private class FilesComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
            String a = "";
            String b = "";
            int result = 0;
            try {
                if (o1.getName().split("_").length > 2 && o2.getName().split("_").length > 2) {
                    a = sdf2.format(sdf.parse(o1.getName().split("_")[2].trim()));
                    b = sdf2.format(sdf.parse(o2.getName().split("_")[2].trim()));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (checkEndsWith(o1.getName().split("_")[1].trim()) > checkEndsWith(o2.getName().split("_")[1].trim())) {
                result = -1;
            } else if (checkEndsWith(o1.getName().split("_")[1].trim()) <
                    checkEndsWith(o2.getName().split("_")[1].trim())) {
                result = 1;
            } else {
                if (o1.getName().split("_").length > 3 && o2.getName().split("_").length > 3) {
                    if (checkEndsWith(a) > checkEndsWith(b)) {
                        result = -1;
                    } else if (checkEndsWith(a) < checkEndsWith(b)) {
                        result = 1;
                    } else {
                        if (checkEndsWith(o1.getName().split("_")[3].trim()) >
                                checkEndsWith(o2.getName().split("_")[3].trim())) {
                            result = -1;
                        } else if (checkEndsWith(o1.getName().split("_")[3].trim()) <
                                checkEndsWith(o2.getName().split("_")[3].trim())) {
                            result = 1;
                        }
                    }
                }
            }
            return result;

        }

    }

    private int checkEndsWith(String name) {
        if (name.matches(".*[a-zA-Z].*")) {
            name = "0";
        }
        return name.endsWith(".xml") ? Integer.parseInt(StringUtils.removeEnd(name, ".xml")) : Integer.parseInt(name);
    }
}