package bodega_system.dto;

import java.util.List;

public class PreparedProductDTO {
    public String name;
    public Double price;
    public List<IngredientDTO> ingredients;

    public static class IngredientDTO{
        public Long productId;
        public Double quantity;
    }

}
