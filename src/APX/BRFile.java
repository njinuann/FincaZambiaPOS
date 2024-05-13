package APX;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class BRFile
{
    public InputStream bytesToStream(byte[] bytes)
    {
        return new ByteArrayInputStream(bytes);
    }

    public InputStream fileToStream(String filePath) throws Exception
    {
        return new FileInputStream(new File(filePath));
    }

    public byte[] getFileBytes(String fileUrl) throws Exception
    {
        File file = new File(fileUrl);
        byte[] fileBytes = new byte[(int) file.length()];
        try (FileInputStream fin = new FileInputStream(file))
        {
            fin.read(fileBytes);
        }
        return fileBytes;
    }

    public void writeToFile(String fileName, String contentToWrite) throws Exception
    {
        writeToFile(new File(fileName), contentToWrite);
    }

    public void appendToFile(String fileName, String contentToWrite) throws Exception
    {
        appendToFile(new File(fileName), contentToWrite);
    }

    public void writeToFile(byte[] myBytes, String directory, String fileName) throws Exception
    {
        createDirectory(directory);
        try (FileOutputStream fout = new FileOutputStream(directory + File.separator + fileName, false))
        {
            fout.write(myBytes);
        }
    }

    public void writeToFile(File file, String contentToWrite) throws Exception
    {
        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")))
        {
            out.write(contentToWrite.trim() + "\r\n");
        }
    }

    public void writeToFile(File file, char[] contentToWrite) throws Exception
    {
        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")))
        {
            out.write(contentToWrite);
        }
    }

    public void appendToFile(File file, String contentToWrite) throws Exception
    {
        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8")))
        {
            out.append(contentToWrite.trim() + "\r\n");
        }
    }

    public boolean moveFile(File sourceFile, String destinationDirectory) throws Exception
    {
        copy(sourceFile, destinationDirectory);
        return deleteFile(sourceFile);
    }

    public File[] getFiles(String dir) throws Exception
    {
        File directory = new File(dir);
        if (directory.exists() && directory.isDirectory())
        {
            return directory.listFiles();
        }
        return new File[0];
    }

    public void copy(File srcFile, String dstDir) throws Exception
    {
        String fExt = (srcFile.getName().indexOf(".") > 0) ? srcFile.getName().substring(srcFile.getName().lastIndexOf(".")) : "";
        String tempFileName = "".equals(fExt) ? srcFile.getName() : srcFile.getName().replace(fExt, ".mds");
        writeToFile(getFileBytes(srcFile.getAbsolutePath()), dstDir, tempFileName);
        if (!"".equals(fExt))
        {
            File tempFile = new File(dstDir, tempFileName);
            tempFile.renameTo(new File(tempFile.getAbsolutePath().replace(".mds", fExt)));
        }
    }

    public boolean deleteFile(String path) throws Exception
    {
        return deleteFile(new File(path));
    }

    public boolean deleteFile(File file) throws Exception
    {
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (File f : files)
            {
                return deleteFile(f);
            }
        }
        return Files.deleteIfExists(file.toPath());
    }

    public void compressFileToGzip(File f) throws Exception
    {
        try (GZIPOutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(f.getAbsolutePath() + ".gz")))); InputStream is = new BufferedInputStream(new FileInputStream(f)))
        {
            int read;
            byte[] buff = new byte[1048576];
            while ((read = is.read(buff)) > 0)
            {
                os.write(buff, 0, read);
            }
            os.finish();
        }
    }

    public String readTextFile(File fileToRead) throws Exception
    {
        String line;
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead))))
        {
            while ((line = bis.readLine()) != null)
            {
                buffer.append(line).append("\r\n");
            }
        }
        return buffer.toString();
    }

    public void addFileToZipArchive(String fileName, InputStream fileInstream, ZipOutputStream out) throws IOException
    {
        try (BufferedInputStream bufferedFileInstream = new BufferedInputStream(fileInstream, 1048576))
        {
            int count;
            byte data[] = new byte[1048576];
            out.putNextEntry(new ZipEntry(fileName));
            while ((count = bufferedFileInstream.read(data, 0, 1048576)) != -1)
            {
                out.write(data, 0, count);
            }
        }
    }

    public String readInputStream(InputStream inputStream) throws IOException
    {
        String line;
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream)))
        {
            while ((line = in.readLine()) != null)
            {
                buffer.append(line);
            }
        }
        return buffer.toString();
    }

    public void extractZipArchive(File zipFile, String destDirectory) throws Exception
    {
        createDirectory(destDirectory);
        try (ZipFile archive = new ZipFile(zipFile))
        {
            Enumeration zipEntries = archive.entries();
            while (zipEntries.hasMoreElements())
            {
                ZipEntry zipEntryFile = (ZipEntry) zipEntries.nextElement();
                try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destDirectory + File.separator + zipEntryFile.getName())); InputStream inputStream = archive.getInputStream(zipEntryFile))
                {
                    int len;
                    byte[] buffer = new byte[1048576];
                    while ((len = inputStream.read(buffer)) > 0)
                    {
                        outputStream.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    public void createDirectory(String directoryPath)
    {
        File dir = new File(directoryPath);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
    }
}
