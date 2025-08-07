package view.dialogs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import db.DatabaseConnector;
import controller.ExcelExporter;
import controller.PdfExporter;

public class RelatorioHospedesHospedadosDialog extends JDialog {

    private JTable tabela;

    public RelatorioHospedesHospedadosDialog(JFrame parent) {
        super(parent, "Relatório - Hóspedes Hospedados", true);
        setSize(600, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        String[] colunas = {"Nome", "Documento", "Telefone", "Email"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // impede edição em qualquer célula
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

        String sql = "SELECT h.nome, h.documento, h.telefone, h.email " +
                     "FROM hospede h " +
                     "JOIN hospedagem hos ON hos.hospede_id = h.id " +
                     "WHERE hos.status = 1";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nome = rs.getString("nome");
                String documento = rs.getString("documento");
                String telefone = rs.getString("telefone");
                String email = rs.getString("email");
                modelo.addRow(new Object[]{nome, documento, telefone, email});
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void exportarParaExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como...");
        fileChooser.setSelectedFile(new File("relatorio_hospedes_hospedados.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();

            if (!arquivo.getName().toLowerCase().endsWith(".xlsx")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".xlsx");
            }

            boolean sucesso = ExcelExporter.exportarTabelaParaExcel(tabela, arquivo);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Exportado para Excel com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar para Excel.");
            }
        }
    }

    private void exportarParaPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como...");
        fileChooser.setSelectedFile(new File("relatorio_hospedes_hospedados.pdf"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();

            if (!arquivo.getName().toLowerCase().endsWith(".pdf")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".pdf");
            }

            boolean sucesso = PdfExporter.exportarTabelaParaPdf(tabela, arquivo, "Relatório - Hóspedes Hospedados");
;
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "PDF exportado com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar para PDF.");
            }
        }
    }

}
