package com.montefiore.gaulthiergain.adhoclibrary.datalink.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;

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
    public MessageAdHoc deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode treeNode;
        try {
            treeNode = jsonParser.readValueAsTree();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (treeNode == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();

        Header header = null;
        if (treeNode.get(HEADER) != null) {
            try {
                header = mapper.treeToValue(treeNode.get(HEADER), Header.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }

        Object pdu = null;
        if (treeNode.get(PDU) != NullNode.getInstance()) {
            String property = treeNode.get(PDU).fields().next().getKey();
            if (property != null) {
                try {
                    pdu = processType(treeNode, property);
                } catch (ClassNotFoundException | JsonProcessingException e) {
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
