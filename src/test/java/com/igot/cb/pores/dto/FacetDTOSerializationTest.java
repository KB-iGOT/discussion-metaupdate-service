package com.igot.cb.pores.dto;

import com.igot.cb.pores.elasticsearch.dto.FacetDTO;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class FacetDTOSerializationTest {

    @Test
    void testSerialization() throws Exception {
        FacetDTO dto = new FacetDTO("val", 10L);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(dto);
        oos.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(in);
        FacetDTO deserialized = (FacetDTO) ois.readObject();

        assertEquals("val", deserialized.getValue());
        assertEquals(10L, deserialized.getCount());
    }
}
