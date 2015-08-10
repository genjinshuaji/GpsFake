package com.example.gpsdemo;

import org.apache.http.util.EncodingUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sy1 on 2015/8/7.
 */
public class FileUnitFromSDCard {
    //д���ݵ�SD�е��ļ�
    public static void writeFileSdcardFile(String fileName,String write_str) throws IOException{
        try{

            FileOutputStream fout = new FileOutputStream(fileName);
            byte [] bytes = write_str.getBytes();

            fout.write(bytes);
            fout.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
    }


    //��SD�е��ļ�
    public static String readFileSdcardFile(String fileName) throws IOException {
        String res="";
        try{
            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte [] buffer = new byte[length];
            fin.read(buffer);

            res = EncodingUtils.getString(buffer, "UTF-8");

            fin.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }

    //��SD�е��ļ�
    public static InputStream readFileSDcardFile(String fileName)  {
        InputStream in = null;
        try{
            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte [] buffer = new byte[length];
            fin.read(buffer);

            in = new BufferedInputStream(fin);
            fin.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
        return in;
    }

    public static void NewDir(String sDir)
    {
        File destDir = new File(sDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
    }
}
