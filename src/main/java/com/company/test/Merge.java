package com.company.test;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class Merge {
    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        File file1 = new File(
                "C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\muster\\new\\merge\\Crossreference_ SAP_1201204891 - Kopie.xml");
        Document doc1 = db.parse(file1);
        Element rootElement1 = doc1.getDocumentElement();

        File file2 = new File(
                "C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\muster\\new\\merge\\Crossreference_ SAP_1201204891.xml");
        Document doc2 = db.parse(file2);
        Element rootElement2 = doc2.getDocumentElement();

        // Copy Attributes
        NamedNodeMap namedNodeMap2 = rootElement2.getAttributes();
        for (int x = 0; x < namedNodeMap2.getLength(); x++) {
            Attr importedNode = (Attr) doc1.importNode(namedNodeMap2.item(x), true);
            rootElement1.setAttributeNodeNS(importedNode);
        }

        // Copy Child Nodes
        NodeList childNodes2 = rootElement2.getChildNodes();
        for (int x = 0; x < childNodes2.getLength(); x++) {
            Node importedNode = doc1.importNode(childNodes2.item(x), true);
            rootElement1.appendChild(importedNode);
        }

        // Output Document
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        DOMSource source = new DOMSource(doc1);
        StreamResult result = new StreamResult(System.out);
        t.transform(source, result);
    }
}
