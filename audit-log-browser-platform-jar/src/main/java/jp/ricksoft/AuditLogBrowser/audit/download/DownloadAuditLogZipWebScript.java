/*
 * Copyright 2018 Ricksoft Co., Ltd.
 * All rights reserved.
 */
package jp.ricksoft.AuditLogBrowser.audit.download;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.google.common.io.Files;

import jp.ricksoft.AuditLogBrowser.file.CSVFileManager;
import jp.ricksoft.AuditLogBrowser.file.FileManager;
import jp.ricksoft.AuditLogBrowser.file.ZipFileManager;

public class DownloadAuditLogZipWebScript extends AbstractWebScript {

    private static final String ENCODING_UTF8 = "UTF-8";
    private static final String MIMETYPE_ZIP  = "application/zip";
    
    private Properties properties;
    private CSVFileManager csvManager;
    private ZipFileManager zipManager;
    private FileManager fileManager;
    
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    public void setCsvManager(CSVFileManager csvManager) {
        this.csvManager = csvManager;
    }
    
    public void setZipManager(ZipFileManager zipManager) {
        this.zipManager = zipManager;
    }
    
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) {
        String tmpDirName       = properties.getProperty("AuditLogBrowser.schedule.download.directoryname.tmp");
        String zipName          = properties.getProperty("AuditLogBrowser.schedule.download.filename.zip");
        String msgNoCsv         = properties.getProperty("AuditLogBrowser.schedule.download.message.no-csv");
        String msgFailCreateZip = properties.getProperty("AuditLogBrowser.schedule.download.message.fail-create-zip");

        // zipファイルを返却する為にResponse Headerを設定
        res.setContentType(MIMETYPE_ZIP);
        res.setContentEncoding(ENCODING_UTF8);
        res.setHeader("Content-Disposition", "attachment;filename=" + zipName);
        res.setHeader("Content-Transfer-Encoding", "binary");
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, post-check=0, pre-check=0");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "0");

        // 取得期間設定
        String fromDate = req.getParameter("fromDate");
        String fromTime = req.getParameter("fromTime");
        String toDate   = req.getParameter("toDate");
        String toTime   = req.getParameter("toTime");
        String user     = req.getParameter("user");
        File tmpDir = null;

        try {
            
            // For CSV and ZIP place.
            tmpDir = fileManager.createDir(System.getProperty("java.io.tmpdir"), tmpDirName);
            
            if (!tmpDir.exists()) {
                throw new IOException(msgFailCreateZip);
            }
            
            csvManager.createAuditLogsCSV(fromDate, fromTime, toDate, toTime, user, tmpDir);
            
            // Check whether csv exist.
            if (tmpDir.list().length < 1) {
                throw new IOException(msgNoCsv);
            }
            
            File zip = zipManager.prepareZip(tmpDir);
            
            // Check whether zip exist.
            if (!zip.exists()) {
                throw new IOException(msgFailCreateZip);
            }

            // Path csv to OutputStream.
            Files.copy(zip, res.getOutputStream());

        } catch (IOException e) {
            throw new WebScriptException(e.getMessage());
        } finally {
            if (tmpDir.exists()) {
                fileManager.deleteAllFiles(tmpDir);
            }
        }
    }
    
}
