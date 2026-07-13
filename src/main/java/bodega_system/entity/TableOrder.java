package bodega_system.entity;

import java.util.List;
import jakarta.persistence.*;

@Entity
public class TableOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private TableBar table;

    private boolean closed;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<TableOrderItem> items;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<TableOrderPayment> partialPayments;

    public List<TableOrderItem> getItems(){
        return items;
    }
    public void setItems(List<TableOrderItem> items){
        this.items = items;
    }

    public List<TableOrderPayment> getPartialPayments(){
        return partialPayments;
    }
    public void setPartialPayments(List<TableOrderPayment> partialPayments){
        this.partialPayments = partialPayments;
    }

    public void setTable (TableBar table){
        this.table = table;
    }

    public void setClosed(boolean closed){
        this.closed=closed;
    }

    public Long getId(){
        return id;
    }

    public TableBar getTable(){
        return table;
    }

    public boolean isClosed(){
        return closed;
    }

}
