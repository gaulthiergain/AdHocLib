package com.montefiore.gaulthiergain.adhoclibrary.datalink.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.Data;

import java.io.IOException;

public class DataDeserializer extends StdDeserializer<Data> {

    private static final String DEST_IP_ADDRESS = "destIpAddress";
    private static final String PAYLOAD = "payload";

    public DataDeserializer() {
        this(null);
    }

    @Override
    public Data deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

        JsonNode treeNode = jsonParser.readValueAsTree();

        if (treeNode == null) {
            return null;
        }

        String ip = null;
        if (treeNode.get(DEST_IP_ADDRESS) != null) {
            ip = treeNode.get(DEST_IP_ADDRESS).asText();
        }

        Object payload = null;
        if (treeNode.get(PAYLOAD) != null) {
            String property = treeNode.get(PAYLOAD).fields().next().getKey();
            if (property != null) {
                try {
                    payload = processType(treeNode, property);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    payload = null;
                }
            } else {
                payload = null;
            }
        }

        return new Data(ip, payload);
    }

    private Object processType(JsonNode treeNode, String property)
            throws JsonProcessingException, ClassNotFoundException {

        ObjectMapper mapper = new ObjectMapper();
        Class<?> cls = Class.forName(property);
        return mapper.treeToValue(treeNode.get(PAYLOAD).get(property), cls);
    }

    private DataDeserializer(Class<Data> vc) {
        super(vc);
    }

}

