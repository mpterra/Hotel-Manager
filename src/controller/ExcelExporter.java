package controller;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.io.FileOutputStream;
import java.io.File;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelExporter {

    public static boolean exportarTabelaParaExcel(JTable tabela, File arquivo) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Relatório");

            TableModel model = tabela.getModel();

            // Cabeçalhos
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < model.getColumnCount(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(model.getColumnName(col));
            }

            // Dados
            for (int row = 0; row < model.getRowCount(); row++) {
                Row dataRow = sheet.createRow(row + 1);
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object valor = model.getValueAt(row, col);
                    Cell cell = dataRow.createCell(col);
                    cell.setCellValue(valor != null ? valor.toString() : "");
                }
            }

            // Salvar arquivo
            try (FileOutputStream out = new FileOutputStream(arquivo)) {
                workbook.write(out);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
