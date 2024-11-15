package frame;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import model.Employee;
import model.Product;

public class EmployeeFrame extends JFrame {
	private LoginFrame loginFrame;
    private String name; 
    private LocalDateTime loginTime;
    protected static final int WIDTH = 1300;
    protected static final int HEIGHT = 900;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH:mm");
    private JTable productsTable;
    private JTextField searchField;
    private DefaultTableModel tableModel; 
    private Employee employee;

    public EmployeeFrame(LoginFrame loginFrame,Employee employee) {
        this.loginFrame = loginFrame;
        this.loginTime = LocalDateTime.now();
        this.employee = employee;
        this.name = employee.getName();
        setSize(WIDTH, HEIGHT);
        setTitle("Employee Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        searchField = new JTextField(15);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearchSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearchSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearchSuggestions();
            }
        });
        searchPanel.add(new JLabel("Search: "), gbc);
        gbc.gridx = 1;
        searchPanel.add(searchField, gbc);
        
        headerPanel.add(searchPanel, BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JButton backButton = new JButton("Back");
        JButton addProductButton = new JButton("Add Products");
        JButton removeProductButton = new JButton("Remove Products");
        JButton addToCartButton = new JButton("Add to Cart");
        JButton viewCartButton = new JButton("View Cart");
        JButton checkoutButton = new JButton("Check out");
        addProductButton.addActionListener(e -> showAddProductOptions());		
        backButton.addActionListener(e -> back());
        removeProductButton.addActionListener(e -> removeProduct());
        addToCartButton.addActionListener(e -> back());
        viewCartButton.addActionListener(e -> back());   //Khanh nhét mấy hàm vào đây để test nhá
        checkoutButton.addActionListener(e -> back());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addProductButton);
        buttonPanel.add(backButton);
        buttonPanel.add(removeProductButton);
        buttonPanel.add(addToCartButton);
        buttonPanel.add(viewCartButton);
        buttonPanel.add(checkoutButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        productsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(productsTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER); 

        add(mainPanel, BorderLayout.CENTER);
        displayAllProducts();
    }
    private void showAddProductOptions() {
        Object[] options = {"Add by CSV", "Add by Hand"};
        int choice = JOptionPane.showOptionDialog(this,
                "How would you like to add products?",
                "Add Products",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0:
                employee.addProductsFromCSV();
                displayAllProducts();
                break;
            case 1:
                employee.addProductByHand();
                displayAllProducts();
                break;
            default:
                break;
        }
    }
    protected void back() {
        LocalDateTime logoutTime = LocalDateTime.now();
        long durationInMinutes = java.time.Duration.between(loginTime, logoutTime).toMinutes();
        logSession(logoutTime, durationInMinutes);
        loginFrame.setVisible(true);
        dispose();
    }

    private void logSession(LocalDateTime logoutTime, long durationInMinutes) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("/media/khanhtty/551473877464395A/OOPLAB/phq/src/log.csv", true))) {
            String formattedLoginTime = loginTime.format(dtf);
            String formattedLogoutTime = logoutTime.format(dtf);
            String logEntry = String.format("%s,%s,%s,%d", name, formattedLoginTime, formattedLogoutTime, durationInMinutes);
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error logging session data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void displayAllProducts() {
        String[] columnNames = { "ID", "Type", "Name", "Price", "Quantity", "Input Price", "Brand", "Suit Age", "Material", "Author", "ISBN", "Publication Year", "Publisher" };
        tableModel = new DefaultTableModel(columnNames, 0);
        productsTable.setModel(tableModel);


        int[] columnWidths = {20, 60, 300, 50, 50, 50, 100, 60, 75, 100, 100, 125, 225}; 
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        productsTable.setFillsViewportHeight(true); 

        for (int i = 0; i < columnWidths.length; i++) {
            TableColumn column = productsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]); 
            column.setResizable(true); 
        }
        List<Product> products = employee.getProductsFromFile();
        for (Product product : products) {
            String[] rowData = employee.getProductRowData(product);
            tableModel.addRow(rowData);
        }
    }
    protected void updateSearchSuggestions() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0); 

        if (query.isEmpty()) {
            displayAllProducts();
            return;
        }
        List<Product> products = employee.getProductsFromFile();
        List<Product> filteredProducts = products.stream()
            .filter(product -> employee.productMatchesQuery(product, query))
            .collect(Collectors.toList());

        for (Product product : filteredProducts) {
            String[] rowData = employee.getProductRowData(product);
            tableModel.addRow(rowData);
        }
    }
    private void removeProduct() {
        int selectedRow = productsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to remove.");
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 2);

        int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove " + name + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            employee.removeProduct(id);
            displayAllProducts();
            JOptionPane.showMessageDialog(this, name + " removed successfully.");
        }
    }
}
