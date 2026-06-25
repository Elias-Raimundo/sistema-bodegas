package bodega_system.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import bodega_system.entity.*;
import bodega_system.repository.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerMovementRepository movementRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PreparedProductRepository preparedProductRepository;

    public CustomerController(
        CustomerRepository customerRepository,
        CustomerMovementRepository movementRepository,
        SaleRepository saleRepository,
        ProductRepository productRepository,
        PreparedProductRepository preparedProductRepository
    ) {
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.preparedProductRepository = preparedProductRepository;
    }

    @GetMapping
    public List<Customer> getAll(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        List<Customer> customers =
            customerRepository.findByCompany_IdOrderByNameAsc(companyId);

        for (Customer customer : customers) {
            customer.setBalance(calculateIndexedBalance(customer));
        }

        return customers;
    }

    @PostMapping
    public Customer create(
        @RequestBody Customer customer,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del cliente es obligatorio");
        }

        Company company = new Company();
        company.setId(companyId);

        customer.setName(customer.getName().trim());
        customer.setBalance(0.0);
        customer.setCompany(company);

        return customerRepository.save(customer);
    }

    @PostMapping("/{customerId}/debt")
    public Customer addDebt(
        @PathVariable Long customerId,
        @RequestBody CustomerMovement movement,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow();

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Cliente no autorizado");
        }

        if (movement.getAmount() == null || movement.getAmount() <= 0) {
            throw new RuntimeException("El importe debe ser mayor a cero");
        }

        CustomerMovement savedMovement = new CustomerMovement();
        savedMovement.setCustomer(customer);
        savedMovement.setAmount(movement.getAmount());
        savedMovement.setType("DEBT");
        savedMovement.setSaleId(null);
        savedMovement.setDescription(
            movement.getDescription() == null
                ? "Deuda cargada manualmente"
                : movement.getDescription()
        );
        savedMovement.setCreatedAt(
            LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"))
        );

        movementRepository.save(savedMovement);

        customer.setBalance(calculateIndexedBalance(customer));

        return customerRepository.save(customer);
    }

    @PostMapping("/{customerId}/payment")
    public Customer addPayment(
        @PathVariable Long customerId,
        @RequestBody CustomerMovement movement,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow();

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Cliente no autorizado");
        }

        if (movement.getAmount() == null || movement.getAmount() <= 0) {
            throw new RuntimeException("El importe debe ser mayor a cero");
        }

        double currentBalance = calculateIndexedBalance(customer);

        if (movement.getAmount() > currentBalance) {
            throw new RuntimeException("El pago no puede ser mayor a la deuda");
        }

        CustomerMovement savedMovement = new CustomerMovement();
        savedMovement.setCustomer(customer);
        savedMovement.setAmount(movement.getAmount());
        savedMovement.setType("PAYMENT");
        savedMovement.setSaleId(null);
        savedMovement.setDescription(
            movement.getDescription() == null
                ? "Pago registrado"
                : movement.getDescription()
        );
        savedMovement.setCreatedAt(
            LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"))
        );

        movementRepository.save(savedMovement);

        customer.setBalance(calculateIndexedBalance(customer));

        return customerRepository.save(customer);
    }

    @GetMapping("/{customerId}/movements")
    public List<CustomerMovement> getMovements(
        @PathVariable Long customerId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow();

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Cliente no autorizado");
        }

        return movementRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    private double calculateIndexedBalance(Customer customer) {
        List<CustomerMovement> movements =
            movementRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId());

        double debt = 0;
        double payments = 0;

        for (CustomerMovement movement : movements) {

            if ("PAYMENT".equals(movement.getType())) {
                payments += movement.getAmount();
            }

            if ("DEBT".equals(movement.getType())) {

                if (movement.getSaleId() == null) {
                    debt += movement.getAmount();
                } else {
                    Sale sale = saleRepository.findById(movement.getSaleId())
                        .orElse(null);

                    if (sale != null) {
                        debt += calculateSaleWithCurrentPrices(sale);
                    }
                }
            }
        }

        return Math.max(debt - payments, 0);
    }

    private double calculateSaleWithCurrentPrices(Sale sale) {
        double subtotal = 0;

        for (SaleItem item : sale.getItems()) {

            if ("PRODUCT".equals(item.getItemType())) {
                Product product = productRepository.findById(item.getProductId())
                    .orElse(null);

                if (product != null) {
                    subtotal += product.getPrice() * item.getQuantity();
                }
            }

            if ("PREPARED".equals(item.getItemType())) {
                PreparedProduct prepared = preparedProductRepository
                    .findById(item.getPreparedProductId())
                    .orElse(null);

                if (prepared != null) {
                    subtotal += prepared.getPrice() * item.getQuantity();
                }
            }
        }

        double discount = sale.getDiscount() == null ? 0 : sale.getDiscount();

        return Math.max(subtotal - discount, 0);
    }

    @PutMapping("/{customerId}")
    public Customer update(
        @PathVariable Long customerId,
        @RequestBody Customer updated,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow();

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Cliente no autorizado");
        }

        if (updated.getName() == null || updated.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre es obligatorio");
        }

        customer.setName(updated.getName().trim());

        return customerRepository.save(customer);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long customerId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow();

        if (!customer.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Cliente no autorizado");
        }

        movementRepository.deleteAll(
            movementRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)
        );

        customerRepository.delete(customer);

        return ResponseEntity.ok().build();
    }
}