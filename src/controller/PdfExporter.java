package controller;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;


import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.io.File;

public class PdfExporter {

	public static boolean exportarTabelaParaPdf(JTable tabela, File arquivo, String tituloRelatorio) {
	    try {
	        PdfWriter writer = new PdfWriter(arquivo.getAbsolutePath());
	        PdfDocument pdf = new PdfDocument(writer);
	        Document document = new Document(pdf);

	        TableModel model = tabela.getModel();

	        // Título centralizado
	        Paragraph titulo = new Paragraph("Relatório de Camas Vagas")
	                .setFontSize(16)
	                .setBold()
	                .setTextAlignment(TextAlignment.CENTER);
	        document.add(titulo);

	        // Espaço entre título e tabela
	        document.add(new Paragraph("\n"));

	        Table table = new Table(model.getColumnCount());

	        // Cabeçalhos
	        for (int col = 0; col < model.getColumnCount(); col++) {
	            table.addHeaderCell(new Cell().add(new Paragraph(model.getColumnName(col))));
	        }

	        // Dados
	        for (int row = 0; row < model.getRowCount(); row++) {
	            for (int col = 0; col < model.getColumnCount(); col++) {
	                Object valor = model.getValueAt(row, col);
	                table.addCell(new Cell().add(new Paragraph(valor != null ? valor.toString() : "")));
	            }
	        }

	        // Centraliza a tabela
	        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

	        document.add(table);

	        document.close();

	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public static boolean exportarTabelaParaPdfComResumoPaisagem(JTable tabela, File arquivo, String titulo, String resumo) {
	    try {
	        PdfWriter writer = new PdfWriter(arquivo.getAbsolutePath());
	        PdfDocument pdf = new PdfDocument(writer);
	        Document document = new Document(pdf, PageSize.A4.rotate());
	        document.setMargins(20, 20, 20, 20);

	        document.add(new Paragraph(titulo)
	            .setFontSize(16)
	            .setBold()
	            .setTextAlignment(TextAlignment.CENTER));

	        if (resumo != null && !resumo.isEmpty()) {
	            document.add(new Paragraph(resumo)
	                .setFontSize(12)
	                .setTextAlignment(TextAlignment.CENTER));
	        }

	        document.add(new Paragraph("\n"));

	        TableModel model = tabela.getModel();
	        Table table = new Table(model.getColumnCount());

	        for (int col = 0; col < model.getColumnCount(); col++) {
	            table.addHeaderCell(new Cell().add(new Paragraph(model.getColumnName(col))));
	        }
	        for (int row = 0; row < model.getRowCount(); row++) {
	            for (int col = 0; col < model.getColumnCount(); col++) {
	                Object valor = model.getValueAt(row, col);
	                table.addCell(new Cell().add(new Paragraph(valor != null ? valor.toString() : "")));
	            }
	        }

	        table.setHorizontalAlignment(HorizontalAlignment.CENTER);
	        document.add(table);

	        document.close();
	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}


}
