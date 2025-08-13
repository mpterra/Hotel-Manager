package view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import db.DatabaseInitializer;
import model.Sessao;

public class TelaPrincipal {

    private JFrame frame;
    private JPanel painelCentral;

    public static void main(String[] args) {

        // Aplica visual do Windows
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                TelaPrincipal window = new TelaPrincipal();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public TelaPrincipal() {
        initialize();
        abrirInicioPanel();
    }

    public JFrame getFrame() {
        return frame;
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Sistema de Gestão do Hotel");
        frame.setBounds(100, 100, 980, 700);
        frame.setLocationRelativeTo(null); // centraliza na tela
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Inicializa painel central
        painelCentral = new JPanel(new BorderLayout());
        frame.getContentPane().add(painelCentral, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        // Menu Inicio
        JMenu menuInicio = criarMenu("Início");
        menuInicio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuInicio.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirInicioPanel();
            }
        });
        menuBar.add(menuInicio);

        // Menu Cadastro
        JMenu menuCadastro = criarMenu("Cadastro");

        JMenuItem itemHospede = criarItem("Hóspede");
        itemHospede.addActionListener(e -> abrirCadastroHospede());
        menuCadastro.add(itemHospede);

        JMenuItem itemQuarto = criarItem("Quarto");
        itemQuarto.addActionListener(e -> abrirCadastroQuarto());
        menuCadastro.add(itemQuarto);

        JMenuItem itemCama = criarItem("Cama");
        itemCama.addActionListener(e -> abrirCadastroCama());
        menuCadastro.add(itemCama);

        menuBar.add(menuCadastro);

        // Menu Hospedagem
        JMenu menuHospedagem = criarMenu("Hospedagem");
        JMenuItem itemCheckIn = criarItem("Check-In");
        itemCheckIn.addActionListener(e -> abrirCheckInPanel());
        menuHospedagem.add(itemCheckIn);

        JMenuItem itemCheckOut = criarItem("Check-Out");
        itemCheckOut.addActionListener(e -> abrirCheckOutPanel());
        menuHospedagem.add(itemCheckOut);

        JMenuItem itemReserva = criarItem("Reserva");
        itemReserva.addActionListener(e -> {
            try {
                abrirReservaPanel();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        menuHospedagem.add(itemReserva);

        menuBar.add(menuHospedagem);

        // Menu Relatórios
        JMenu menuRelatorios = criarMenu("Relatórios");
        menuRelatorios.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuRelatorios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirRelatoriosPanel();
            }
        });
        menuBar.add(menuRelatorios);

        // Menu Usuário
        JMenu menuUsuario = criarMenu("Usuário");
        JMenuItem itemCriarUsuario = criarItem("Criar Usuário");
        itemCriarUsuario.addActionListener(e -> abrirCadastroUsuario());
        menuUsuario.add(itemCriarUsuario);
        menuUsuario.add(criarItem("Alterar Senha"));
        JMenuItem itemSair = criarItem("Sair");
        itemSair.addActionListener(e -> frame.dispose());
        menuUsuario.add(itemSair);
        menuBar.add(menuUsuario);

        // Usuário logado alinhado à direita com margem
        JLabel lblUsuario = new JLabel("Usuário: " + Sessao.getUsuarioLogado().getLogin());
        lblUsuario.setHorizontalAlignment(SwingConstants.RIGHT);
        lblUsuario.setBorder(new EmptyBorder(0, 0, 0, 20)); // margem à direita
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(lblUsuario);
    }

    private void abrirInicioPanel() {
        painelCentral.removeAll();
        painelCentral.add(new InicioPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCadastroUsuario() {
        painelCentral.removeAll();
        painelCentral.add(new CadastroUsuarioPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCadastroHospede() {
        painelCentral.removeAll();
        painelCentral.add(new CadastroHospedePanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCadastroQuarto() {
        painelCentral.removeAll();
        painelCentral.add(new CadastroQuartoPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCadastroCama() {
        painelCentral.removeAll();
        painelCentral.add(new CadastroCamaPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCheckInPanel() {
        painelCentral.removeAll();
        painelCentral.add(new CheckInPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirCheckOutPanel() {
        painelCentral.removeAll();
        painelCentral.add(new CheckOutPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirReservaPanel() throws Exception {
        painelCentral.removeAll();
        painelCentral.add(new ReservaPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void abrirRelatoriosPanel() {
        painelCentral.removeAll();
        painelCentral.add(new RelatoriosPanel(), BorderLayout.CENTER);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private JMenu criarMenu(String titulo) {
        JMenu menu = new JMenu(titulo);
        menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return menu;
    }

    private JMenuItem criarItem(String texto) {
        JMenuItem item = new JMenuItem(texto);
        item.setIcon(null);
        item.setDisabledIcon(null);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return item;
    }
}
