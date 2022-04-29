package me.doublenico.bedstom.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;

public class StringUtils {
    public static String capitaliteFirst(String str) {
        str = str.toLowerCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    public static JsonObject readJson(String path, String template) throws IOException {
        File ranksFile = new File(path);
        JsonObject json = null;
        if (ranksFile.exists()) {
            StringBuilder full = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(ranksFile));
            String line = br.readLine();
            while (line != null) {
                full.append(line);
                line = br.readLine();
            }
            json = new Gson().fromJson(full.toString(), JsonObject.class);
        } else {
            ranksFile.createNewFile();
            json = new Gson().fromJson(template,JsonObject.class);
            BufferedWriter bw = new BufferedWriter(new FileWriter(ranksFile));
            bw.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            bw.flush();
            bw.close();
        }
        return json;
    }
}
