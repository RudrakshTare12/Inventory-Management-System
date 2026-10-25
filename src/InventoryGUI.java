import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

public class InventoryGUI extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/inventory_db";
    private static final String USER = "root";
    private static final String PASSWORD = "tare";

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField nameField, qtyField, priceField, searchField;
    private JLabel totalProductsLabel, totalUnitsLabel, totalValuationLabel;

    public InventoryGUI() {
        setTitle("Inventory Management System - Pro Dashboard");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. TOP DASHBOARD CARDS
        JPanel dashboardPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        totalProductsLabel = new JLabel("Total Products: 0", JLabel.CENTER);
        totalUnitsLabel = new JLabel("Stock Units: 0", JLabel.CENTER);
        totalValuationLabel = new JLabel("Valuation: ₹0.00", JLabel.CENTER);

        styleCard(totalProductsLabel, new Color(41, 128, 185));
        styleCard(totalUnitsLabel, new Color(39, 174, 96));
        styleCard(totalValuationLabel, new Color(142, 68, 173));

        dashboardPanel.add(totalProductsLabel);
        dashboardPanel.add(totalUnitsLabel);
        dashboardPanel.add(totalValuationLabel);
        add(dashboardPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL (Search & Table)
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("Search Item: "));
        searchField = new JTextField(25);
        searchPanel.add(searchField);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Quantity", "Price (₹)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM PANEL (Inputs & Actions)
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.add(new JLabel("Name:"));
        nameField = new JTextField(12);
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Qty:"));
        qtyField = new JTextField(6);
        inputPanel.add(qtyField);

        inputPanel.add(new JLabel("Price (₹):"));
        priceField = new JTextField(8);
        inputPanel.add(priceField);

        bottomContainer.add(inputPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addButton = new JButton("Add Item");
        JButton restockButton = new JButton("Restock (+)");
        JButton sellButton = new JButton("Sell / Deduct (-)");
        JButton deleteButton = new JButton("Delete Item");
        JButton refreshButton = new JButton("Refresh");

        buttonPanel.add(addButton);
        buttonPanel.add(restockButton);
        buttonPanel.add(sellButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        bottomContainer.add(buttonPanel, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);

        // --- EVENT LISTENERS ---
        addButton.addActionListener(e -> addItem());
        restockButton.addActionListener(e -> modifyStock(true));
        sellButton.addActionListener(e -> modifyStock(false));
        deleteButton.addActionListener(e -> deleteItem());
        refreshButton.addActionListener(e -> loadTableData());

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });

        loadTableData();
    }

    private void styleCard(JLabel label, Color bg) {
        label.setOpaque(true);
        label.setBackground(bg);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
    }

    private void filterTable() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        int totalProducts = 0;
        int totalUnits = 0;
        double totalValuation = 0.0;

        String sql = "SELECT * FROM items";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");

                totalProducts++;
                totalUnits += qty;
                totalValuation += (qty * price);

                tableModel.addRow(new Object[]{id, name, qty, price});
            }

            totalProductsLabel.setText("Total Products: " + totalProducts);
            totalUnitsLabel.setText("Stock Units: " + totalUnits);
            totalValuationLabel.setText(String.format("Valuation: ₹%.2f", totalValuation));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error while loading data!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addItem() {
        String name = nameField.getText().trim();
        String qtyStr = qtyField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields must be filled!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);

            if (qty < 0 || price < 0) {
                JOptionPane.showMessageDialog(this, "Quantity and Price cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "INSERT INTO items (name, quantity, price) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, qty);
                pstmt.setDouble(3, price);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Item added successfully!");
                nameField.setText("");
                qtyField.setText("");
                priceField.setText("");
                loadTableData();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for Quantity and Price!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error while adding item!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void modifyStock(boolean isRestock) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the table first!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String actionName = isRestock ? "Restock" : "Sell / Deduct";

        String input = JOptionPane.showInputDialog(this, "Enter quantity to " + actionName + ":");
        if (input != null && !input.trim().isEmpty()) {
            try {
                int amount = Integer.parseInt(input.trim());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be greater than zero!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = isRestock
                        ? "UPDATE items SET quantity = quantity + ? WHERE id = ?"
                        : "UPDATE items SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";

                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, amount);
                    pstmt.setInt(2, id);
                    if (!isRestock) {
                        pstmt.setInt(3, amount);
                    }

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Stock updated successfully!");
                        loadTableData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Operation failed! Check if stock is sufficient.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid integer quantity!", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error during stock update!", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void deleteItem() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the table to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete item ID " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM items WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Item deleted successfully!");
                loadTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error while deleting!", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryGUI().setVisible(true));
    }
}