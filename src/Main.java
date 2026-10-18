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
            System.out.println("1. Add Item");
            System.out.println("2. View All Items");
            System.out.println("3. Update Item (Quantity/Price)");
            System.out.println("4. Search Item by Name");
            System.out.println("5. Check Low Stock Items (< 5)");
            System.out.println("6. Delete Item");
            System.out.println("7. Exit");
            System.out.print("Enter your choice (1-7): ");

            if (!scanner.hasNextInt()) {
                System.out.println("ERROR: Please enter a valid number between 1 and 7.");
                scanner.next();
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addItem(scanner);
                    break;
                case 2:
                    viewItems();
                    break;
                case 3:
                    updateItem(scanner);
                    break;
                case 4:
                    searchItem(scanner);
                    break;
                case 5:
                    checkLowStock();
                    break;
                case 6:
                    deleteItem(scanner);
                    break;
                case 7:
                    System.out.println("Exiting System. Thank you!");
                    scanner.close();
                    return;
                default:
                    System.out.println("INVALID CHOICE: Please select between 1 and 7.");
            }
        }
    }

    private static void addItem(Scanner scanner) {
        System.out.print("Enter Item Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("ERROR: Item name cannot be empty.");
            return;
        }

        System.out.print("Enter Quantity: ");
        while (!scanner.hasNextInt()) {
            System.out.println("ERROR: Enter a valid integer for quantity.");
            scanner.next();
        }
        int quantity = scanner.nextInt();
        scanner.nextLine();

        if (quantity < 0) {
            System.out.println("ERROR: Quantity cannot be negative.");
            return;
        }

        System.out.print("Enter Price: ");
        double price;
        try {
            String priceStr = scanner.nextLine().trim();
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Invalid price format.");
            return;
        }

        if (price < 0) {
            System.out.println("ERROR: Price cannot be negative.");
            return;
        }

        String sql = "INSERT INTO items (name, quantity, price) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("SUCCESS: Item added successfully to MySQL database!");
            }

        } catch (SQLException e) {
            System.out.println("Database Error while adding item!");
            e.printStackTrace();
        }
    }

    private static void viewItems() {
        String sql = "SELECT * FROM items";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            printTable(rs);

        } catch (SQLException e) {
            System.out.println("Database Error while viewing items!");
            e.printStackTrace();
        }
    }

    private static void updateItem(Scanner scanner) {
        System.out.print("Enter Item ID to update: ");
        if (!scanner.hasNextInt()) {
            System.out.println("ERROR: Invalid ID format.");
            scanner.next();
            return;
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Enter New Quantity: ");
        int quantity = scanner.nextInt();

        System.out.print("Enter New Price: ");
        double price = scanner.nextDouble();
        scanner.nextLine();

        String sql = "UPDATE items SET quantity = ?, price = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantity);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, id);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("SUCCESS: Item updated successfully!");
            } else {
                System.out.println("WARNING: Item ID not found!");
            }

        } catch (SQLException e) {
            System.out.println("Database Error while updating item!");
            e.printStackTrace();
        }
    }

    private static void searchItem(Scanner scanner) {
        System.out.print("Enter Item Name to search: ");
        String keyword = scanner.nextLine().trim();

        String sql = "SELECT * FROM items WHERE name LIKE ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            printTable(rs);

        } catch (SQLException e) {
            System.out.println("Database Error while searching item!");
            e.printStackTrace();
        }
    }

    private static void checkLowStock() {
        String sql = "SELECT * FROM items WHERE quantity < 5";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n--- LOW STOCK ITEMS (< 5) ---");
            printTable(rs);

        } catch (SQLException e) {
            System.out.println("Database Error while checking low stock!");
            e.printStackTrace();
        }
    }

    private static void deleteItem(Scanner scanner) {
        System.out.print("Enter Item ID to delete: ");
        if (!scanner.hasNextInt()) {
            System.out.println("ERROR: Invalid ID format.");
            scanner.next();
            return;
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("SUCCESS: Item deleted successfully!");
            } else {
                System.out.println("WARNING: Item ID not found!");
            }

        } catch (SQLException e) {
            System.out.println("Database Error while deleting item!");
            e.printStackTrace();
        }
    }

    private static void printTable(ResultSet rs) throws SQLException {
        System.out.println("--------------------------------------------------");
        System.out.printf("%-5s %-20s %-10s %-10s\n", "ID", "Name", "Quantity", "Price");
        System.out.println("--------------------------------------------------");

        boolean found = false;
        while (rs.next()) {
            found = true;
            int id = rs.getInt("id");
            String name = rs.getString("name");
            int quantity = rs.getInt("quantity");
            double price = rs.getDouble("price");

            System.out.printf("%-5d %-20s %-10d %-10.2f\n", id, name, quantity, price);
        }

        if (!found) {
            System.out.println("No matching items found.");
        }
        System.out.println("--------------------------------------------------");
    }
}