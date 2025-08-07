package view.dialogs;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import db.DatabaseConnector;

public class EditCheckOutDialog extends JDialog {

    public EditCheckOutDialog(Window parent, int hospedagemId) {
        super(parent, "Finalizar Check-out", ModalityType.APPLICATION_MODAL);
        setSize(400, 180);
        setLayout(null);

        String nomeHospede = carregarNomeHospede(hospedagemId);
        JLabel lblConfirm = new JLabel("Deseja finalizar o check-out de " + nomeHospede + "?");
        lblConfirm.setBounds(30, 30, 340, 25);
        add(lblConfirm);

        JButton btnSim = new JButton("Sim");
        btnSim.setBounds(60, 80, 100, 30);
        btnSim.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnSim);

        JButton btnNao = new JButton("Cancelar");
        btnNao.setBounds(180, 80, 120, 30);
        btnNao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnNao);

        btnSim.addActionListener(e -> {
            finalizarCheckOut(hospedagemId);
            dispose();
        });

        btnNao.addActionListener(e -> dispose());

        setLocationRelativeTo(null);  // Centraliza na tela
    }

    private String carregarNomeHospede(int hospedagemId) {
        String nome = "";
        String sql = "SELECT h.nome FROM hospede h " +
                     "JOIN hospedagem res ON h.id = res.hospede_id " +
                     "WHERE res.id = ?";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hospedagemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nome = rs.getString("nome");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        return nome.isEmpty() ? "hóspede" : nome;
    }

    private void finalizarCheckOut(int hospedagemId) {
        String sqlUpdateHospedagem = "UPDATE hospedagem SET status = 0, data_saida = ? WHERE id = ?";
        String sqlUpdateCama = "UPDATE cama SET status = 0 WHERE id = (SELECT cama_id FROM hospedagem WHERE id = ?)";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psHospedagem = conn.prepareStatement(sqlUpdateHospedagem);
                 PreparedStatement psCama = conn.prepareStatement(sqlUpdateCama)) {

                LocalDate hoje = LocalDate.now();
                psHospedagem.setString(1, hoje.toString());
                psHospedagem.setInt(2, hospedagemId);
                psHospedagem.executeUpdate();

                psCama.setInt(1, hospedagemId);
                psCama.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Check-out concluído.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Erro ao finalizar check-out.", "Erro", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
}
