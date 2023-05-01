import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {
    private JProgressBar progressBar;

    public ProgressDialog(JFrame frame, String title, String message) {
        super(frame, title, true);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setSize(300, 150);
        this.setLayout(new GridBagLayout());
        this.setLocationRelativeTo(frame);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10, 0, 10, 0);
        this.add(new JLabel(message), constraints);

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(false);
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 10, 10, 10);
        this.add(progressBar, constraints);
        this.setVisible(true);
    }

    public void setValue(int value) {
        progressBar.setValue(value);
        progressBar.setString("Processing " + value + " of " + getMaximum());
        System.out.println("Processing " + value + " of " + getMaximum());
        progressBar.repaint();
    }

    public int getValue() {
        return progressBar.getValue();
    }

    public int getMaximum() {
        return progressBar.getMaximum();
    }

    public void setMaximum(int value) {
        progressBar.setMaximum(value);
    }
}
