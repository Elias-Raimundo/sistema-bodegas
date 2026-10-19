package bodega_system.dto;

// Aumento (o rebaja) de precios en masa
public class BulkPriceDTO {
    // "PRODUCTS" (productos) o "PREPARED" (tragos)
    public String target;
    // Solo para productos: categoría a modificar (null = todas)
    public Long categoryId;
    // Porcentaje: 15 = +15 %, -10 = -10 %
    public Double percent;
    // Redondeo hacia arriba al múltiplo indicado: 0/1 = al peso, 10, 50, 100
    public Integer roundTo;
}
