package com.montefiore.gaulthiergain.adhoclibrary.datalink.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public class MessageDeserializer extends StdDeserializer<MessageAdHoc> {

    private static final String HEADER = "header";
    private static final String PDU = "pdu";

    public MessageDeserializer() {
        this(null);
    }

    private MessageDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MessageAdHoc deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.readValueAsTree();

        System.out.println(">>>" + treeNode);
        if (treeNode == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();

        Header header = null;
        if (treeNode.get(HEADER) != null) {
            header = mapper.treeToValue(treeNode.get(HEADER), Header.class);
        }

        Object pdu = null;
        if (treeNode.get(PDU) != null) {
            String property = treeNode.get(PDU).fields().next().getKey();
            if (property != null) {
                try {
                    pdu = processType(treeNode, property);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    pdu = null;
                }
            } else {
                pdu = null;
            }
        }

        return new MessageAdHoc(header, pdu);
    }

    private Object processType(JsonNode treeNode, String property)
            throws JsonProcessingException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Class<?> cls = Class.forName(property);
        return mapper.treeToValue(treeNode.get(PDU).get(property), cls);
    }
}
