package com.yupi.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yupi.yuaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        // 生成到 static/files 目录，方便用户通过 URL 下载
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        // 确保文件名以 .pdf 结尾
        if (!fileName.endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
                document.setFont(font);
                // 按换行符分段，每段独立添加
                String[] lines = content.split("\\n");
                for (String line : lines) {
                    document.add(new Paragraph(line.isEmpty() ? " " : line));
                }
            }
            // 返回可下载的相对路径提示
            return "PDF generated successfully. File saved at: " + filePath
                    + ". 用户可通过服务器文件路径访问该文件。";
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
