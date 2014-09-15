package org.earthcube.geosoft.software.classes.audit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

public class DRAT extends Thread {
  public static boolean BUSY = false;
  public static enum Repo {GIT, SVN, ZIP, TARGZ, FILE};
  public static HashMap<String, String> RESULTS = 
      new HashMap<String, String>();
  
  private String repourl;
  private Repo type;
  private String drathome;
  
  public DRAT(String repo, String type, String drathome) {
    this.repourl = repo;
    this.drathome = drathome;
    if(type != null) {
      if(type.equals("git"))
        this.type = Repo.GIT;
      else if(type.equals("svn"))
        this.type = Repo.SVN;
      else if(type.equals("zip"))
        this.type = Repo.ZIP;
      else if(type.equals("targz"))
        this.type = Repo.TARGZ;
      else
        this.type = Repo.FILE;
    }
    else {
      if(repo.endsWith(".git")) 
        this.type = Repo.GIT;
      else if(repo.endsWith(".zip"))
        this.type = Repo.ZIP;
      else if(repo.endsWith(".tar.gz") || repo.endsWith(".tgz"))
        this.type = Repo.TARGZ;
      else if(repo.matches("svn"))
        this.type = Repo.SVN;
      else
        this.type = Repo.FILE;
    }
  }
  
  private boolean myRun(File cwd, String... args) 
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(args);
    if(cwd != null)
      pb.directory(cwd);
    pb.environment().put("DRAT_HOME", drathome);
    pb.redirectErrorStream(true);
    Process proc = pb.start();
    BufferedReader br = new BufferedReader( 
        new InputStreamReader( proc.getInputStream() ) );
    @SuppressWarnings("unused")
    String line;
    while ( (line = br.readLine()) != null ) { /*System.out.println(line);*/ }
    return (proc.waitFor() == 0);
  }
  
  public void run() {
    if(DRAT.BUSY)
      return;

    if(RESULTS.containsKey(this.repourl))
      return;
    
    DRAT.BUSY = true;
    File f = null;
    try {
      f = File.createTempFile("Drat", "");
      if(f.delete() && f.mkdir()) {
        if(this.fetchRepo(f)) {
          
          if(!myRun(f, drathome + "/bin/oodt", "stop"))
            return;
          Thread.sleep(2000);
          
          if(!myRun(f, drathome + "/bin/drat", "reset"))
            return;
          Thread.sleep(2000);
          
          if(!myRun(f, drathome + "/bin/oodt", "start"))
            return;
          Thread.sleep(5000);
          
          if(!myRun(f, drathome + "/bin/drat", "go", f.getAbsolutePath()))
            return;
          Thread.sleep(2000);
          
          if(!myRun(f, drathome + "/bin/oodt", "stop"))
            return;
          Thread.sleep(2000);
          
          File dir = new File(drathome + "/data/archive/rataggregate");
          if(dir.exists() && dir.isDirectory()) {
            for(File csvf : dir.listFiles()) {
              String aggr = FileUtils.readFileToString(csvf);
              RESULTS.put(this.repourl, aggr);
            }
          }
          else {
            RESULTS.put(this.repourl, "");
          }
          
          return;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      DRAT.BUSY = false;
      if(f != null) {
        try {
          FileUtils.deleteDirectory(f);
        } catch (IOException e) { }
      }
    }
  }
  
  private boolean fetchRepo(File tmpfolder) {
    try {
      if(this.type == Repo.GIT) {
        return myRun(tmpfolder, "git", "clone", this.repourl);
      }
      else if(this.type == Repo.SVN) {
        return myRun(tmpfolder, "svn", "co", this.repourl);
      }
      else if(this.type == Repo.ZIP) {
        File zipfile = new File(tmpfolder.getAbsolutePath() 
            + File.separator + "repo.zip");
        FileUtils.copyURLToFile(new URL(this.repourl), zipfile);
        return myRun(tmpfolder, "unzip", zipfile.getAbsolutePath());
      }
      else if(this.type == Repo.TARGZ) {
        File tgzfile = new File(tmpfolder.getAbsolutePath() 
            + File.separator + "repo.tar.gz");
        FileUtils.copyURLToFile(new URL(this.repourl), tgzfile);
        return myRun(tmpfolder, "tar", "-xvzf", tgzfile.getAbsolutePath());
      }
      else if(this.type == Repo.FILE) {
        String fname = this.repourl.replaceAll(".*/", "");
        File file = new File(tmpfolder.getAbsolutePath() 
            + File.separator + fname);
        FileUtils.copyURLToFile(new URL(this.repourl), file);
        return file.exists();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
