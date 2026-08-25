package bodega_system.dto;

import java.util.List;

import bodega_system.entity.Sale;

public class SalesReportDTO {

    public Double total;
    public Long salesCount;
    public Double totalProfit;
    public Integer productsSold;

    public double cash;
    public double transfer;
    public double debit;
    public double credit;

    public List<Sale> sales;
}