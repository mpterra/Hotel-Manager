package view;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import db.DatabaseConnector;
import view.dialogs.QuartoDialog;

public class InicioPanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public InicioPanel() {
    	setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        carregarQuartosComHospedes();
    }

    private void carregarQuartosComHospedes() {
        try (Connection conn = DatabaseConnector.conectar()) {
            String sqlQuartos = "SELECT numero FROM quarto ORDER BY numero";
            try (PreparedStatement ps = conn.prepareStatement(sqlQuartos);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int numeroQuarto = rs.getInt("numero");
                    QuartoInfo quartoInfo = new QuartoInfo(numeroQuarto);

                    carregarHospedesDoQuarto(quartoInfo, conn);

                    JButton btn = criarBotaoQuarto(quartoInfo);
                    add(btn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar quartos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton criarBotaoQuarto(QuartoInfo quartoInfo) {
        StringBuilder sb = new StringBuilder();

        sb.append("<b>Quarto ").append(quartoInfo.numero).append("</b><br><br>");
        if (quartoInfo.hospedes.isEmpty()) {
            sb.append("Vazio");
        } else {
            for (String nome : quartoInfo.hospedes) {
                sb.append(nome).append("<br>");
            }
            if (quartoInfo.dataDesocupacao != null) {
                sb.append("<br>Desocupa em ").append(quartoInfo.dataDesocupacao.format(DISPLAY_DATE_FORMATTER));
            }
        }

        JButton btn = new JButton("<html><div style='text-align:center;'>" + sb.toString() + "</div></html>");
        btn.setPreferredSize(new Dimension(144, 125));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 🔴 Se a data de desocupação for passada, pinta o botão de vermelho
        if (quartoInfo.dataDesocupacao != null) {
            LocalDate hoje = LocalDate.now();

            if (quartoInfo.dataDesocupacao.isBefore(hoje)) {
                // Atrasado → Vermelho
                btn.setBackground(Color.RED);
                btn.setForeground(Color.WHITE);
            } else if (!quartoInfo.dataDesocupacao.isAfter(hoje.plusDays(28))) {
                // Até 28 dias → Amarelo
                btn.setBackground(Color.YELLOW);
                btn.setForeground(Color.BLACK);
            } else {
                // Mais de 28 dias → Azul
                btn.setBackground(Color.BLUE);
                btn.setForeground(Color.WHITE);
            }

            btn.setOpaque(true);
            btn.setBorderPainted(false);
        }


        // Ação ao clicar no botão
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new QuartoDialog(SwingUtilities.getWindowAncestor(InicioPanel.this), quartoInfo.numero).setVisible(true);
            }
        });

        return btn;
    }


    private void carregarHospedesDoQuarto(QuartoInfo quartoInfo, Connection conn) throws SQLException {
        String sql = "SELECT h.nome, res.data_saida " +
                "FROM hospedagem res " +
                "JOIN cama c ON res.cama_id = c.id " +
                "JOIN hospede h ON res.hospede_id = h.id " +
                "WHERE c.quarto_numero = ? AND res.status = 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quartoInfo.numero);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> nomes = new ArrayList<>();
                LocalDate dataMaisProxima = null;

                while (rs.next()) {
                    nomes.add(rs.getString("nome"));

                    String dataSaidaStr = rs.getString("data_saida");
                    if (dataSaidaStr != null && !dataSaidaStr.isEmpty()) {
                        LocalDate data = LocalDate.parse(dataSaidaStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        if (dataMaisProxima == null || data.isBefore(dataMaisProxima)) {
                            dataMaisProxima = data;
                        }
                    }
                }

                quartoInfo.hospedes = nomes;
                quartoInfo.dataDesocupacao = dataMaisProxima;
            }
        }
    }

    private static class QuartoInfo {
        int numero;
        List<String> hospedes = new ArrayList<>();
        LocalDate dataDesocupacao;

        QuartoInfo(int numero) {
            this.numero = numero;
        }
    }
}
