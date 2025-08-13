package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import db.DatabaseConnector;
import view.dialogs.QuartoDialog;

public class InicioPanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JPanel contentPanel;
    private final JTextField searchField;
    private final List<QuartoInfo> todosQuartos = new ArrayList<>();
    private int colunas = 5;

    public InicioPanel() {
        setLayout(new BorderLayout());

        // Painel de busca no topo com espaçamento maior
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(new EmptyBorder(20, 10, 15, 10)); // top 20, bottom 15

        searchField = new JTextField();
        searchField.setToolTipText("Digite para filtrar quartos ou hóspedes...");
        searchPanel.add(new JLabel("Buscar:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        add(searchPanel, BorderLayout.NORTH);

        // Painel interno com GridLayout dinâmico
        contentPanel = new JPanel(new GridLayout(0, colunas, 10, 10));
        contentPanel.setBorder(new EmptyBorder(15, 10, 10, 10)); // top 15 para mais espaçamento

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Ajuste de colunas ao redimensionar
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarColunas();
            }
        });

        // Filtragem ao digitar
        searchField.addActionListener(e -> filtrarQuartos());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarQuartos(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarQuartos(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarQuartos(); }
        });

        carregarQuartosComHospedes();
    }

    private void carregarQuartosComHospedes() {
        todosQuartos.clear();
        contentPanel.removeAll();

        try (Connection conn = DatabaseConnector.conectar()) {
            String sqlQuartos = "SELECT numero FROM quarto ORDER BY numero";
            try (PreparedStatement ps = conn.prepareStatement(sqlQuartos);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int numeroQuarto = rs.getInt("numero");
                    QuartoInfo quartoInfo = new QuartoInfo(numeroQuarto);

                    carregarHospedesDoQuarto(quartoInfo, conn);
                    todosQuartos.add(quartoInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar quartos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }

        filtrarQuartos();
    }

    private void filtrarQuartos() {
        String termo = searchField.getText().trim().toLowerCase();
        contentPanel.removeAll();

        for (QuartoInfo quarto : todosQuartos) {
            boolean corresponde = termo.isEmpty()
                    || String.valueOf(quarto.numero).contains(termo)
                    || quarto.hospedes.stream().anyMatch(nome -> nome.toLowerCase().contains(termo));

            if (corresponde) {
                contentPanel.add(criarBotaoQuarto(quarto));
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void ajustarColunas() {
        int larguraPainel = getWidth();
        int larguraBotao = 144 + 10;
        int novasColunas = Math.max(1, larguraPainel / larguraBotao);

        if (novasColunas != colunas) {
            colunas = novasColunas;
            contentPanel.setLayout(new GridLayout(0, colunas, 10, 10));
            filtrarQuartos();
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

        JButton btn = new JButton("<html><div style='text-align:center;'>" + sb + "</div></html>");
        btn.setPreferredSize(new Dimension(144, 125));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (quartoInfo.dataDesocupacao != null) {
            LocalDate hoje = LocalDate.now();

            if (quartoInfo.dataDesocupacao.isBefore(hoje)) {
                btn.setBackground(Color.RED);
                btn.setForeground(Color.WHITE);
            } else if (!quartoInfo.dataDesocupacao.isAfter(hoje.plusDays(28))) {
                btn.setBackground(Color.YELLOW);
                btn.setForeground(Color.BLACK);
            } else {
                btn.setBackground(Color.BLUE);
                btn.setForeground(Color.WHITE);
            }

            btn.setOpaque(true);
            btn.setBorderPainted(false);
        }

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
