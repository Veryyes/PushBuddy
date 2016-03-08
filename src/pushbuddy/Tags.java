/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pushbuddy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Represents a file with tags
 * file format: pathToFileOnCloud;pathToCorrespondingFileOnLocalMachine\n
 * @author Brandon
 */
public class Tags {
    private static class Tag implements Comparable<Tag> {        
        String cloudPath;
        String localPath;
        
        @Override
        public int compareTo(Tag o) {
            return this.cloudPath.compareTo(o.cloudPath);
        }
        
        private Tag(String cloudPath, String localPath ){
            this.cloudPath=cloudPath;
            this.localPath=localPath;
        }
    }
    
    private LinkedList<Tag> tags; 
    private File file;
    
    public Tags(File writeFile) {
        file = writeFile;
        tags = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while ((line = br.readLine()) != null) {
                String[] tagComponents = line.split(";");
                add(tagComponents[0], tagComponents[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Adds a new tagged file 
     * @param localPath the local path on the machine
     * @param cloudPath the path of the file on the cloud
     * @return true if valid parameters are used
     */
    public boolean add(String cloudPath, String localPath) {
        if(localPath==null||cloudPath==null||localPath.length()==0||cloudPath.length()==0)
            return false;

        File f = new File(localPath);
        if (f.isDirectory()) {
            ArrayList<File> files = new ArrayList();
            files.add(f);
            getLocalSubfiles(f.toPath(), files);
            
            for (File sub : files) {
                String subCloud = "/" + f.toPath().getParent().relativize(sub.toPath()).toString().replace("\\", "/");
                String subLocal = sub.getAbsolutePath();
                tags.add(new Tag(subCloud, subLocal));
            }
        } else {
            tags.add(new Tag(cloudPath, localPath));
        }

        return true;
    }
    
    private void getLocalSubfiles(Path rootPath, ArrayList<File> paths) {
        File root = new File(rootPath.toUri());
        for (File sub : root.listFiles()) {
            if (sub.isDirectory()) {
                getLocalSubfiles(sub.toPath(), paths);
            } else {
                paths.add(sub);
            }
        }
    }
    
    /**
     * removes a tag based on the path on cloud
     * @param cloudPath the path of the file on the cloud
     * @return true if the file at cloud path exists
     */
    public boolean remove(String cloudPath){
        int index = Collections.binarySearch(tags, new Tag(cloudPath,null));
        if(index<=0)
            return false;
        tags.remove(index);
        return true;
    }
    /**
     * O(n) linear search :(
     * @param local The local Path of the file
     * @return The corresponding path of the file in cloud, null if there is not a corresponding file
     */
    public String getCloudPath(String local){
       for(Tag t:tags){
           if(t.localPath.equals(local))
               return t.cloudPath;
       }
       return null;
    }
    /**
     * Sorry Linear O(n)
     * @param cloud the path of the file in cloud
     * @return the corresponding path of the file in the local machine
     */
    public String getLocalPath(String cloud){
        /*int index = Collections.binarySearch(tags, new Tag(cloud,null)); //Not working, fix later
        if(index<=0)
            return null;
        return tags.get(index).localPath;*/
        for(Tag t:tags)
            if(t.cloudPath.equals(cloud))
                return t.localPath;
        return null;
    }   
    public int size(){
        return tags.size();
    }
    public boolean write(){
        try(FileWriter fw = new FileWriter(file)){
            for(Tag t: tags){
                fw.write(t.cloudPath+";"+t.localPath+"\n");
            }
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public File[] getLocalFiles(){
        File[] files = new File[size()];
        int i=0;
        for(Tag t:tags){
            files[i] = new File(t.localPath);
            i++;
        }
        return files;
    }
    public void printContents(){
        for(Tag t:tags){
            System.out.println(t.cloudPath+";"+t.localPath);
        }
    }
    
    public void clear() {
        tags.clear();
    }
}
