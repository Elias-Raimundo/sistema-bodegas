package bodega_system.dto;

import java.util.List;
import bodega_system.entity.SalePayment;

public class CloseTableDTO{
    public List<SalePayment> payments;
    public Double discount;
}