package bodega_system.dto;

import java.util.List;

public class SplitItemDTO {

    public List<SplitShareDTO> shares;

    public static class SplitShareDTO {
        public Long tableId;
        public Double amount;
    }
}
