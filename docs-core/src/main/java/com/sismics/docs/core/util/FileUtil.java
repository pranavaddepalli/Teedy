package com.sismics.docs.core.util;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.Tesseract;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sismics.docs.core.dao.jpa.FileDao;
import com.sismics.docs.core.model.jpa.Document;
import com.sismics.docs.core.model.jpa.File;

/**
 * File entity utilities.
 * 
 * @author bgamard
 */
public class FileUtil {
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    
    /**
     * OCR a file.
     * 
     * @param document Document linked to the file
     * @param file File to OCR
     */
    public static void ocrFile(Document document, final File file) {
        Tesseract instance = Tesseract.getInstance();
        java.io.File storedfile = Paths.get(DirectoryUtil.getStorageDirectory().getPath(), file.getId()).toFile();
        String content = null;
        BufferedImage image = null;
        try {
            image = ImageIO.read(storedfile);
        } catch (IOException e) {
            log.error("Error reading the image " + storedfile, e);
        }
        
        // Upscale and grayscale the image
        BufferedImage resizedImage = Scalr.resize(image, Method.AUTOMATIC, Mode.AUTOMATIC, 3500,
                new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null));
        image.flush();
        image = resizedImage;

        // OCR the file
        try {
            instance.setLanguage(document.getLanguage());
            content = instance.doOCR(image);
        } catch (Exception e) {
            log.error("Error while OCR-izing the file " + storedfile, e);
        }
        
        file.setContent(content);
        
        // Store the OCR-ization result in the database
        TransactionUtil.handle(new Runnable() {
            @Override
            public void run() {
                FileDao fileDao = new FileDao();
                fileDao.updateContent(file);
            }
        });
    }
}