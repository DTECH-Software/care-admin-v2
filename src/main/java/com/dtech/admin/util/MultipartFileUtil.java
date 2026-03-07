/**
 * User: Himal_J
 * Date: 3/9/2025
 * Time: 7:36 PM
 * <p>
 */

package com.dtech.admin.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Log4j2
public class MultipartFileUtil {

    public static MultipartFile convertToMultipartFile(String base64File,String fileType,String fileName) throws IOException {
        log.info("converting file " + base64File);
        byte[] decodedBytes = Base64.getDecoder().decode(base64File);
        return new MockMultipartFile("file", fileName, fileType, decodedBytes);
    }

}
