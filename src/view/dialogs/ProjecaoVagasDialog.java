package view.dialogs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

import com.toedter.calendar.JDateChooser;
import db.DatabaseConnector;
import model.Cama;

import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProjecaoVagasDialog extends JDialog {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FRIENDLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JDateChooser txtDataInicio;
    private JDateChooser txtDataFim;
    private JTable tabela;

    public ProjecaoVagasDialog(JFrame parent) {
        super(parent, "Projeção de Vagas - Camas Disponíveis no Período", true);
        setSize(700, 450);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
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

        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        painelFiltros.add(btnFiltrar);

        add(painelFiltros, BorderLayout.NORTH);

        String[] colunas = {"Descrição da Cama", "Quarto"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // impede edição em qualquer célula
            }
        };
        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        btnFiltrar.addActionListener(e -> carregarCamasDisponiveisNoPeriodo());
    }

    private void carregarCamasDisponiveisNoPeriodo() {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();
        modelo.setRowCount(0);

        LocalDate dataInicio;
        LocalDate dataFim;

        try {
            dataInicio = parseFromChooser(txtDataInicio);
            dataFim = parseFromChooser(txtDataFim);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Data inválida. Use o formato dd/MM/yyyy.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dataFim.isBefore(dataInicio)) {
            JOptionPane.showMessageDialog(this, "Data fim não pode ser anterior à data início.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql =
                "SELECT c.id, c.descricao, c.quarto_numero " +
                "FROM cama c " +
                "WHERE NOT EXISTS ( " +
                "  SELECT 1 FROM hospedagem h " +
                "  WHERE h.cama_id = c.id " +
                "    AND NOT (h.data_saida < ? OR h.data_entrada > ?) " +
                ") " +
                "ORDER BY c.descricao";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

        	ps.setString(1, ISO.format(dataInicio));
        	ps.setString(2, ISO.format(dataFim));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[] {
                        rs.getString("descricao"),
                        rs.getInt("quarto_numero")
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar camas disponíveis: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parseFromChooser(JDateChooser chooser) throws DateTimeParseException {
        Date data = chooser.getDate();
        if (data == null) throw new DateTimeParseException("Data nula", "", 0);
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private JTextComponent editor(JDateChooser chooser) {
        return (JTextComponent) chooser.getDateEditor().getUiComponent();
    }

    private void applyAutoSlashFilter(JDateChooser chooser) {
        JTextComponent editor = editor(chooser);
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
    
    public List<Cama> getCamasDisponiveis(LocalDate dataInicio, LocalDate dataFim) throws Exception {
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("Data fim não pode ser anterior à data início.");
        }

        List<Cama> lista = new ArrayList<>();

        String sql =
                "SELECT c.id, c.descricao, c.quarto_numero " +
                "FROM cama c " +
                "WHERE NOT EXISTS ( " +
                "  SELECT 1 FROM hospedagem h " +
                "  WHERE h.cama_id = c.id " +
                "    AND NOT (h.data_saida < ? OR h.data_entrada > ?) " +
                ") " +
                "ORDER BY c.descricao";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Corrigido aqui:
            ps.setString(1, ISO.format(dataInicio));
            ps.setString(2, ISO.format(dataFim));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String descricao = rs.getString("descricao");
                    int quarto = rs.getInt("quarto_numero");
                    lista.add(new Cama(id, descricao));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao carregar camas disponíveis: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }

        return lista;
    }

}
