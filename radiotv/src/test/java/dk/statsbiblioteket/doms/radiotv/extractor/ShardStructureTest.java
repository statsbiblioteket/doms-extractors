package dk.statsbiblioteket.doms.radiotv.extractor;

import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ProcessorException;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ShardStructure;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: 1/4/12
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShardStructureTest extends TestCase {

    public void testXML() throws ProcessorException, ParserConfigurationException {
        ShardStructure structure = new ShardStructure();
        ShardStructure.MissingStart ms = new ShardStructure.MissingStart();
        ms.setMissingSeconds(101);
        structure.setMissingStart(ms);
        Document doc = structure.getXml();
        DOMImplementationLS domImplLS = (DOMImplementationLS) doc
                .getImplementation();
        LSSerializer serializer = domImplLS.createLSSerializer();
        String str = serializer.writeToString(doc);
        System.out.println(str);
    }


    public void testMarshall() throws JAXBException, IOException {
        ShardStructure structure = new ShardStructure();
        ShardStructure.MissingStart ms = new ShardStructure.MissingStart();
        ms.setMissingSeconds(101);
        structure.setMissingStart(ms);
        ShardStructure.Hole hole = new ShardStructure.Hole();
        hole.setFilePath1("file1");
        hole.setFilePath2("file2");
        hole.setHoleLength(1010L);
        structure.addHole(hole);
        JAXBContext context = JAXBContext.newInstance(ShardStructure.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(structure, System.out);
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                Result result = new StreamResult(System.out);
                result.setSystemId("");
                return result;
            }
        });
    }

}
