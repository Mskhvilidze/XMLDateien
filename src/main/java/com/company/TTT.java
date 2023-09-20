package com.company;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class TTT {
    public static void main(String[] args) {
        String path = "C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\muster\\new\\merge\\test.xml";
        File file = new File(path);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file1 = new File(
                    "C:\\Users\\p05865\\IdeaProjects\\XMLDateien\\muster\\new\\merge\\");
            File[] files = file1.listFiles();
            Document document = builder.newDocument();
            Element element = document.createElement("STEP-ProductInformation");
            Element element1 = document.createElement("Products");
            for (int c = 0; c < files.length; c++) {
                Document doc1 = builder.parse(files[c]);
                Element e1 = null;
                NodeList list = doc1.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    Node p = list.item(i);
                    if (p.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) p;
                        NamedNodeMap map = e.getAttributes();
                        for (int a = 0; a < map.getLength(); a++) {
                            Attr importNode = (Attr) document.importNode(map.item(a), true);
                            element.setAttributeNodeNS(importNode);
                        }
                        NodeList list1 = e.getChildNodes();
                        for (int j = 0; j < list1.getLength(); j++) {
                            Node p1 = list1.item(j);
                            if (p1.getNodeType() == Node.ELEMENT_NODE) {
                                e1 = (Element) p1;
                                NamedNodeMap map1 = e1.getAttributes();
                                for (int a = 0; a < map1.getLength(); a++) {
                                    Attr importNode = (Attr) document.importNode(map1.item(a), true);
                                    element1.setAttributeNodeNS(importNode);
                                }
                            }
                        }
                    }
                }
                element.appendChild(element1);
                // Copy Child Nodes
                NodeList childNodes2 = e1.getChildNodes();
                for (int x = 0; x < childNodes2.getLength(); x++) {
                    Node importedNode = document.importNode(childNodes2.item(x), true);
                    element1.appendChild(importedNode);
                }

            }
            document.appendChild(element);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(System.out);
            t.transform(source, result);
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
