import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class FTPVerify {
    static final String HOST="127.0.0.1", USER="test", PASS="test";
    static final int PORT=2121;
    static final String BASE_DIR="/TarotAssistant_DB/";

    // ==== port of FtpPaths ====
    static String join(String base, String rel){
        String b = base.replaceAll("/+$","");
        String r = rel.trim().replaceAll("^/+","");
        return r.isEmpty()? b : b+"/"+r;
    }
    static String cleanName(String n){
        String s=n.trim();
        while(s.endsWith("/")) s=s.substring(0,s.length()-1).trim();
        return s;
    }

    static FTPClient connectAndLogin() throws Exception {
        FTPClient c=new FTPClient();
        c.setConnectTimeout(10000);
        c.connect(HOST,PORT);
        if(!c.login(USER,PASS)) throw new RuntimeException("login fail: "+c.getReplyString());
        c.enterLocalPassiveMode();
        return c;
    }

    // ==== port of listDirectoryEntries ====
    static List<FTPFile> listDirectoryEntries(FTPClient c, String path) throws Exception {
        List<FTPFile> parsed = new ArrayList<>();
        try {
            FTPFile[] files = c.listFiles(path);
            if (files!=null) for (FTPFile f: files){
                String n = cleanName(f.getName());
                if(!n.isEmpty() && !n.equals(".") && !n.equals("..") && !n.contains("=")) parsed.add(f);
            }
        } catch(Exception ignored){}
        if(!parsed.isEmpty()) return parsed;

        String cwd = c.printWorkingDirectory();
        if (cwd==null) cwd="/";
        List<FTPFile> out=new ArrayList<>();
        String[] names;
        try { names = c.listNames(path); } catch(Exception e){ names=new String[0]; }
        for(String raw: names){
            String name = cleanName(raw.contains("/")? raw.substring(raw.lastIndexOf('/')+1) : raw);
            if(name.isEmpty()||name.equals(".")||name.equals("..")) continue;
            boolean isDir=false;
            try {
                isDir = c.changeWorkingDirectory(name);
                c.changeWorkingDirectory(cwd);
            } catch(Exception ignored){}
            FTPFile f=new FTPFile();
            f.setName(name);
            f.setType(isDir? FTPFile.DIRECTORY_TYPE : FTPFile.FILE_TYPE);
            out.add(f);
        }
        return out;
    }

    // ==== port of deleteContents ====
    static void deleteContents(FTPClient c, String relPath, List<String> remaining) throws Exception {
        for(FTPFile entry: listDirectoryEntries(c, relPath)){
            String name = cleanName(entry.getName());
            if(name.equals(".")||name.equals("..")) continue;
            String childRel = relPath.isEmpty()? name : relPath+"/"+name;
            boolean ok;
            if(entry.isDirectory()){
                deleteContents(c, childRel, remaining);
                ok = c.removeDirectory(childRel);
            } else {
                ok = c.deleteFile(childRel);
            }
            if(!ok) remaining.add(childRel);
        }
    }

    // ==== port of deleteUserFolder ====
    static String deleteUserFolder(FTPClient c, String folder) throws Exception {
        String root = join(BASE_DIR, folder);
        if(!c.changeWorkingDirectory(root)) return "SUCCESS (already gone)";
        List<String> remaining = new ArrayList<>();
        c.changeWorkingDirectory(root);
        deleteContents(c, "", remaining);
        int idx = root.lastIndexOf('/');
        String parent = idx>=0? root.substring(0,idx) : "";
        c.changeWorkingDirectory(parent.isEmpty()? "/" : parent);
        String dirName = root.substring(idx+1);
        if(!c.removeDirectory(dirName) && !c.removeDirectory(root)) remaining.add(folder);
        if(!remaining.isEmpty()) return "PARTIAL: "+new LinkedHashSet<>(remaining)+" reply="+c.getReplyString().trim();
        return "SUCCESS";
    }

    public static void main(String[] args) throws Exception {
        for(String folder: args){
            FTPClient c = connectAndLogin();
            System.out.println(folder + " -> " + deleteUserFolder(c, folder));
            c.logout(); c.disconnect();
        }
    }
}
