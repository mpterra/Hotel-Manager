package view.dialogs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.toedter.calendar.JDateChooser;
import db.DatabaseConnector;
import controller.ExcelExporter;
import controller.PdfExporter;

public class RelatorioHistoricoHospedagensDialog extends JDialog {

    private JTable tabela;
    private JDateChooser txtDataInicio;
    private JDateChooser txtDataFim;
    private JComboBox<String> comboSexo;
    private JLabel lblTotal;
    private JTextField txtBuscaNome;
    private TableRowSorter<DefaultTableModel> sorter;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public RelatorioHistoricoHospedagensDialog(JFrame parent) {
        super(parent, "Relatório - Histórico de Hospedagens", true);
        setSize(800, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Painel filtros
        JPanel painelFiltros = new JPanel();
        painelFiltros.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        painelFiltros.add(new JLabel("Data Início:"));
        txtDataInicio = new JDateChooser();
        txtDataInicio.setDateFormatString("dd/MM/yyyy");
        painelFiltros.add(txtDataInicio);
        applyAutoSlashFilter(txtDataInicio);

        painelFiltros.add(new JLabel("Data Fim:"));
        txtDataFim = new JDateChooser();
        txtDataFim.setDateFormatString("dd/MM/yyyy");
        painelFiltros.add(txtDataFim);
        applyAutoSlashFilter(txtDataFim);

        painelFiltros.add(new JLabel("Sexo:"));
        comboSexo = new JComboBox<>(new String[]{"Todos", "M", "F"});
        painelFiltros.add(comboSexo);

        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnLimpar = new JButton("Limpar filtros");

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        painelBotoes.add(btnFiltrar);
        painelBotoes.add(btnLimpar);

        painelFiltros.add(Box.createHorizontalStrut(20));
        painelFiltros.add(painelBotoes);

        add(painelFiltros, BorderLayout.NORTH);

        lblTotal = new JLabel();
        atualizarLabelTotal(0, 0);

        String[] colunas = {"Nome", "Sexo", "Documento", "Telefone", "Email", "Data Entrada", "Data Saída"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabela = new JTable(modelo);

        // Painel total (label)
        JPanel painelTotal = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelTotal.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        painelTotal.add(lblTotal);

        // Painel busca (abaixo do total)
        JPanel painelBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelBusca.add(new JLabel("Buscar hóspede:"));
        txtBuscaNome = new JTextField(20);
        painelBusca.add(txtBuscaNome);

        // Configurar TableRowSorter para filtrar tabela
        sorter = new TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);

        // Listener para filtro dinâmico no campo busca
        txtBuscaNome.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                String texto = txtBuscaNome.getText();
                if (texto.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0)); // coluna "Nome"
                }
                atualizarLabelTotalPeloFiltro();
            }
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        // Montar painel centro com ordem: total, busca, tabela
        JPanel painelCentro = new JPanel();
        painelCentro.setLayout(new BoxLayout(painelCentro, BoxLayout.Y_AXIS));
        painelCentro.add(painelTotal);
        painelCentro.add(painelBusca);
        painelCentro.add(new JScrollPane(tabela));

        add(painelCentro, BorderLayout.CENTER);

        btnFiltrar.addActionListener(e -> carregarDados());
        btnLimpar.addActionListener(e -> {
            txtDataInicio.setDate(null);
            txtDataFim.setDate(null);
            comboSexo.setSelectedIndex(0);
            txtBuscaNome.setText("");
            carregarDados();
        });

        btnFiltrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Botões exportação Excel e PDF
        JPanel painelExportar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExcel = new JButton("Exportar para Excel");
        JButton btnPdf = new JButton("Exportar para PDF");
        btnExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPdf.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        painelExportar.add(btnExcel);
        painelExportar.add(btnPdf);
        add(painelExportar, BorderLayout.SOUTH);

        btnExcel.addActionListener(e -> exportarParaExcel());
        btnPdf.addActionListener(e -> exportarParaPdf());

        carregarDados();
    }

    private void carregarDados() {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();
        modelo.setRowCount(0);

        LocalDate dataInicio = null;
        LocalDate dataFim = null;
        String sexoSelecionado = (String) comboSexo.getSelectedItem();

        try {
            dataInicio = parseFromChooser(txtDataInicio);
        } catch (DateTimeParseException e) {
            dataInicio = null;
        }
        try {
            dataFim = parseFromChooser(txtDataFim);
        } catch (DateTimeParseException e) {
            dataFim = null;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT h.nome, h.sexo, h.documento, h.telefone, h.email, ");
        sql.append("hos.data_entrada, hos.data_saida ");
        sql.append("FROM hospede h ");
        sql.append("JOIN hospedagem hos ON hos.hospede_id = h.id ");
        sql.append("WHERE 1=1 ");

        if (dataInicio != null) {
            sql.append("AND hos.data_entrada >= ? ");
        }
        if (dataFim != null) {
            sql.append("AND hos.data_saida <= ? ");
        }
        if (!"Todos".equals(sexoSelecionado)) {
            sql.append("AND h.sexo = ? ");
        }
        sql.append("ORDER BY hos.id DESC");

        StringBuilder sqlTotais = new StringBuilder();
        sqlTotais.append("SELECT COUNT(*) AS totalHospedagens, ");
        sqlTotais.append("COALESCE(SUM(julianday(hos.data_saida) - julianday(hos.data_entrada)), 0) AS totalDiarias ");
        sqlTotais.append("FROM hospedagem hos ");
        sqlTotais.append("JOIN hospede h ON h.id = hos.hospede_id ");
        sqlTotais.append("WHERE 1=1 ");

        if (dataInicio != null) {
            sqlTotais.append("AND hos.data_entrada >= ? ");
        }
        if (dataFim != null) {
            sqlTotais.append("AND hos.data_saida <= ? ");
        }
        if (!"Todos".equals(sexoSelecionado)) {
            sqlTotais.append("AND h.sexo = ? ");
        }

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql.toString());
             PreparedStatement stmtTotais = conn.prepareStatement(sqlTotais.toString())) {

            int paramIndex = 1;
            if (dataInicio != null) {
                stmt.setString(paramIndex, ISO.format(dataInicio));
                stmtTotais.setString(paramIndex, ISO.format(dataInicio));
                paramIndex++;
            }
            if (dataFim != null) {
                stmt.setString(paramIndex, ISO.format(dataFim));
                stmtTotais.setString(paramIndex, ISO.format(dataFim));
                paramIndex++;
            }
            if (!"Todos".equals(sexoSelecionado)) {
                stmt.setString(paramIndex, sexoSelecionado);
                stmtTotais.setString(paramIndex, sexoSelecionado);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getString("nome"),
                            rs.getString("sexo"),
                            rs.getString("documento"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            formatarDataSegura(rs.getString("data_entrada")),
                            formatarDataSegura(rs.getString("data_saida"))
                    });
                }
            }

            try (ResultSet rsTotais = stmtTotais.executeQuery()) {
                if (rsTotais.next()) {
                    int totalHospedagens = rsTotais.getInt("totalHospedagens");
                    int totalDiarias = rsTotais.getInt("totalDiarias");
                    atualizarLabelTotal(totalHospedagens, totalDiarias);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + e.getMessage());
        }

        // Atualiza label total considerando filtro do campo busca (vazio no carregamento)
        atualizarLabelTotalPeloFiltro();
    }

    private void atualizarLabelTotal(int totalHospedagens, int totalDiarias) {
        lblTotal.setText(String.format(
                "<html><b style='font-size:14px;'>Total de hospedagens: %d&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Total de diárias ofertadas: %d</b></html>",
                totalHospedagens, totalDiarias));
    }

    private void atualizarLabelTotalPeloFiltro() {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();

        int totalHospedagensFiltradas = tabela.getRowCount(); // linhas visíveis após filtro

        int totalDiarias = 0;
        for (int i = 0; i < totalHospedagensFiltradas; i++) {
            int modelIndex = tabela.convertRowIndexToModel(i);

            String dataEntradaStr = (String) modelo.getValueAt(modelIndex, 5); // coluna Data Entrada
            String dataSaidaStr = (String) modelo.getValueAt(modelIndex, 6);   // coluna Data Saída

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date dataEntrada = sdf.parse(dataEntradaStr);
                Date dataSaida = sdf.parse(dataSaidaStr);
                long diffMillis = dataSaida.getTime() - dataEntrada.getTime();
                int diffDias = (int) (diffMillis / (1000 * 60 * 60 * 24));
                if (diffDias > 0) {
                    totalDiarias += diffDias;
                }
            } catch (Exception e) {
                // Ignorar erro de parse
            }
        }

        atualizarLabelTotal(totalHospedagensFiltradas, totalDiarias);
    }

    private LocalDate parseFromChooser(JDateChooser chooser) throws DateTimeParseException {
        Date data = chooser.getDate();
        if (data == null) throw new DateTimeParseException("Data nula", "", 0);
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void applyAutoSlashFilter(JDateChooser chooser) {
        JTextComponent editor = (JTextComponent) chooser.getDateEditor().getUiComponent();
        AbstractDocument doc = (AbstractDocument) editor.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            private boolean isDeleting = false;

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                isDeleting = true;
                super.remove(fb, offset, length);
                isDeleting = false;
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if (isDeleting) {
                    super.insertString(fb, offset, string, attr);
                    return;
                }
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, string);

                String text = sb.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                int len = text.length();
                for (int i = 0; i < len; i++) {
                    formatted.append(text.charAt(i));
                    if (i == 1 || i == 3) formatted.append("/");
                }
                if (formatted.length() > 10) formatted.setLength(10);

                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                if (isDeleting) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text);

                String onlyDigits = sb.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                int len = onlyDigits.length();
                for (int i = 0; i < len; i++) {
                    formatted.append(onlyDigits.charAt(i));
                    if (i == 1 || i == 3) formatted.append("/");
                }
                if (formatted.length() > 10) formatted.setLength(10);

                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attrs);
            }
        });
    }

    private String formatarDataSegura(String dataString) {
        if (dataString == null || dataString.isEmpty()) return "";
        try {
            Date data = new SimpleDateFormat("yyyy-MM-dd").parse(dataString);
            return new SimpleDateFormat("dd/MM/yyyy").format(data);
        } catch (Exception ex) {
            return "";
        }
    }

    private void exportarParaExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar relatório como...");
        fileChooser.setSelectedFile(new File("relatorio_historico_hospedagens.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();
            if (!arquivo.getName().toLowerCase().endsWith(".xlsx")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".xlsx");
            }
            boolean sucesso = ExcelExporter.exportarTabelaParaExcel(tabela, arquivo);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar relatório.");
            }
        }
    }

    private void exportarParaPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar relatório como...");
        fileChooser.setSelectedFile(new File("relatorio_historico_hospedagens.pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();
            if (!arquivo.getName().toLowerCase().endsWith(".pdf")) {
                arquivo = new File(arquivo.getAbsolutePath() + ".pdf");
            }
            String titulo = "Relatório - Histórico de Hospedagens";
            String resumo = lblTotal.getText()
                    .replaceAll("<[^>]*>", "")
                    .replaceAll("&nbsp;", " ")
                    .trim();

            boolean sucesso = PdfExporter.exportarTabelaParaPdfComResumoPaisagem(tabela, arquivo, titulo, resumo);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Relatório PDF exportado com sucesso!");
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao exportar PDF.");
            }
        }
    }

}
