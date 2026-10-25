import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    static final String URL = "jdbc:mysql://localhost:3306/inventory_db";
    static final String USER = "root";
    static final String PASSWORD = "tare";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n========================================");
            System.out.println("     INVENTORY MANAGEMENT SYSTEM        ");
            System.out.println("========================================");
            System.out.println("1. Add New Item");
            System.out.println("2. View All Items");
            System.out.println("3. Update Item Details (Quantity/Price)");
            System.out.println("4. Search Item by Name");
            System.out.println("5. Check Low Stock Items (< 5)");
            System.out.println("6. Restock Existing Item (Add Stock)");
            System.out.println("7. Deduct Stock (Sell Item)");
            System.out.println("8. Calculate Total Inventory Valuation");
            System.out.println("9. View Inventory Statistics (Dashboard)");
            System.out.println("10. Delete Item");
            System.out.println("11. Exit");
            System.out.print("Enter your choice (1-11): ");

            if (!scanner.hasNextInt()) {
                System.out.println("ERROR: Please enter a valid number between 1 and 11.");
                scanner.next();
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: addItem(scanner); break;
                case 2: viewItems(); break;
                case 3: updateItem(scanner); break;
                case 4: searchItem(scanner); break;
                case 5: checkLowStock(); break;
                case 6: restockItem(scanner); break;
                case 7: deductStock(scanner); break;
                case 8: calculateTotalValue(); break;
                case 9: viewInventoryStats(); break;
                case 10: deleteItem(scanner); break;
                case 11:
                    System.out.println("Exiting System. Thank you!");
                    scanner.close();
                    return;
                default:
                    System.out.println("INVALID CHOICE: Please select between 1 and 11.");
            }
        }
    }

    private static void addItem(Scanner scanner) {
        System.out.print("Enter Item Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("ERROR: Name cannot be empty."); return; }

        System.out.print("Enter Quantity: ");
        while (!scanner.hasNextInt()) { scanner.next(); }
        int quantity = scanner.nextInt(); scanner.nextLine();

        System.out.print("Enter Price (INR): ");
        double price = Double.parseDouble(scanner.nextLine().trim());

        String sql = "INSERT INTO items (name, quantity, price) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
            System.out.println("SUCCESS: Item added successfully!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void viewItems() {
        String sql = "SELECT * FROM items";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            printTable(rs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void updateItem(Scanner scanner) {
        System.out.print("Enter Item ID to update: ");
        int id = scanner.nextInt(); scanner.nextLine();
        System.out.print("Enter New Quantity: ");
        int quantity = scanner.nextInt(); scanner.nextLine();
        System.out.print("Enter New Price: ");
        double price = Double.parseDouble(scanner.nextLine().trim());

        String sql = "UPDATE items SET quantity = ?, price = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, id);
            if (pstmt.executeUpdate() > 0) System.out.println("SUCCESS: Item updated!");
            else System.out.println("WARNING: ID not found!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void searchItem(Scanner scanner) {
        System.out.print("Enter Item Name to search: ");
        String keyword = scanner.nextLine().trim();
        String sql = "SELECT * FROM items WHERE name LIKE ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) { printTable(rs); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void checkLowStock() {
        String sql = "SELECT * FROM items WHERE quantity < 5";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("\n--- LOW STOCK ITEMS (< 5) ---");
            printTable(rs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void restockItem(Scanner scanner) {
        System.out.print("Enter Item ID to restock: ");
        int id = scanner.nextInt(); scanner.nextLine();
        System.out.print("Enter quantity to add: ");
        int addQty = scanner.nextInt(); scanner.nextLine();

        String sql = "UPDATE items SET quantity = quantity + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, id);
            if (pstmt.executeUpdate() > 0) System.out.println("SUCCESS: Restocked successfully!");
            else System.out.println("WARNING: ID not found!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void deductStock(Scanner scanner) {
        System.out.print("Enter Item ID to sell: ");
        int id = scanner.nextInt(); scanner.nextLine();
        System.out.print("Enter quantity to sell: ");
        int soldQty = scanner.nextInt(); scanner.nextLine();

        String updateSql = "UPDATE items SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, soldQty);
            pstmt.setInt(2, id);
            pstmt.setInt(3, soldQty);
            if (pstmt.executeUpdate() > 0) System.out.println("SUCCESS: Sale recorded, stock deducted!");
            else System.out.println("ERROR: Insufficient stock or ID not found!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void calculateTotalValue() {
        String sql = "SELECT SUM(quantity * price) AS total_value FROM items";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                System.out.printf("Total Inventory Valuation: ₹%.2f\n", rs.getDouble("total_value"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void viewInventoryStats() {
        String sql = "SELECT COUNT(*) as total_items, SUM(quantity) as total_units FROM items";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                System.out.println("Total Unique Products: " + rs.getInt("total_items"));
                System.out.println("Total Units in Stock: " + rs.getInt("total_units"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void deleteItem(Scanner scanner) {
        System.out.print("Enter Item ID to delete: ");
        int id = scanner.nextInt(); scanner.nextLine();
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            if (pstmt.executeUpdate() > 0) System.out.println("SUCCESS: Deleted!");
            else System.out.println("WARNING: ID not found!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void printTable(ResultSet rs) throws SQLException {
        System.out.println("----------------------------------------------------------");
        System.out.printf("%-5s %-20s %-10s %-15s\n", "ID", "Name", "Quantity", "Price");
        System.out.println("----------------------------------------------------------");
        while (rs.next()) {
            System.out.printf("%-5d %-20s %-10d ₹%-14.2f\n", rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getDouble("price"));
        }
        System.out.println("----------------------------------------------------------");
    }
}