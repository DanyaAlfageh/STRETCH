package edu.colostate.cs.fa2017.stretch.util;

import java.io.*;


public class FileEditor {

    String fileName;
    String placeHolder;
    String replacement;
    String group;

    public FileEditor(String fileName, String placeHolder, String replacement, String group){

        this.fileName = fileName;
        this.placeHolder = placeHolder;
        this.replacement = replacement;
        this.group = group;
    }


    public String replace() {

        String oldFileName = fileName;  //"./config/util/template.xml";
        String tmpFileName = fileName.replaceAll("Template", group); //"./config/util/test1.xml";


        //Delete the config file, if exist
        File oldFile = new File(tmpFileName);
        if(oldFile.isFile()){
            oldFile.delete();
        }


        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            //Assuming the order is as in the template..groupname first, then donated,...
            String[] place = placeHolder.split("$$");
            String[] replace = replacement.split("##");

            if(place.length!=replace.length){
                return "ERROR";
            }

            int len = place.length;
            int index = 0;

            br = new BufferedReader(new FileReader(oldFileName));
            bw = new BufferedWriter(new FileWriter(tmpFileName));
            String line;
            while ((line = br.readLine()) != null) {
                if (index < len && line.contains(place[index])){
                    line = line.replace(place[index], replace[index]);
                    index++;
                }
                bw.write(line+"\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if(br != null)
                    br.close();
            } catch (IOException e) {
                System.out.println(e);
            }
            try {
                if(bw != null)
                    bw.close();
            } catch (IOException e) {
                //
            }
        }
        // Once everything is complete, delete old file..
//        File oldFile = new File(oldFileName);
//        oldFile.delete();

        // And rename tmp file's name to old file name
//        File newFile = new File(tmpFileName);
//        newFile.renameTo(oldFile);

        return tmpFileName;

    }
}
