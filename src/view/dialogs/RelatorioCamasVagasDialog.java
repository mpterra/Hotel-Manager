package view.dialogs;

import db.DatabaseConnector;
import controller.ExcelExporter;
import controller.PdfExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RelatorioCamasVagasDialog extends JDialog {

    private JTable tabela;

    public RelatorioCamasVagasDialog(JFrame parent) {
        super(parent, "Relatório - Camas Vagas", true);
        setSize(600, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        String[] colunas = {"Quarto", "Descrição da Cama"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExcel = new JButton("Exportar para Excel");
        JButton btnPdf = new JButton("Exportar para PDF");
        painelBotoes.add(btnExcel);
        painelBotoes.add(btnPdf);
        add(painelBotoes, BorderLayout.SOUTH);

        btnExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPdf.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnExcel.addActionListener(e -> exportarParaExcel());
        btnPdf.addActionListener(e -> exportarParaPdf());

        carregarDados();
    }

    private void carregarDados() {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();
        modelo.setRowCount(0);

        String sql = "SELECT q.numero AS quarto_numero, c.descricao AS cama_descricao "
                   + "FROM cama c "
                   + "JOIN quarto q ON c.quarto_numero = q.numero "
                   + "WHERE c.status = 0";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String quarto = rs.getString("quarto_numero");
                String cama = rs.getString("cama_descricao");
                modelo.addRow(new Object[] { quarto, cama });
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void exportarParaExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como...");
        fileChooser.setSelectedFile(new File("relatorio_camas_vagas.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();

            if (!arquivo.getName().toLowerCase().endsWith(".xlsx")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".xlsx");
            }

            boolean sucesso = ExcelExporter.exportarTabelaParaExcel(tabela, arquivo);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Exportado com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar.");
            }
        }
    }

    private void exportarParaPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como...");
        fileChooser.setSelectedFile(new File("relatorio_camas_vagas.pdf"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();

            if (!arquivo.getName().toLowerCase().endsWith(".pdf")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".pdf");
            }

            boolean sucesso = PdfExporter.exportarTabelaParaPdf(tabela, arquivo, "Relatório de Camas Vagas");
;
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "PDF exportado com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar para PDF.");
            }
        }
    }

}
