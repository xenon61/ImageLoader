/*
 * Скопировал класс de.innosystec.unrar.testutil.ExtractArchive, потому что потребовалось
 * внести небольшие изменения в код, в частности 
 * в процедуре public static void extractArchive(File archive, File destination) {
 * перед выходом была добавлена строка 
 * arch.close();
 * так как без закрытия не получалось удалить архив.
 */
package sample;

//package de.innosystec.unrar.testutil;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * extract an archive to the given location
 * 
 * @author edmund wagner
 * 
 */
public class ExtractArchive {

    //private static Log logger = LogFactory.getLog(ExtractArchive.class.getName());

    public static void extractArchive(String archive, String destination) {
       // JOptionPane.showMessageDialog(null,"I'm in method!");
	if (archive == null || destination == null) {
        JOptionPane.showMessageDialog(null,"archive and destination must me set");
	    throw new RuntimeException("archive and destination must me set");
	}
	File arch = new File(archive);
	if (!arch.exists()) {
        JOptionPane.showMessageDialog(null,"the archive does not exit:");
	    throw new RuntimeException("the archive does not exit: " + archive);

	}
	File dest = new File(destination);
	if (!dest.exists() || !dest.isDirectory()) {
        JOptionPane.showMessageDialog(null,"the destination must exist and point to a directory:");
	    throw new RuntimeException("the destination must exist and point to a directory: "+ destination);
	}
	extractArchive(arch, dest);
    }

    public static void main(String[] args) {
	if (args.length == 2) {
	    extractArchive(args[0], args[1]);
	} else {
	    System.out.println("usage: java -jar extractArchive.jar <thearchive> <the destination directory>");
	}
    }

    public static void extractArchive(File archive, File destination) {
	Archive arch = null;
	try {
	    arch = new Archive(archive);
	} catch (RarException e) {
	    //logger.error(e);
	    JOptionPane.showMessageDialog(null, "Ошибка разархивации.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
	} catch (IOException e) {
	    //logger.error(e1);
	    JOptionPane.showMessageDialog(null, "Ошибка чтения архива.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
	}
        catch(Exception e){
            JOptionPane.showMessageDialog(null, "Фатальная ошибка.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
        }
        
        
        
	if (arch != null) {
            if (arch.isEncrypted()) {
		//logger.warn("archive is encrypted cannot extreact");
		JOptionPane.showMessageDialog(null, "Невозможно извлечь файлы, так как архив зашифрован.", null, JOptionPane.ERROR_MESSAGE);
		return;
	    }
	    FileHeader fh = null;
	    while (true) {
		fh = arch.nextFileHeader();
		if (fh == null) {
		    break;
		}
		if (fh.isEncrypted()) {
		    //logger.warn("file is encrypted cannot extract: "+ fh.getFileNameString());
		    JOptionPane.showMessageDialog(null, "Невозможно извлечь файл "+ fh.getFileNameString()+" так как он зашифрован.", null, JOptionPane.ERROR_MESSAGE);
		    continue;
		}
		//logger.info("extracting: " + fh.getFileNameString());
		try {
		    if (fh.isDirectory()) {
			createDirectory(fh, destination);
		    } else {
			File f = createFile(fh, destination);
                        
			OutputStream stream = new FileOutputStream(f);
			arch.extractFile(fh, stream);
			stream.close();
		    }
		} catch (IOException e) {
		    //logger.error("error extracting the file", e);
		    JOptionPane.showMessageDialog(null, "Ошибка разархивации файла.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
		} catch (RarException e) {
		    //logger.error("error extraction the file", e);
		    JOptionPane.showMessageDialog(null, "Ошибка разархивации файла.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
		}
	    }
	}
	try {
	    if(arch!=null)
		arch.close();
	} catch (IOException ex) {  JOptionPane.showMessageDialog(null,"чтото");}
    }

    private static File createFile(FileHeader fh, File destination) {
	File f = null;
	String name = null;
	if (fh.isFileHeader() && fh.isUnicode()) {
	    name = fh.getFileNameW();
	} else {
	    name = fh.getFileNameString();
	}
	f = new File(destination, name);
	if (!f.exists()) {
	    try {
		f = makeFile(destination, name);
	    } catch (IOException e) {
		//logger.error("error creating the new file: " + f.getName(), e);
		JOptionPane.showMessageDialog(null, "Ошибка сохранения извлеченного файла на диск.\n"+e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
	    }
	}
	return f;
    }

    private static File makeFile(File destination, String name)
	    throws IOException {
//	String[] dirs = name.split("\\\\");
	String[] dirs = name.split("/");
	if (dirs == null) {
	    return null;
	}
	String path = "";
	int size = dirs.length;
	if (size == 1) {
	    return new File(destination, name);
	} else if (size > 1) {
	    for (int i = 0; i < dirs.length - 1; i++) {
		path = path + File.separator + dirs[i];
		new File(destination, path).mkdir();
	    }
	    path = path + File.separator + dirs[dirs.length - 1];
	    File f = new File(destination, path);
	    f.createNewFile();
	    return f;
	} else {
	    return null;
	}
    }

    private static void createDirectory(FileHeader fh, File destination) {
	File f = null;
	if (fh.isDirectory() && fh.isUnicode()) {
	    f = new File(destination, fh.getFileNameW());
	    if (!f.exists()) {
		makeDirectory(destination, fh.getFileNameW());
	    }
	} else if (fh.isDirectory() && !fh.isUnicode()) {
	    f = new File(destination, fh.getFileNameString());
	    if (!f.exists()) {
		makeDirectory(destination, fh.getFileNameString());
	    }
	}
    }

    private static void makeDirectory(File destination, String fileName) {
//	String[] dirs = fileName.split("\\\\");
	String[] dirs = fileName.split("/");
	if (dirs == null) {
	    return;
	}
	String path = "";
	for (String dir : dirs) {
	    path = path + File.separator + dir;
	    new File(destination, path).mkdir();
	}

    }
}