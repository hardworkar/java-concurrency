import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws SAXException, ParserConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        File schemaFile = new File ("xmls/schema.xsd");
        docBuilderFactory.setSchema(schemaFactory.newSchema(schemaFile));
        docBuilderFactory.setNamespaceAware(true);
        Document doc =
                docBuilderFactory.newDocumentBuilder().parse("xmls/clean.xml");
        StreamSource styleSheet = new StreamSource(new File("xmls/transform.xsl"));
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer(styleSheet);
        transformer.transform(new DOMSource(doc),
                new StreamResult(new FileOutputStream("output.html")));
        System.out.println("Success!");
    }
}
