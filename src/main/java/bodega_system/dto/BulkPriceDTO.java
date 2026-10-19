package bodega_system.dto;

import java.util.List;

// Actualización de precios en masa
public class BulkPriceDTO {
    // "PRODUCTS" (productos) o "PREPARED" (tragos)
    public String target;
    // Productos (o tragos) elegidos uno por uno
    public List<Long> ids;
    // "PERCENT" (porcentaje) o "AMOUNT" (monto fijo en pesos)
    public String mode;
    // Valor: 15 = +15 % o +$15 según el modo. Negativo para bajar.
    public Double value;
    // Qué precio cambia: "PRICE" (venta), "COST" (costo) o "BOTH"
    public String applyTo;
    // Redondeo hacia arriba del precio de venta: 0/1 = al peso, 10, 50, 100
    public Integer roundTo;
}
