package com.montefiore.gaulthiergain.adhoclibrary.datalink.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class Serializer extends StdSerializer<Object> {

    public Serializer() {
        this(null);
    }

    public Serializer(Class<Object> t) {
        super(t);
    }

    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(o.getClass().getName(), o);
        jsonGenerator.writeEndObject();
    }
}
