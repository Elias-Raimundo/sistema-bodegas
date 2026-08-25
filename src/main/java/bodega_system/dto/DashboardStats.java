package bodega_system.dto;

public class DashboardStats {
    public long totalProducts;
    public Double totalStock;
    public long lowStock;
    public double inventoryValue;
    public double costValue;

    public long getTotalProducts() { return totalProducts; }
    public Double getTotalStock() { return totalStock; }
    public long getLowStock() { return lowStock; }
    public double getInventoryValue() { return inventoryValue; }
    public double getCostValue() { return costValue; }
}
