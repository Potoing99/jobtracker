package com.capstone.jobtracker.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

// 간단 주석만: 확장자/MIME/매직넘버 3단 검사 + 파일명 sanitize
public class FileValidationUtil {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "docx", "doc");
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    public static String sanitize(String name) {
        if (name == null) return null;
        return name.replaceAll("[\\r\\n\\\\/]+", "_");
    }

    public static String extOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase();
    }

    // 1) 확장자 화이트리스트
    public static void requireAllowedExtension(String originalFilename) {
        String ext = extOf(originalFilename);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + ext);
        }
    }

    // 2) MIME 화이트리스트 (멀티파트/OS 추론 둘 다)
    public static void requireAllowedMime(MultipartFile file, Path tempPathForProbe) throws IOException {
        String multipartMime = file.getContentType();
        String probed = null;
        try {
            Files.copy(file.getInputStream(), tempPathForProbe);
            probed = Files.probeContentType(tempPathForProbe);
        } finally {
            try { Files.deleteIfExists(tempPathForProbe); } catch (Exception ignore) {}
        }
        boolean okMulti = multipartMime != null && ALLOWED_MIME.contains(multipartMime);
        boolean okProbed = probed != null && ALLOWED_MIME.contains(probed);
        if (!(okMulti || okProbed)) {
            throw new IllegalArgumentException("허용되지 않은 MIME 타입입니다: " + multipartMime + " / " + probed);
        }
    }

    // 3) 매직넘버(간단판)
    public static void requireAllowedMagic(MultipartFile file, String originalFilename) throws IOException {
        String ext = extOf(originalFilename);
        byte[] head = readHead(file, 8);

        if ("pdf".equals(ext)) {
            if (!(head.length >= 5 && head[0] == 0x25 && head[1] == 0x50 && head[2] == 0x44 && head[3] == 0x46 && head[4] == 0x2D)) {
                throw new IllegalArgumentException("PDF 시그니처가 아닙니다.");
            }
        } else if ("docx".equals(ext)) {
            if (!(head.length >= 2 && head[0] == 0x50 && head[1] == 0x4B)) {
                throw new IllegalArgumentException("DOCX(Zip) 시그니처가 아닙니다.");
            }
        } else if ("doc".equals(ext)) {
            if (!(head.length >= 4 && (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0)) {
                throw new IllegalArgumentException("DOC(OLE) 시그니처가 아닙니다.");
            }
        }
    }

    private static byte[] readHead(MultipartFile file, int n) throws IOException {
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[n];
            int r = in.read(buf);
            if (r <= 0) return new byte[0];
            if (r < n) {
                byte[] exact = new byte[r];
                System.arraycopy(buf, 0, exact, 0, r);
                return exact;
            }
            return buf;
        }
    }
}