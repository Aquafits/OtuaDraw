package com.otuadraw.util;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.otuadraw.data.model.InkTrail;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class QuickDrawUtil {

    public String getQuickDrawPayLoad(InkTrail trail, int width, int height) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(printStream, "UTF-8"));

        writer.beginObject();
        writer.name("input_type").value(0);
        writer.name("requests");
        writeRequestArray(writer, trail, width, height);
        writer.endObject();

        writer.flush();
        return byteArrayOutputStream.toString();
    }

    private void writeRequestArray(JsonWriter writer, InkTrail trail, int width, int height) throws IOException {
        writer.beginArray();                                                //[
        writer.beginObject();                                               //  {
        writer.name("language").value("quickdraw");                         //    "language":"quickdraw",
        writer.name("writing_guide");                                       //    "writing_guide":
        writer.beginObject()                                                //      {
                .name("width").value(width)                                 //        "width":${width}
                .name("height").value(height)                               //        "height":${height}
                .endObject();                                               //      },

        writer.name("ink");                                                 //     "ink":

        writer.beginArray();                                                //       [
        writer.beginArray();                                                //         [
        writer.beginArray();                                                //           [
        for(int x:trail.getXList()){
            writer.value(x);                                                //             1,2,3
        }
        writer.endArray();                                                  //           ],
        writer.beginArray();                                                //           [
        for(int y:trail.getYList()){
            writer.value(y);                                                //             1,2,3
        }
        writer.endArray();                                                  //           ],
        writer.beginArray();                                                //           [
        for(Long t:trail.getTList()){
            writer.value(t);                                                //             1,2,3
        }
        writer.endArray();                                                  //           ],
        writer.endArray();                                                  //         ]
        writer.endArray();                                                  //       ]

        writer.endObject();                                                 //  }
        writer.endArray();                                                  //]
    }

    public List<String> getBestGuesses(String responseString) throws IOException {
        List<String> ret = new ArrayList<>();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
        JsonReader reader = new JsonReader(new InputStreamReader(byteArrayInputStream, "UTF-8"));
        reader.beginArray();
        reader.skipValue();
        reader.beginArray();
        reader.beginArray();
        reader.skipValue();
        reader.beginArray();
        while (reader.hasNext()){
            ret.add(reader.nextString());
        }
        reader.endArray();
        reader.close();
        byteArrayInputStream.close();

        return ret;
    }
}
