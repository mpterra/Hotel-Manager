package view.dialogs;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.toedter.calendar.JDateChooser;

import db.DatabaseConnector;

public class EditCheckInDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int idCheckIn;

    private JComboBox<Item> comboHospede;
    private JComboBox<Item> comboCama;
    private JDateChooser dcEntrada;
    private JDateChooser dcSaida;

    private final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter FRIENDLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EditCheckInDialog(Window owner, int idCheckIn) {
        super(owner, "Editar Check-in", ModalityType.APPLICATION_MODAL);
        this.idCheckIn = idCheckIn;

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Hóspede
        add(new JLabel("Hóspede:"), gc);
        gc.gridx = 1;
        comboHospede = new JComboBox<>();
        add(comboHospede, gc);

        // Cama
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Cama:"), gc);
        gc.gridx = 1;
        comboCama = new JComboBox<>();
        add(comboCama, gc);

        // Data Entrada
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Entrada:"), gc);
        gc.gridx = 1;
        dcEntrada = new JDateChooser();
        dcEntrada.setDateFormatString("dd/MM/yyyy");
        add(dcEntrada, gc);

        // Data Saída
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Saída:"), gc);
        gc.gridx = 1;
        dcSaida = new JDateChooser();
        dcSaida.setDateFormatString("dd/MM/yyyy");
        add(dcSaida, gc);

        // Aplica filtro para autocompletar as barras nos dois campos de data
        applyAutoSlashFilter(dcEntrada);
        applyAutoSlashFilter(dcSaida);

        // Botões
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnCancelar = new JButton("Cancelar");
        buttons.add(btnSalvar);
        buttons.add(btnExcluir);
        buttons.add(btnCancelar);

        gc.gridx = 0; gc.gridy++;
        gc.gridwidth = 2;
        add(buttons, gc);

        // Ações
        btnSalvar.addActionListener(e -> salvar());
        btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExcluir.addActionListener(e -> excluir());
        btnExcluir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        preencherCombos();
        carregarDados();

        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setResizable(false);
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

    // --- Restante do código permanece igual ---
    private void preencherCombos() {
        comboHospede.removeAllItems();
        comboCama.removeAllItems();

        // Carregar hóspedes
        String sqlHosp = "SELECT id, nome FROM hospede ORDER BY nome";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sqlHosp);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comboHospede.addItem(new Item(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Carregar camas disponíveis + a cama do check-in atual (para permitir não perder a seleção)
        // Primeiro, carregar cama atual do check-in
        int camaAtualId = -1;
        String sqlCamaAtual = "SELECT cama_id FROM hospedagem WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sqlCamaAtual)) {
            ps.setInt(1, idCheckIn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) camaAtualId = rs.getInt("cama_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Carregar camas disponíveis (status = 0) ou a cama atual mesmo que esteja ocupada (status=1)
        String sqlCama = "SELECT id, descricao FROM cama WHERE status = 0 OR id = ? ORDER BY descricao";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sqlCama)) {
            ps.setInt(1, camaAtualId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comboCama.addItem(new Item(rs.getInt("id"), rs.getString("descricao")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarDados() {
        String sql = "SELECT hospede_id, cama_id, data_entrada, data_saida FROM hospedagem WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCheckIn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int hospedeId = rs.getInt("hospede_id");
                    int camaId = rs.getInt("cama_id");
                    String entradaISO = rs.getString("data_entrada");
                    String saidaISO = rs.getString("data_saida");

                    setComboSelection(comboHospede, hospedeId);
                    setComboSelection(comboCama, camaId);

                    setChooserDate(dcEntrada, isoToLocalDate(entradaISO));
                    setChooserDate(dcSaida, isoToLocalDate(saidaISO));
                } else {
                    JOptionPane.showMessageDialog(this, "Registro de hospedagem não encontrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    dispose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void salvar() {
        Item hospede = (Item) comboHospede.getSelectedItem();
        Item cama = (Item) comboCama.getSelectedItem();

        if (hospede == null) {
            JOptionPane.showMessageDialog(this, "Selecione um hóspede.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cama == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma cama.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate entrada;
        LocalDate saida;
        try {
            entrada = getChooserDate(dcEntrada);
            saida = getChooserDate(dcSaida);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Datas inválidas.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (saida.isBefore(entrada)) {
            JOptionPane.showMessageDialog(this, "Data de saída não pode ser anterior à entrada.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Atualiza hospedagem e status da cama dentro de transação
        String sqlUpdHosp = "UPDATE hospedagem SET hospede_id = ?, cama_id = ?, data_entrada = ?, data_saida = ? WHERE id = ?";
        String sqlDesocupaAntiga = "UPDATE cama SET status = 0 WHERE id = (SELECT cama_id FROM hospedagem WHERE id = ?)";
        String sqlOcupaNova = "UPDATE cama SET status = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psDesocupa = conn.prepareStatement(sqlDesocupaAntiga);
                 PreparedStatement psUpdHosp = conn.prepareStatement(sqlUpdHosp);
                 PreparedStatement psOcupa = conn.prepareStatement(sqlOcupaNova)) {

                // Desocupa cama antiga
                psDesocupa.setInt(1, idCheckIn);
                psDesocupa.executeUpdate();

                // Atualiza hospedagem
                psUpdHosp.setInt(1, hospede.id);
                psUpdHosp.setInt(2, cama.id);
                psUpdHosp.setString(3, ISO.format(entrada));
                psUpdHosp.setString(4, ISO.format(saida));
                psUpdHosp.setInt(5, idCheckIn);
                psUpdHosp.executeUpdate();

                // Ocupa nova cama
                psOcupa.setInt(1, cama.id);
                psOcupa.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Check-in atualizado com sucesso.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao salvar.", "Erro", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int op = JOptionPane.showConfirmDialog(this, "Confirma exclusão deste check-in?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        String sqlDelHosp = "DELETE FROM hospedagem WHERE id = ?";
        String sqlDesocupaCama = "UPDATE cama SET status = 0 WHERE id = (SELECT cama_id FROM hospedagem WHERE id = ?)";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psDesocupa = conn.prepareStatement(sqlDesocupaCama);
                 PreparedStatement psDelHosp = conn.prepareStatement(sqlDelHosp)) {

                psDesocupa.setInt(1, idCheckIn);
                psDesocupa.executeUpdate();

                psDelHosp.setInt(1, idCheckIn);
                int deleted = psDelHosp.executeUpdate();

                conn.commit();
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this, "Check-in excluído.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Registro não encontrado para exclusão.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao excluir.", "Erro", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== Helpers ======================

    /** Item combo genérico (id + label). */
    private static class Item {
        final int id;
        final String label;
        Item(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    /** Seleciona item no combo pelo id. */
    private static void setComboSelection(JComboBox<Item> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Converte String ISO para LocalDate, com fallback null. */
    private static LocalDate isoToLocalDate(String iso) {
        try {
            return LocalDate.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    /** Configura data no JDateChooser */
    private static void setChooserDate(JDateChooser dc, LocalDate ld) {
        if (ld == null) {
            dc.setDate(null);
            return;
        }
        Date date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dc.setDate(date);
    }

    /** Lê data do JDateChooser (lança DateTimeParseException se inválida) */
    private static LocalDate getChooserDate(JDateChooser dc) throws DateTimeParseException {
        Date d = dc.getDate();
        if (d == null) throw new DateTimeParseException("Data nula", "", 0);
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
