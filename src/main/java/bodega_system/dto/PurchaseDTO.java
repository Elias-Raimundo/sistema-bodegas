package bodega_system.dto;

import java.util.List;

// Compra a un proveedor con detalle de productos
public class PurchaseDTO {
    public String invoiceNumber;
    public String invoiceDate;      // yyyy-MM-dd (vacío = hoy)
    public Double paidAmount;       // pago inicial (opcional)
    public String description;
    public List<Item> items;

    public static class Item {
        public Long productId;      // producto existente
        public String newName;      // o producto nuevo
        public Long newCategoryId;  // categoría del producto nuevo (opcional)
        public Double quantity;     // se suma al stock
        public Double unitCost;     // pasa a ser el costo del producto
        public Double salePrice;    // nuevo precio de venta (null = no cambiar)
    }
}
