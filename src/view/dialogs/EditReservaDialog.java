package view.dialogs;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.toedter.calendar.JDateChooser;

import db.DatabaseConnector;
import model.Cama;
import model.Hospede;

public class EditReservaDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int idReserva;

    private JTextField tfHospede;
    private JTextField tfCama;
    private JDateChooser dcEntrada;
    private JDateChooser dcSaida;

    private final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public EditReservaDialog(Window owner, int idReserva) {
        super(owner, "Visualizar Reserva", ModalityType.APPLICATION_MODAL);
        this.idReserva = idReserva;

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Hóspede - não editável
        add(new JLabel("Hóspede:"), gc);
        gc.gridx = 1;
        tfHospede = new JTextField(20);
        tfHospede.setEditable(false);
        add(tfHospede, gc);

        // Cama - não editável
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Cama:"), gc);
        gc.gridx = 1;
        tfCama = new JTextField(20);
        tfCama.setEditable(false);
        add(tfCama, gc);

        // Data Entrada - não editável
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Data Entrada:"), gc);
        gc.gridx = 1;
        dcEntrada = new JDateChooser();
        dcEntrada.setDateFormatString("dd/MM/yyyy");
        dcEntrada.setEnabled(false);
        add(dcEntrada, gc);

        // Data Saída - não editável
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Data Saída:"), gc);
        gc.gridx = 1;
        dcSaida = new JDateChooser();
        dcSaida.setDateFormatString("dd/MM/yyyy");
        dcSaida.setEnabled(false);
        add(dcSaida, gc);

        // Botões: Check In, Excluir e Sair
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCheckIn = new JButton("Check In");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnSair = new JButton("Sair");
        buttons.add(btnCheckIn);
        buttons.add(btnExcluir);
        buttons.add(btnSair);

        gc.gridx = 0; gc.gridy++;
        gc.gridwidth = 2;
        add(buttons, gc);

        btnCheckIn.addActionListener(e -> checkIn());
        btnCheckIn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExcluir.addActionListener(e -> excluir());
        btnExcluir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSair.addActionListener(e -> dispose());
        btnSair.setCursor(new Cursor(Cursor.HAND_CURSOR));

        carregarDados();

        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void carregarDados() {
        String sql = "SELECT h.nome AS hospede_nome, c.descricao AS cama_desc, data_entrada, data_saida " +
                     "FROM hospedagem res " +
                     "JOIN hospede h ON res.hospede_id = h.id " +
                     "JOIN cama c ON res.cama_id = c.id " +
                     "WHERE res.id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tfHospede.setText(rs.getString("hospede_nome"));
                    tfCama.setText(rs.getString("cama_desc"));
                    setChooserDate(dcEntrada, isoToLocalDate(rs.getString("data_entrada")));
                    setChooserDate(dcSaida, isoToLocalDate(rs.getString("data_saida")));
                } else {
                    JOptionPane.showMessageDialog(this, "Reserva não encontrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    dispose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados da reserva.", "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void checkIn() {
        String sql = "UPDATE hospedagem SET status = 1 WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Check-in realizado com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Reserva não encontrada para check-in.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao realizar check-in.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int op = JOptionPane.showConfirmDialog(this, "Confirma exclusão desta reserva?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        String sqlDel = "DELETE FROM hospedagem WHERE id = ?";
        String sqlDesocupa = "UPDATE cama SET status = 0 WHERE id = (SELECT cama_id FROM hospedagem WHERE id = ?)";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDesocupa = conn.prepareStatement(sqlDesocupa);
                 PreparedStatement psDel = conn.prepareStatement(sqlDel)) {

                psDesocupa.setInt(1, idReserva);
                psDesocupa.executeUpdate();

                psDel.setInt(1, idReserva);
                int deleted = psDel.executeUpdate();

                conn.commit();

                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this, "Reserva excluída.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Reserva não encontrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao excluir reserva.", "Erro", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static LocalDate isoToLocalDate(String iso) {
        try {
            return LocalDate.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private static void setChooserDate(JDateChooser dc, LocalDate ld) {
        if (ld == null) {
            dc.setDate(null);
            return;
        }
        Date date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        dc.setDate(date);
    }
}
