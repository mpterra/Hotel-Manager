package view;

import java.awt.Cursor;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

import db.DatabaseInitializer;
import model.Sessao;
import model.Usuario;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;

public class TelaLogin {

	private JFrame frame;
	private JTextField textLogin;
	private JPasswordField passwordField;

	public static void main(String[] args) {
		try {
	        UIManager.setLookAndFeel(new FlatLightLaf());
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

		EventQueue.invokeLater(() -> {
			// Garante que o banco e o usuário admin já estão prontos
			DatabaseInitializer.inicializarBanco();

			TelaLogin window = new TelaLogin();
			window.frame.setVisible(true);
		});
	}

	public TelaLogin() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setTitle("Sistema de Gestão do Hotel");
		frame.setBounds(100, 100, 360, 366);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setLayout(null);

		JLabel lblLogin = new JLabel("Login");
		lblLogin.setHorizontalAlignment(SwingConstants.CENTER);
		lblLogin.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblLogin.setBounds(118, 39, 106, 21);
		frame.getContentPane().add(lblLogin);

		JLabel lblSenha = new JLabel("Senha");
		lblSenha.setHorizontalAlignment(SwingConstants.CENTER);
		lblSenha.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblSenha.setBounds(118, 99, 113, 21);
		frame.getContentPane().add(lblSenha);

		textLogin = new JTextField();
		textLogin.setBounds(78, 60, 191, 20);
		frame.getContentPane().add(textLogin);
		textLogin.setColumns(10);

		passwordField = new JPasswordField();
		passwordField.setBounds(78, 121, 191, 20);
		frame.getContentPane().add(passwordField);

		JLabel lblUsuarioIncorreto = new JLabel("Usuário ou senha incorretos");
		lblUsuarioIncorreto.setHorizontalAlignment(SwingConstants.CENTER);
		lblUsuarioIncorreto.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblUsuarioIncorreto.setForeground(Color.RED);
		lblUsuarioIncorreto.setBounds(78, 249, 191, 14);
		frame.getContentPane().add(lblUsuarioIncorreto);
		lblUsuarioIncorreto.setVisible(false);

		JButton btnEntrar = new JButton("Entrar");
		btnEntrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnEntrar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String login = textLogin.getText();
				String senha = new String(passwordField.getPassword());

				Usuario usuario = new Usuario(login, senha);
				boolean existe_usuario = usuario.autenticar();

				if (existe_usuario) {
					Sessao.setUsuarioLogado(usuario);
					TelaPrincipal telaPrincipal = new TelaPrincipal();
					telaPrincipal.getFrame().setVisible(true);
					frame.dispose();
				} else {
					lblUsuarioIncorreto.setVisible(true);
				}
			}
		});
		btnEntrar.setBounds(128, 176, 89, 23);
		frame.getContentPane().add(btnEntrar);
	}
}