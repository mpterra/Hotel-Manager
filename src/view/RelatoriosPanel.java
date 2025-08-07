package view;

import javax.swing.*;
import java.awt.*;
import view.dialogs.RelatorioCamasVagasDialog;
import view.dialogs.RelatorioHospedesHospedadosDialog;
import view.dialogs.RelatorioHistoricoHospedagensDialog;
import view.dialogs.ProjecaoVagasDialog;  // Importa a nova dialog

public class RelatoriosPanel extends JPanel {

    public RelatoriosPanel() {
        setLayout(null);

        int x = 50;
        int y = 30;
        int largura = 300;
        int altura = 35;
        int espaco = 15;

        add(criarBotaoCamasVagas("1. Camas Vagas", x, y += 0, largura, altura));
        add(criarBotaoHospedesHospedados("2. Hóspedes Hospedados", x, y += altura + espaco, largura, altura));
        add(criarBotaoHistoricoHospedagens("3. Histórico de Hospedagens", x, y += altura + espaco, largura, altura));
        add(criarBotaoProjecaoVagas("4. Projeção de Vagas", x, y += altura + espaco, largura, altura));
    }

    private JButton criarBotaoCamasVagas(String texto, int x, int y, int largura, int altura) {
        JButton botao = new JButton(texto);
        botao.setBounds(x, y, largura, altura);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botao.addActionListener(e -> {
            new RelatorioCamasVagasDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
        });

        return botao;
    }

    private JButton criarBotaoHospedesHospedados(String texto, int x, int y, int largura, int altura) {
        JButton botao = new JButton(texto);
        botao.setBounds(x, y, largura, altura);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botao.addActionListener(e -> {
            new RelatorioHospedesHospedadosDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
        });

        return botao;
    }

    private JButton criarBotaoHistoricoHospedagens(String texto, int x, int y, int largura, int altura) {
        JButton botao = new JButton(texto);
        botao.setBounds(x, y, largura, altura);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botao.addActionListener(e -> {
            new RelatorioHistoricoHospedagensDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
        });

        return botao;
    }

    private JButton criarBotaoProjecaoVagas(String texto, int x, int y, int largura, int altura) {
        JButton botao = new JButton(texto);
        botao.setBounds(x, y, largura, altura);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botao.addActionListener(e -> {
            new ProjecaoVagasDialog((JFrame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
        });

        return botao;
    }
}
