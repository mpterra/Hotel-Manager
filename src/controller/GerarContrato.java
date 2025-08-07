package controller;

import org.apache.poi.xwpf.usermodel.*;
import javax.swing.*;
import java.awt.Component;
import java.io.*;
import java.util.Map;

public class GerarContrato {

    // Recebe o componente pai para o diálogo
    public static void gerarComDialogo(Component parent, String caminhoModelo, Map<String, String> dados) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Contrato");
        String id = dados.getOrDefault("{{reserva_id}}", "0");
        String ano = dados.getOrDefault("{{ano}}", "0000");
        String nome = dados.getOrDefault("{{nome}}", "nome").replaceAll("[^a-zA-Z0-9]", "_"); // limpa caracteres problemáticos

        String nomeArquivo = String.format("Termo %s %s %s.docx", id, ano, nome);
        fileChooser.setSelectedFile(new File(nomeArquivo));

        // Passa o parent no lugar do null
        int userSelection = fileChooser.showSaveDialog(parent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();

            try (FileInputStream fis = new FileInputStream(caminhoModelo);
                 XWPFDocument document = new XWPFDocument(fis)) {

                for (XWPFParagraph p : document.getParagraphs()) {
                    substituirTexto(p, dados);
                    aplicarFonte(p); // aplica Lato 12
                }

                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                substituirTexto(p, dados);
                                aplicarFonte(p); // aplica Lato 12
                            }
                        }
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
                    document.write(fos);
                }

                JOptionPane.showMessageDialog(parent, "Contrato salvo com sucesso em:\n" + arquivoDestino.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Erro ao gerar contrato:\n" + e.getMessage());
            }
        } else {
            System.out.println("Usuário cancelou o salvamento.");
        }
    }

    private static void substituirTexto(XWPFParagraph paragraph, Map<String, String> dados) {
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String texto = run.getText(0);
            if (texto != null) {
                paragraphText.append(texto);
            }
        }

        String textoCompleto = paragraphText.toString();
        String textoModificado = textoCompleto;

        for (Map.Entry<String, String> entry : dados.entrySet()) {
            textoModificado = textoModificado.replace(entry.getKey(), entry.getValue());
        }

        if (!textoCompleto.equals(textoModificado)) {
            int runsCount = paragraph.getRuns().size();
            for (int i = runsCount - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            XWPFRun novoRun = paragraph.createRun();
            novoRun.setText(textoModificado);
        }
    }

    private static void aplicarFonte(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            run.setFontFamily("Lato");
            run.setFontSize(12);
        }
    }
}
