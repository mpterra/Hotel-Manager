package view;

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
import model.Hospede;
import view.dialogs.EditReservaDialog; // <-- Import do diálogo
import view.dialogs.ProjecaoVagasDialog;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservaPanel extends JPanel {

    private JDateChooser dataEntradaChooser;
    private JDateChooser dataSaidaChooser;
    private JComboBox<Cama> comboCamas;
    private JComboBox<Hospede> comboHospedes;
    private JButton btnBuscarCamas;
    private JButton btnCriarReserva;

    private JTable tabelaReservas;
    private DefaultTableModel modeloTabela;

    private ProjecaoVagasDialog projecaoVagasDialog;

    public ReservaPanel() throws Exception {
        projecaoVagasDialog = new ProjecaoVagasDialog(null);
        setLayout(new BorderLayout());

        // Painel superior (formulário)
        JPanel topo = new JPanel(null);
        topo.setPreferredSize(new Dimension(0, 160));

        JLabel label = new JLabel("Data Entrada:");
        label.setBounds(22, 25, 80, 14);
        topo.add(label);

        dataEntradaChooser = new JDateChooser();
        dataEntradaChooser.setBounds(105, 22, 88, 20);
        dataEntradaChooser.setDateFormatString("dd/MM/yyyy");
        topo.add(dataEntradaChooser);
        applyAutoSlashFilter(dataEntradaChooser);

        JLabel label_1 = new JLabel("Data Saída:");
        label_1.setBounds(205, 25, 68, 14);
        topo.add(label_1);

        dataSaidaChooser = new JDateChooser();
        dataSaidaChooser.setBounds(275, 22, 88, 20);
        dataSaidaChooser.setDateFormatString("dd/MM/yyyy");
        topo.add(dataSaidaChooser);
        applyAutoSlashFilter(dataSaidaChooser);

        btnBuscarCamas = new JButton("Buscar Camas Disponíveis");
        btnBuscarCamas.setBounds(375, 21, 200, 23);
        btnBuscarCamas.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topo.add(btnBuscarCamas);

        comboCamas = new JComboBox<>();
        comboCamas.setBounds(22, 60, 300, 25);
        topo.add(comboCamas);

        comboHospedes = new JComboBox<>();
        comboHospedes.setBounds(22, 95, 300, 25);
        comboHospedes.setEditable(true);
        topo.add(comboHospedes);

        btnCriarReserva = new JButton("Criar Reserva");
        btnCriarReserva.setBounds(327, 95, 130, 23);
        btnCriarReserva.setEnabled(false);
        btnCriarReserva.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topo.add(btnCriarReserva);

        add(topo, BorderLayout.NORTH);

        // Tabela e scroll pane no centro para ocupar toda largura e altura restante
        tabelaReservas = new JTable();
        modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Hóspede", "E-mail", "Telefone", "Cama", "Entrada", "Saída"}, 0
        ) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaReservas.setModel(modeloTabela);

        JScrollPane scrollPane = new JScrollPane(tabelaReservas);
        add(scrollPane, BorderLayout.CENTER);

        // Adicionar listener para clique duplo na tabela
        tabelaReservas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && tabelaReservas.getSelectedRow() != -1) {
                    editarReservaSelecionada();
                }
            }
        });

        carregarHospedesSemHospedagemAtiva();
        carregarReservasFuturas();

        btnBuscarCamas.addActionListener(e -> {
            comboCamas.removeAllItems();
            btnCriarReserva.setEnabled(false);

            LocalDate entrada = getLocalDate(dataEntradaChooser);
            LocalDate saida = getLocalDate(dataSaidaChooser);

            if (entrada == null || saida == null) {
                JOptionPane.showMessageDialog(this, "Informe datas válidas.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                List<Cama> camas = projecaoVagasDialog.getCamasDisponiveis(entrada, saida);

                if (camas.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nenhuma cama disponível para o período informado.", "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Cama cama : camas) {
                        comboCamas.addItem(cama);
                    }
                    btnCriarReserva.setEnabled(true);
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCriarReserva.addActionListener(e -> {
            Cama camaSelecionada = (Cama) comboCamas.getSelectedItem();
            Hospede hospedeSelecionado = (Hospede) comboHospedes.getSelectedItem();

            if (camaSelecionada == null || hospedeSelecionado == null) {
                JOptionPane.showMessageDialog(this, "Selecione uma cama e um hóspede.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate entrada = getLocalDate(dataEntradaChooser);
            LocalDate saida = getLocalDate(dataSaidaChooser);

            if (entrada == null || saida == null || !saida.isAfter(entrada)) {
                JOptionPane.showMessageDialog(this, "Datas inválidas.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                criarReserva(hospedeSelecionado.getId(), camaSelecionada.getId(), entrada, saida);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }

    private void editarReservaSelecionada() {
        int linha = tabelaReservas.getSelectedRow();
        if (linha == -1) return;

        int linhaModelo = tabelaReservas.convertRowIndexToModel(linha);

        Object idObj = modeloTabela.getValueAt(linhaModelo, 0);
        if (idObj == null) return;

        int idReserva = (int) idObj;

        EditReservaDialog dialog = new EditReservaDialog(SwingUtilities.getWindowAncestor(this), idReserva);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        try {
            carregarReservasFuturas();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao recarregar reservas.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criarReserva(int hospedeId, int camaId, LocalDate entrada, LocalDate saida) throws Exception {
        String sql = "INSERT INTO hospedagem (hospede_id, cama_id, data_entrada, data_saida, status) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hospedeId);
            ps.setInt(2, camaId);
            ps.setString(3, entrada.toString());
            ps.setString(4, saida.toString());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Reserva criada com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                comboCamas.removeAllItems();
                btnCriarReserva.setEnabled(false);
                carregarHospedesSemHospedagemAtiva();
                carregarReservasFuturas();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao criar reserva.", "Erro", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao criar reserva: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarHospedesSemHospedagemAtiva() throws Exception {
        comboHospedes.removeAllItems();
        String sql = """
            SELECT * FROM hospede
            WHERE id NOT IN (
                SELECT hospede_id FROM hospedagem WHERE status = 1
            )
            ORDER BY nome
        """;
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Hospede h = new Hospede();
                h.setId(rs.getInt("id"));
                h.setNome(rs.getString("nome"));
                comboHospedes.addItem(h);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarReservasFuturas() throws Exception {
        modeloTabela.setRowCount(0);
        String sql = """
            SELECT h.id,
                   hos.nome AS hospede,
                   hos.email,
                   hos.telefone,
                   c.descricao AS cama,
                   h.data_entrada,
                   h.data_saida
            FROM hospedagem h
            JOIN hospede hos ON h.hospede_id = hos.id
            JOIN cama c ON h.cama_id = c.id
            WHERE h.status = 0 AND date(h.data_entrada) > date('now')
            ORDER BY h.data_entrada
        """;

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            while (rs.next()) {
                int id = rs.getInt("id");
                String hospede = rs.getString("hospede");
                String email = rs.getString("email");
                String telefone = rs.getString("telefone");
                String cama = rs.getString("cama");
                String entrada = LocalDate.parse(rs.getString("data_entrada")).format(formatter);
                String saida = LocalDate.parse(rs.getString("data_saida")).format(formatter);

                modeloTabela.addRow(new Object[]{id, hospede, email, telefone, cama, entrada, saida});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private LocalDate getLocalDate(JDateChooser chooser) {
        if (chooser.getDate() == null) return null;
        return chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
}
