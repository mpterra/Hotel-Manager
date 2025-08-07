package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import db.DatabaseConnector;
import view.dialogs.EditCheckInDialog;

import java.awt.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.toedter.calendar.JDateChooser;

import controller.GerarContrato;

public class CheckInPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // Combo editável com busca (substitui campo de texto + botão)
    private JComboBox<Item> comboHospede;
    private JComboBox<Item> comboCama;

    private JDateChooser dcEntrada;
    private JDateChooser dcSaida;

    private JTable table;
    private JTextField txtFiltroCheckins; // filtro da tabela
    private TableRowSorter<DefaultTableModel> sorter;

    // Controle do loop do auto-complete
    private boolean internalUpdate = false;
    private String lastQuery = "";

    private final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter FRIENDLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Runnable onAfterCheckIn;

    public CheckInPanel() { this(null); }

    public CheckInPanel(Runnable onAfterCheckIn) {
        this.onAfterCheckIn = onAfterCheckIn;
        setLayout(null);

        // --- Hóspede (combo editável com busca) ---
        JLabel lblHospede = new JLabel("Hóspede:");
        lblHospede.setBounds(226, 35, 80, 25);
        add(lblHospede);

        comboHospede = new JComboBox<>();
        comboHospede.setBounds(316, 35, 257, 25);
        comboHospede.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        comboHospede.setEditable(true); // permite digitar para buscar
        add(comboHospede);

        configurarAutoCompleteHospede();
        atualizarSugestoesHospede(""); // carrega só hóspedes SEM hospedagem ativa

        // --- Cama ---
        JLabel lblCama = new JLabel("Cama:");
        lblCama.setBounds(226, 70, 80, 25);
        add(lblCama);

        comboCama = new JComboBox<>();
        comboCama.setBounds(316, 70, 257, 25);
        comboCama.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(comboCama);

        // --- Datas ---
        JLabel lblEntrada = new JLabel("Entrada (dd/mm/aaaa):");
        lblEntrada.setBounds(226, 105, 160, 25);
        add(lblEntrada);

        dcEntrada = new JDateChooser();
        dcEntrada.setDateFormatString("dd/MM/yyyy");
        dcEntrada.setBounds(386, 105, 187, 25);
        add(dcEntrada);
        applyAutoSlashFilter(dcEntrada);

        JLabel lblSaida = new JLabel("Saída (dd/mm/aaaa):");
        lblSaida.setBounds(226, 140, 160, 25);
        add(lblSaida);

        dcSaida = new JDateChooser();
        dcSaida.setDateFormatString("dd/MM/yyyy");
        dcSaida.setBounds(386, 140, 187, 25);
        add(dcSaida);
        applyAutoSlashFilter(dcSaida);

        JButton btnCheckIn = new JButton("Check-in");
        btnCheckIn.setBounds(453, 175, 120, 30);
        btnCheckIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btnCheckIn);

        // ---- Filtro acima da tabela ----
        JLabel lblFiltro = new JLabel("Buscar Check-ins (Hóspede):");
        lblFiltro.setBounds(34, 251, 200, 25);
        add(lblFiltro);

        txtFiltroCheckins = new JTextField();
        txtFiltroCheckins.setBounds(205, 251, 280, 25);
        add(txtFiltroCheckins);
        // ---------------------------------

        // --- Tabela ---
        String[] colunas = { "ID", "Hóspede", "Email", "Telefone", "Cama", "Entrada", "Saída" };
        DefaultTableModel modelo = new DefaultTableModel(new Object[][] {}, colunas) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(modelo);
        
        

        sorter = new TableRowSorter<>(modelo);
        table.setRowSorter(sorter);

        // filtro em tempo real na tabela (coluna 1 = Hóspede)
        txtFiltroCheckins.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                String t = txtFiltroCheckins.getText();
                if (t == null || t.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(t.trim()), 1));
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(34, 287, 715, 193);
        add(scrollPane);

        // Ocultar a coluna ID (coluna 0)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Ajustar largura das colunas Email e Telefone
        table.getColumnModel().getColumn(1).setPreferredWidth(165); // Nome
        table.getColumnModel().getColumn(2).setPreferredWidth(160); // Email
        table.getColumnModel().getColumn(3).setPreferredWidth(85); // Telefone

        // --- Inicializações ---
        preencherDatasPadrao();
        carregarCamasDisponiveis();
        atualizarTabela();

        // Ajustar saída ao mudar entrada
        dcEntrada.getDateEditor().addPropertyChangeListener(evt -> {
            if ("date".equals(evt.getPropertyName())) ajustarSaidaPadrao30dias();
        });

        btnCheckIn.addActionListener(e -> {
			try {
				realizarCheckInAcao();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

        // Duplo clique para editar
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = table.rowAtPoint(evt.getPoint());
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        Object idObj = table.getModel().getValueAt(modelRow, 0);
                        if (idObj instanceof Integer) {
                            int idCheckIn = (Integer) idObj;
                            EditCheckInDialog dialog = new EditCheckInDialog(SwingUtilities.getWindowAncestor(CheckInPanel.this), idCheckIn);
                            dialog.setLocationRelativeTo(CheckInPanel.this);
                            dialog.setVisible(true);
                            atualizarTabela();
                            // Atualiza sugestões do combo após possíveis mudanças
                            lastQuery = "";
                            atualizarSugestoesHospede(((JTextComponent) comboHospede.getEditor().getEditorComponent()).getText().trim());
                        }
                    }
                }
            }
        });
    }

    /* ===== Modelo de item ===== */
    private static class Item {
        final int id; final String label;
        Item(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    /* ===== Autocomplete do hóspede no JComboBox ===== */
    private void configurarAutoCompleteHospede() {
        final JTextComponent editor = (JTextComponent) comboHospede.getEditor().getEditorComponent();

        // Debounce para aliviar consultas enquanto digita
        final int DELAY = 250;
        final javax.swing.Timer[] timerRef = new javax.swing.Timer[1];

        DocumentListener dl = new DocumentListener() {
            private void reagendar() {
                if (internalUpdate) return; // ignora eventos causados por nós
                if (timerRef[0] != null && timerRef[0].isRunning()) timerRef[0].stop();
                timerRef[0] = new javax.swing.Timer(DELAY, ev -> {
                    String termo = editor.getText().trim();
                    atualizarSugestoesHospede(termo);
                });
                timerRef[0].setRepeats(false);
                timerRef[0].start();
            }
            @Override public void insertUpdate(DocumentEvent e) { reagendar(); }
            @Override public void removeUpdate(DocumentEvent e)  { reagendar(); }
            @Override public void changedUpdate(DocumentEvent e) { reagendar(); }
        };
        editor.getDocument().addDocumentListener(dl);

        // ENTER escolhe a primeira sugestão se nada estiver selecionado
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (comboHospede.getSelectedIndex() < 0 && comboHospede.getItemCount() > 0) {
                        internalUpdate = true;
                        try {
                            comboHospede.setSelectedIndex(0);
                            comboHospede.setPopupVisible(false);
                            Item it = (Item) comboHospede.getSelectedItem();
                            if (it != null) editor.setText(it.label);
                            lastQuery = editor.getText().trim();
                        } finally {
                            internalUpdate = false;
                        }
                    }
                }
            }
        });

        // Ao fechar o popup (seleção), fixa o texto sem disparar nova busca
        comboHospede.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (internalUpdate) return;
                internalUpdate = true;
                try {
                    Object sel = comboHospede.getSelectedItem();
                    if (sel instanceof Item) {
                        ((JTextComponent) comboHospede.getEditor().getEditorComponent())
                                .setText(((Item) sel).label);
                        lastQuery = ((Item) sel).label.trim();
                    }
                } finally {
                    internalUpdate = false;
                }
            }
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    }

    private void atualizarSugestoesHospede(String termo) {
        if (termo == null) termo = "";
        // Evita reconsultar quando nada mudou
        if (termo.equals(lastQuery) && comboHospede.getItemCount() > 0) {
            if (!termo.isEmpty()
                && !comboHospede.isPopupVisible()
                && ((JTextComponent) comboHospede.getEditor().getEditorComponent()).isFocusOwner()) {
                comboHospede.setPopupVisible(true);
            }
            return;
        }

        DefaultComboBoxModel<Item> model = new DefaultComboBoxModel<>();
        String like = "%" + termo + "%";

        // **Somente hóspedes sem hospedagem ativa (status = 1)**
        String sql =
            "SELECT ho.id, ho.nome " +
            "FROM hospede ho " +
            "WHERE ho.nome LIKE ? " +
            "  AND NOT EXISTS ( " +
            "      SELECT 1 FROM hospedagem h " +
            "       WHERE h.hospede_id = ho.id AND h.status = 1 " +
            "  ) " +
            "ORDER BY ho.id DESC, ho.nome ASC";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addElement(new Item(rs.getInt("id"), rs.getString("nome")));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JTextComponent editor = (JTextComponent) comboHospede.getEditor().getEditorComponent();
        String typed = editor.getText(); // preserva o que foi digitado

        internalUpdate = true;
        try {
            comboHospede.setModel(model);
            comboHospede.setSelectedItem(null); // não selecionar automaticamente
            editor.setText(typed);              // restaura o texto
            lastQuery = termo;

            // Abre popup se houver resultados e o usuário está digitando
            if (editor.isFocusOwner() && comboHospede.getItemCount() > 0 && !comboHospede.isPopupVisible()) {
                comboHospede.setPopupVisible(true);
            }
        } finally {
            internalUpdate = false;
        }
    }

    /* ===== Fluxo de check-in ===== */
    private int realizarCheckInAcao(int hospedeId, int camaId, String dataEntradaISO, String dataSaidaISO) throws Exception {
        String insertHosp =
            "INSERT INTO hospedagem (hospede_id, cama_id, data_entrada, data_saida, status) " +
            "VALUES (?, ?, ?, ?, 1)";
        String ocupaCama  = "UPDATE cama SET status = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(insertHosp, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement ps2 = conn.prepareStatement(ocupaCama)) {

                ps1.setInt(1, hospedeId);
                ps1.setInt(2, camaId);
                ps1.setString(3, dataEntradaISO);
                ps1.setString(4, dataSaidaISO);
                int affectedRows = ps1.executeUpdate();

                if (affectedRows == 0) {
                    throw new Exception("Falha ao inserir hospedagem, nenhuma linha afetada.");
                }

                int idHospedagem;
                try (ResultSet generatedKeys = ps1.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idHospedagem = generatedKeys.getInt(1);
                    } else {
                        throw new Exception("Falha ao obter ID gerado da hospedagem.");
                    }
                }

                ps2.setInt(1, camaId);
                ps2.executeUpdate();

                conn.commit();
                return idHospedagem;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void realizarCheckInAcao() throws Exception {
        Item hospede = (Item) comboHospede.getSelectedItem();
        if (hospede == null) {
            JOptionPane.showMessageDialog(this, "Selecione um hóspede na lista (digite para buscar e escolha).", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (hasHospedagemAtiva(hospede.id)) {
            JOptionPane.showMessageDialog(this, "Este hóspede já possui hospedagem ativa.", "Atenção", JOptionPane.WARNING_MESSAGE);
            lastQuery = ((JTextComponent) comboHospede.getEditor().getEditorComponent()).getText().trim();
            atualizarSugestoesHospede(lastQuery);
            return;
        }

        Item cama = (Item) comboCama.getSelectedItem();
        if (cama == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma cama disponível.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate entrada, saida;
        try {
            entrada = parseFromChooser(dcEntrada);

            String saidaText = editor(dcSaida).getText().trim();
            if (saidaText.isEmpty()) {
                saida = entrada.plusDays(30);
                setChooserDate(dcSaida, saida);
            } else {
                saida = parseFromChooser(dcSaida);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Datas inválidas. Use dd/MM/yyyy ou ddMMyyyy.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (saida.isBefore(entrada)) {
            JOptionPane.showMessageDialog(this, "Saída não pode ser anterior à entrada.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int idHospedagem;
        try {
            idHospedagem = realizarCheckIn(hospede.id, cama.id, ISO.format(entrada), ISO.format(saida));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao inserir hospedagem.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        atualizarTabela();
        carregarCamasDisponiveis();

        lastQuery = "";
        String termo = ((JTextComponent) comboHospede.getEditor().getEditorComponent()).getText().trim();
        atualizarSugestoesHospede(termo);
        ajustarSaidaPadrao30dias();

        JTextComponent editor = (JTextComponent) comboHospede.getEditor().getEditorComponent();
        editor.setText("");
        comboHospede.setSelectedItem(null);
        editor.requestFocusInWindow();

        if (onAfterCheckIn != null) {
            try { onAfterCheckIn.run(); } catch (Exception ignore) {}
        }

        // === GERAR CONTRATO ===
        String nome = "", documento = "", telefone = "", email = "", numeroQuarto = "";

        try (Connection conn = DatabaseConnector.conectar()) {
            // dados do hóspede
            try (PreparedStatement ps = conn.prepareStatement("SELECT nome, documento, telefone, email FROM hospede WHERE id = ?")) {
                ps.setInt(1, hospede.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nome = rs.getString("nome");
                        documento = rs.getString("documento");
                        telefone = rs.getString("telefone");
                        email = rs.getString("email");
                    }
                }
            }

            // número do quarto via cama
            try (PreparedStatement ps = conn.prepareStatement("SELECT quarto_numero FROM cama WHERE id = ?")) {
                ps.setInt(1, cama.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        numeroQuarto = String.valueOf(rs.getInt("quarto_numero"));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao buscar dados para o contrato.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("Nome: " + nome);
        System.out.println("Documento: " + documento);
        System.out.println("Email: " + email);
        System.out.println("Telefone: " + telefone);
        System.out.println("Quarto: " + numeroQuarto);
        System.out.println("Data Saída: " + FRIENDLY.format(saida));

        Map<String, String> dadosContrato = new HashMap<>();
        dadosContrato.put("{{reserva_id}}", String.valueOf(idHospedagem));
        dadosContrato.put("{{ano}}", String.valueOf(LocalDate.now().getYear()));
        dadosContrato.put("{{nome}}", nome);
        dadosContrato.put("{{documento}}", documento);
        dadosContrato.put("{{telefone}}", telefone);
        dadosContrato.put("{{email}}", email);
        dadosContrato.put("{{data_entrada}}", FRIENDLY.format(entrada));
        dadosContrato.put("{{data_saida}}", FRIENDLY.format(saida));
        dadosContrato.put("{{quarto}}", numeroQuarto);

        GerarContrato.gerarComDialogo(this, "modelos/modelo_reserva.docx", dadosContrato);
    }



    // Confirma se há hospedagem ativa para o hóspede
    private boolean hasHospedagemAtiva(int hospedeId) {
        String sql = "SELECT 1 FROM hospedagem WHERE hospede_id = ? AND status = 1 LIMIT 1";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hospedeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao verificar hospedagem ativa.", "Erro", JOptionPane.ERROR_MESSAGE);
            // Em caso de erro, melhor evitar o check-in por segurança
            return true;
        }
    }

    private void buscarHospedesPorNome(String filtro) {
        // (Se for reutilizar, manter a mesma regra: apenas sem hospedagem ativa)
        DefaultComboBoxModel<Item> model = new DefaultComboBoxModel<>();
        String sql =
            "SELECT ho.id, ho.nome " +
            "FROM hospede ho " +
            "WHERE ho.nome LIKE ? " +
            "  AND NOT EXISTS ( " +
            "      SELECT 1 FROM hospedagem h " +
            "       WHERE h.hospede_id = ho.id AND h.status = 1 " +
            "  ) " +
            "ORDER BY ho.id DESC, ho.nome ASC";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) model.addElement(new Item(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comboHospede.setModel(model);
    }

    private void carregarCamasDisponiveis() {
        comboCama.removeAllItems();
        String sql = "SELECT id, descricao FROM cama WHERE status = 0 ORDER BY descricao";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) comboCama.addItem(new Item(rs.getInt("id"), rs.getString("descricao")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int realizarCheckIn(int hospedeId, int camaId, String dataEntradaISO, String dataSaidaISO) throws Exception {
        String insertHosp =
            "INSERT INTO hospedagem (hospede_id, cama_id, data_entrada, data_saida, status) " +
            "VALUES (?, ?, ?, ?, 1)";
        String ocupaCama  = "UPDATE cama SET status = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(insertHosp);
                 PreparedStatement ps2 = conn.prepareStatement(ocupaCama);
                 Statement stmt = conn.createStatement()) {

                ps1.setInt(1, hospedeId);
                ps1.setInt(2, camaId);
                ps1.setString(3, dataEntradaISO);
                ps1.setString(4, dataSaidaISO);
                int affectedRows = ps1.executeUpdate();

                if (affectedRows == 0) {
                    throw new Exception("Falha ao inserir hospedagem, nenhuma linha afetada.");
                }

                // Pega o último ID inserido na conexão atual SQLite
                int idHospedagem = -1;
                try (ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        idHospedagem = rs.getInt(1);
                    }
                }

                if (idHospedagem == -1) {
                    throw new Exception("Falha ao obter ID gerado da hospedagem.");
                }

                ps2.setInt(1, camaId);
                ps2.executeUpdate();

                conn.commit();
                return idHospedagem;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }



    private void atualizarTabela() {
        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0);

        String sql =
            "SELECT h.id, ho.nome AS hospede, ho.email, ho.telefone, c.descricao AS cama, h.data_entrada, h.data_saida " +
            "FROM hospedagem h " +
            "JOIN hospede ho ON ho.id = h.hospede_id " +
            "JOIN cama c ON c.id = h.cama_id " +
            "WHERE h.status = 1 " +
            "ORDER BY h.id DESC";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String hospede = rs.getString("hospede");
                String email = rs.getString("email");
                String telefone = rs.getString("telefone");
                String cama = rs.getString("cama");
                String entradaISO = rs.getString("data_entrada");
                String saidaISO = rs.getString("data_saida");

                String entradaFriendly = isoToFriendly(entradaISO);
                String saidaFriendly = isoToFriendly(saidaISO);

                modelo.addRow(new Object[] { id, hospede, email, telefone, cama, entradaFriendly, saidaFriendly });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao atualizar tabela de check-ins.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void preencherDatasPadrao() {
        Date hoje = new Date();
        dcEntrada.setDate(hoje);
        ajustarSaidaPadrao30dias();
    }

    private void ajustarSaidaPadrao30dias() {
        try {
            LocalDate entrada = parseFromChooser(dcEntrada);
            LocalDate saida = entrada.plusDays(30);
            setChooserDate(dcSaida, saida);
        } catch (Exception e) {
            // ignorar erro se data inválida
        }
    }

    private LocalDate parseFromChooser(JDateChooser chooser) {
        Date data = chooser.getDate();
        if (data == null) throw new DateTimeParseException("Data nula", "", 0);
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void setChooserDate(JDateChooser chooser, LocalDate date) {
        Date d = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        chooser.setDate(d);
    }

    private String isoToFriendly(String iso) {
        if (iso == null) return "";
        try {
            LocalDate d = LocalDate.parse(iso, ISO);
            return FRIENDLY.format(d);
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    private JTextComponent editor(JDateChooser chooser) {
        return (JTextComponent) chooser.getDateEditor().getUiComponent();
    }

    // Auto-slasher: coloca / automaticamente em datas dd/mm/aaaa
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
