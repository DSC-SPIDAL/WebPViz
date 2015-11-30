package models.xml;

import javax.xml.bind.*;
import java.io.*;

public class XMLLoader {
    public static Plotviz load(InputStream file) throws Exception {
        try {
            JAXBContext ctx = JAXBContext.newInstance(Plotviz.class);
            Unmarshaller um = ctx.createUnmarshaller();
            return (Plotviz) um.unmarshal(file);
        } catch (JAXBException e) {
            throw new Exception("Failed to load file.", e);
        }
    }

    public static void save(File file, Plotviz plotviz) throws Exception {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            JAXBContext ctx = JAXBContext.newInstance(Plotviz.class);
            Marshaller ma = ctx.createMarshaller();
            ma.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ma.marshal(plotviz, fileOutputStream);
        } catch (FileNotFoundException | JAXBException e) {
            throw new Exception("Failed to save plotviz file", e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }


}
