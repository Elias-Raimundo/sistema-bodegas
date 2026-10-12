package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TableOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private int quantity;
    private double price;
    private Long productId;
    private String itemType;
    private Long preparedProductId;

    // NUEVO: soporte para "dividir" un ítem entre varias mesas.
    // linkedGroupId agrupa todas las partes de un mismo reparto.
    // stockOwner marca cuál de las partes es la que descuenta stock real
    // al cerrar la mesa (para no descontar el mismo producto varias veces).
    private String linkedGroupId;
    private Boolean stockOwner;

    // Cantidad real de unidades que descuenta del stock la parte "dueña"
    // (ej: 2 Heineken divididas entre 3 mesas -> la dueña descuenta 2).
    private Integer stockQuantity;

    // Entre cuántas mesas se dividió el ítem (para mostrarlo en pantalla).
    private Integer splitParts;

    @JsonIgnore
    @ManyToOne
    private TableOrder order;

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public Long getProductId() {
        return productId;
    }

    public TableOrder getOrder() {
        return order;
    }

    public void setProductName(String productName){
        this.productName = productName;
    }

    public void setQuantity(int quantity){
        this.quantity = quantity;
    }

    public void setPrice(double price){
        this.price = price;
    }

    public void setOrder(TableOrder order){
        this.order = order;
    }
    
    public String getItemType(){
        return itemType;
    }

    public Long getPreparedProductId(){
        return preparedProductId;
    }

    public void setProductId(Long productId){
        this.productId = productId;
    }

    public void setItemType(String itemType){
        this.itemType = itemType;
    }

    public void setPreparedProductId(Long preparedProductId){
        this.preparedProductId = preparedProductId;
    }

    public String getLinkedGroupId() {
        return linkedGroupId;
    }

    public void setLinkedGroupId(String linkedGroupId) {
        this.linkedGroupId = linkedGroupId;
    }


    public boolean isStockOwner() {
        return stockOwner == null || stockOwner;
    }

    public void setStockOwner(Boolean stockOwner) {
        this.stockOwner = stockOwner;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getSplitParts() {
        return splitParts;
    }

    public void setSplitParts(Integer splitParts) {
        this.splitParts = splitParts;
    }

    // Unidades a descontar del stock al cerrar la mesa:
    // - ítem normal: su cantidad
    // - ítem compartido: solo la parte dueña descuenta, y descuenta
    //   las unidades reales que se dividieron (no el "1" de la parte)
    @JsonIgnore
    public int getUnitsForStock() {
        if (linkedGroupId == null) return quantity;
        if (!isStockOwner()) return 0;
        return stockQuantity != null ? stockQuantity : quantity;
    }

}
