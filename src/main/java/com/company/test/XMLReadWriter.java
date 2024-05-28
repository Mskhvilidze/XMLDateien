package com.company.test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;


public class XMLReadWriter {
    private boolean isReady = false;
    private List<Thread> list = new ArrayList();

    public boolean isReady() {
        return isReady;
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
                          File... files) throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        docBuilderFactory
                .setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = docBuilderFactory
                .newDocumentBuilder();
        Document base = docBuilder.parse(files[0]);

        Node results = (Node) expression.evaluate(base,
                XPathConstants.NODE);
        if (results == null) {
            throw new IOException(files[0]
                    + ": Fehlschlag" + files.length);
        }

        for (int i = 1; i < files.length; i++) {
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
    public void write(Document doc, String dest) throws TransformerConfigurationException, IOException {
        String uuid = UUID.randomUUID().toString();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new StringWriter());
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dest + "\\" + "File" + "_" + uuid + ".xml")
                , StandardCharsets.UTF_8))) {
            transformer.transform(source, result);
            String xmlOutput = result.getWriter().toString();
            output.write(xmlOutput);
            output.close();
            System.out.println("Ready");
        } catch (TransformerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * create for XML files
     *
     * @param files
     * @throws Exception
     */
    public void createForXMLFile(File[] files, String dest) throws Exception {

        Thread thread = new Thread(() -> {
            File[] files1 = new File[files.length / 4];
            int count = 0;
            for (int j = 0; j < files.length / 4; j++) {
                files1[count++] = files[j];
                if (count == files1.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread thread1 = new Thread(() -> {
            File[] files2 = new File[files.length / 4];
            int count = 0;
            for (int j = files.length / 4; j < files.length / 2; j++) {
                files2[count++] = files[j];
                if (count == files2.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            File[] files3 = new File[files.length / 4];
            int count = 0;
            for (int j = files.length / 2; j < files.length; j++) {
                files3[count++] = files[j];
                if (count == files3.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files3);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread thread3 = new Thread(() -> {
            int help = 0;
            final int i = files.length - (files.length / 4) * 3;
            File[] files4 = new File[i];
            help = files.length - i;
            int count = 0;
            for (int j = help; j < files.length; j++) {
                files4[count++] = files[j];
                if (count == files4.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files4);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        list.add(thread);
        list.add(thread1);
        list.add(thread2);
        list.add(thread3);
    }

    /**
     * create two XML-Dateien
     *
     * @param files
     * @throws Exception
     */
    public void createTwoXMLFile(File[] files, String dest) throws Exception {

        Thread thread = new Thread(() -> {
            File[] files1 = new File[files.length / 4];
            int count = 0;
            for (int j = 0; j < files.length / 4; j++) {
                files1[count++] = files[j];
                if (count == files1.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread thread1 = new Thread(() -> {
            File[] files2 = new File[files.length / 4];
            int count = 0;
            for (int j = files.length / 4; j < files.length / 2; j++) {
                files2[count++] = files[j];
                if (count == files2.length) {
                    break;
                }
            }
            Document doc = null;
            try {
                doc = merge("/STEP-ProductInformation/Products", files2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                write(doc, dest);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        thread1.start();
    }

    /**
     * Erstellen Eine XML-Datei
     *
     * @param files
     * @throws Exception
     */
    public void createOneXMLFile(File[] files, String dest) throws Exception {
        int count = 0;

        File[] files1 = new File[files.length];

        for (int j = 0; j < files.length; j++) {
            files1[count++] = files[j];
            if (count == files1.length) {
                break;
            }
        }
        Document doc = merge("/STEP-ProductInformation/Products", files1);
        write(doc, dest);
        count = 0;
    }

    public List<Thread> getList(){
        return list;
    }

    /**
     * Gets the base location of the given class.
     * <p>
     * If the class is directly on the file system (e.g.,
     * "/path/to/my/package/MyClass.class") then it will return the base directory
     * (e.g., "file:/path/to").
     * </p>
     * <p>
     * If the class is within a JAR file (e.g.,
     * "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the
     * path to the JAR (e.g., "file:/path/to/my-jar.jar").
     * </p>
     *
     * @param c The class whose location is desired.
     */
    public static URL getLocation(final Class<?> c) {
        if (c == null) return null; // could not load the class

        // try the easy way first
        try {
            final URL codeSourceLocation =
                    c.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null) return codeSourceLocation;
        } catch (final SecurityException e) {
            // NB: Cannot access protection domain.
        } catch (final NullPointerException e) {
            // NB: Protection domain or code source is null.
        }

        // NB: The easy way failed, so we try the hard way. We ask for the class
        // itself as a resource, then strip the class's path from the URL string,
        // leaving the base path.

        // get the class's raw resource path
        final URL classResource = c.getResource(c.getSimpleName() + ".class");
        if (classResource == null) return null; // cannot find class resource

        final String url = classResource.toString();
        final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
        if (!url.endsWith(suffix)) return null; // weird URL

        // strip the class's path from the URL string
        final String base = url.substring(0, url.length() - suffix.length());

        String path = base;

        // remove the "jar:" prefix and "!/" suffix, if present
        if (path.startsWith("jar:")) path = path.substring(4, path.length() - 2);

        try {
            return new URL(path);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except that
     * it also handles "jar:file:" URLs, returning the path to the JAR file.
     * </p>
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final URL url) {
        return url == null ? null : urlToFile(url.toString());
    }

    /**
     * Converts the given URL string to its corresponding {@link File}.
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final String url) {
        String path = url;
        if (path.startsWith("jar:")) {
            // remove "jar:" prefix and "!/" suffix
            final int index = path.indexOf("!/");
            path = path.substring(4, index);
        }
        try {
            if (path.matches("file:[A-Za-z]:.*")) {
                path = "file:/" + path.substring(5);
            }
            return new File(new URL(path).toURI());
        } catch (final MalformedURLException e) {
            // NB: URL is not completely well-formed.
        } catch (final URISyntaxException e) {
            // NB: URL is not completely well-formed.
        }
        if (path.startsWith("file:")) {
            // pass through the URL as-is, minus "file:" prefix
            path = path.substring(5);
            return new File(path);
        }
        throw new IllegalArgumentException("Invalid URL: " + url);
    }
}
