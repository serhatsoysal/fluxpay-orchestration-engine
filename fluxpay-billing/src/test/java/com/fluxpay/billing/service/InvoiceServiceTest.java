package com.fluxpay.billing.service;

import com.fluxpay.billing.entity.Invoice;
import com.fluxpay.billing.entity.InvoiceItem;
import com.fluxpay.billing.repository.InvoiceItemRepository;
import com.fluxpay.billing.repository.InvoiceRepository;
import com.fluxpay.common.dto.InvoiceStatsResponse;
import com.fluxpay.common.dto.Period;
import com.fluxpay.common.enums.InvoiceStatus;
import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.common.exception.ValidationException;
import com.fluxpay.product.entity.Price;
import com.fluxpay.product.repository.PriceRepository;
import com.fluxpay.security.context.TenantContext;
import com.fluxpay.subscription.entity.Customer;
import com.fluxpay.subscription.entity.Subscription;
import com.fluxpay.subscription.repository.CustomerRepository;
import com.fluxpay.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private TaxService taxService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice invoice;
    private UUID tenantId;
    private UUID customerId;
    private UUID subscriptionId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        
        customerId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(customerId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setSubtotal(100L);
        invoice.setTotal(100L);
        invoice.setAmountDue(100L);
        invoice.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getInvoiceById_Success() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(invoice.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoice.getId());
    }

    @Test
    void getInvoiceById_ThrowsException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInvoicesByCustomer_Success() {
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(invoices);

        List<Invoice> result = invoiceService.getInvoicesByCustomer(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void createInvoice_ShouldGenerateInvoiceNumber() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isNotNull();
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }

    @Test
    void createInvoiceWithTax_ShouldCalculateTax() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(10000L);
        newInvoice.setTotal(10000L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        InvoiceService service = new InvoiceService(invoiceRepository, invoiceItemRepository, taxService, customerRepository, subscriptionRepository, priceRepository);

        when(taxService.calculateTax(anyLong(), anyString())).thenReturn(java.util.Map.of("taxAmount", 1000L, "taxRate", 0.10));
        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createInvoiceWithTax(newInvoice, items, "US");

        assertThat(result.getTax()).isEqualTo(1000L);
        assertThat(result.getTotal()).isEqualTo(11000L);
        verify(taxService).calculateTax(10000L, "US");
    }

    @Test
    void finalizeInvoice_ShouldSetStatusToOpen() {
        invoice.setStatus(InvoiceStatus.DRAFT);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.finalizeInvoice(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void markInvoiceAsPaid_ShouldUpdateStatusAndAmounts() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.markInvoiceAsPaid(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getAmountPaid()).isEqualTo(invoice.getTotal());
        assertThat(result.getAmountDue()).isZero();
    }

    @Test
    void voidInvoice_ShouldSetStatusToVoid() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.voidInvoice(invoice.getId());

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void getInvoiceItems_ShouldReturnItems() {
        UUID invoiceId = invoice.getId();
        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(invoiceId);

        when(invoiceItemRepository.findByInvoiceId(invoiceId)).thenReturn(Arrays.asList(item));

        List<InvoiceItem> result = invoiceService.getInvoiceItems(invoiceId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getInvoicesBySubscription_ShouldReturnInvoices() {
        UUID testSubscriptionId = UUID.randomUUID();
        invoice.setSubscriptionId(testSubscriptionId);

        when(invoiceRepository.findBySubscriptionId(testSubscriptionId)).thenReturn(Arrays.asList(invoice));

        List<Invoice> result = invoiceService.getInvoicesBySubscription(subscriptionId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void getInvoiceById_ShouldThrowWhenDeleted() {
        invoice.setDeletedAt(java.time.Instant.now());
        UUID invoiceId = invoice.getId();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInvoiceWithTax_WithNullCountryCode_ShouldNotCalculateTax() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(10000L);
        newInvoice.setTotal(10000L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        InvoiceService service = new InvoiceService(invoiceRepository, invoiceItemRepository, taxService, customerRepository, subscriptionRepository, priceRepository);

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createInvoiceWithTax(newInvoice, items, null);

        verify(taxService, never()).calculateTax(anyLong(), any());
        assertThat(result).isNotNull();
    }

    @Test
    void createInvoice_WithExistingInvoiceNumber_ShouldNotGenerateNew() {
        Invoice newInvoice = new Invoice();
        newInvoice.setInvoiceNumber("CUSTOM-001");
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isEqualTo("CUSTOM-001");
        verify(invoiceRepository, never()).findTopByTenantIdOrderByCreatedAtDesc(any());
    }

    @Test
    void createInvoice_WithExistingInvoice_ShouldIncrementInvoiceNumber() {
        Invoice lastInvoice = new Invoice();
        lastInvoice.setInvoiceNumber("INV-000005");

        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(lastInvoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-000006");
    }

    @Test
    void createInvoice_WithInvalidLastInvoiceNumber_ShouldStartFromOne() {
        Invoice lastInvoice = new Invoice();
        lastInvoice.setInvoiceNumber("INVALID");

        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(lastInvoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-000001");
    }

    @Test
    void createInvoice_FirstInvoice_ShouldGenerateINV000001() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-000001");
    }

    @Test
    void createInvoice_ShouldSaveAllInvoiceItems() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(100L);
        newInvoice.setTotal(100L);

        InvoiceItem item1 = new InvoiceItem();
        InvoiceItem item2 = new InvoiceItem();
        InvoiceItem item3 = new InvoiceItem();
        List<InvoiceItem> items = Arrays.asList(item1, item2, item3);

        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.createInvoice(newInvoice, items);

        verify(invoiceItemRepository, times(3)).save(any(InvoiceItem.class));
        assertThat(result).isNotNull();
    }

    @Test
    void createInvoiceWithTax_ShouldSetTaxDetails() {
        Invoice newInvoice = new Invoice();
        newInvoice.setCustomerId(customerId);
        newInvoice.setSubtotal(10000L);
        newInvoice.setTotal(10000L);

        List<InvoiceItem> items = Arrays.asList(new InvoiceItem());

        InvoiceService service = new InvoiceService(invoiceRepository, invoiceItemRepository, taxService, customerRepository, subscriptionRepository, priceRepository);

        java.util.Map<String, Object> taxDetails = new java.util.HashMap<>();
        taxDetails.put("taxAmount", 2000L);
        taxDetails.put("taxRate", 20.0);
        taxDetails.put("taxType", "VAT");
        when(taxService.calculateTax(anyLong(), anyString())).thenReturn(taxDetails);
        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createInvoiceWithTax(newInvoice, items, "US");

        assertThat(result.getTax()).isEqualTo(2000L);
        assertThat(result.getTaxDetails()).isEqualTo(taxDetails);
        assertThat(result.getTotal()).isEqualTo(12000L);
        assertThat(result.getAmountDue()).isEqualTo(12000L);
    }

    @Test
    void createInvoiceWithValidation_ShouldCreateInvoice() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        item.setPriceId(null);
        
        Invoice newInvoice = new Invoice();
        newInvoice.setId(UUID.randomUUID());
        newInvoice.setInvoiceNumber("INV-000001");
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setId(newInvoice.getId());
            inv.setInvoiceNumber(newInvoice.getInvoiceNumber());
            return inv;
        });
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(i -> i.getArgument(0));
        
        Invoice result = invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        );
        
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getInvoiceDate()).isEqualTo(invoiceDate);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(result.getCurrency()).isEqualTo("USD");
        verify(customerRepository).findById(customerId);
    }

    @Test
    void createInvoiceWithValidation_WhenCustomerNotFound_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenSubscriptionNotFound_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, subscriptionId, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenDueDateBeforeInvoiceDate_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().minusDays(1);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Due date must be after invoice date");
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenNoItems_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(), null
        )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invoice must have at least one item");
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenPriceNotFound_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        UUID priceId = UUID.randomUUID();
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        item.setPriceId(priceId);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getInvoiceStatsWithPeriod_ShouldReturnStats() {
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();
        
        when(invoiceRepository.sumAmountDueByTenantId(tenantId)).thenReturn(50000L);
        when(invoiceRepository.sumOverdueAmountByTenantId(eq(tenantId), any())).thenReturn(10000L);
        
        InvoiceStatsResponse result = invoiceService.getInvoiceStatsWithPeriod(dateFrom, dateTo);
        
        assertThat(result).isNotNull();
        assertThat(result.getTotalOutstanding()).isEqualTo(50000L);
        assertThat(result.getPastDue()).isEqualTo(10000L);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getPeriod().getFrom()).isEqualTo(dateFrom);
        assertThat(result.getPeriod().getTo()).isEqualTo(dateTo);
        verify(invoiceRepository).sumAmountDueByTenantId(tenantId);
    }

    @Test
    void getInvoiceStatsWithPeriod_WithNullDates_ShouldUseDefaults() {
        when(invoiceRepository.sumAmountDueByTenantId(tenantId)).thenReturn(50000L);
        when(invoiceRepository.sumOverdueAmountByTenantId(eq(tenantId), any())).thenReturn(10000L);
        
        InvoiceStatsResponse result = invoiceService.getInvoiceStatsWithPeriod(null, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getFrom()).isNotNull();
        assertThat(result.getPeriod().getTo()).isNotNull();
        verify(invoiceRepository).sumAmountDueByTenantId(tenantId);
    }

    @Test
    void createInvoiceWithValidation_WhenDueDateEqualsInvoiceDate_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = invoiceDate;
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Due date must be after invoice date");
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenCustomerDeleted_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(Instant.now());
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenCustomerWrongTenant_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        UUID differentTenantId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(differentTenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenSubscriptionWrongTenant_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        UUID differentTenantId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        com.fluxpay.subscription.entity.Subscription subscription = new com.fluxpay.subscription.entity.Subscription();
        subscription.setId(subscriptionId);
        subscription.setTenantId(differentTenantId);
        subscription.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, subscriptionId, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenSubscriptionDeleted_ShouldThrowException() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        com.fluxpay.subscription.entity.Subscription subscription = new com.fluxpay.subscription.entity.Subscription();
        subscription.setId(subscriptionId);
        subscription.setTenantId(tenantId);
        subscription.setDeletedAt(Instant.now());
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithValidation(
                customerId, subscriptionId, invoiceDate, dueDate, "USD", List.of(item), null
        )).isInstanceOf(ResourceNotFoundException.class);
        
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceWithValidation_WhenItemHasNullPriceId_ShouldNotValidatePrice() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(14);
        
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setDeletedAt(null);
        
        InvoiceItem item = new InvoiceItem();
        item.setDescription("Test item");
        item.setAmount(10000L);
        item.setPriceId(null);
        
        Invoice newInvoice = new Invoice();
        newInvoice.setId(UUID.randomUUID());
        newInvoice.setInvoiceNumber("INV-000001");
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findTopByTenantIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setId(newInvoice.getId());
            inv.setInvoiceNumber(newInvoice.getInvoiceNumber());
            return inv;
        });
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(i -> i.getArgument(0));
        
        Invoice result = invoiceService.createInvoiceWithValidation(
                customerId, null, invoiceDate, dueDate, "USD", List.of(item), null
        );
        
        assertThat(result).isNotNull();
        verify(priceRepository, never()).findById(any());
    }

    @Test
    void getInvoiceStatsWithPeriod_WithOnlyDateFrom_ShouldUseDefaultDateTo() {
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        
        when(invoiceRepository.sumAmountDueByTenantId(tenantId)).thenReturn(50000L);
        when(invoiceRepository.sumOverdueAmountByTenantId(eq(tenantId), any())).thenReturn(10000L);
        
        InvoiceStatsResponse result = invoiceService.getInvoiceStatsWithPeriod(dateFrom, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getFrom()).isEqualTo(dateFrom);
        assertThat(result.getPeriod().getTo()).isNotNull();
    }

    @Test
    void getInvoiceStatsWithPeriod_WithOnlyDateTo_ShouldUseDefaultDateFrom() {
        LocalDate dateTo = LocalDate.now();
        
        when(invoiceRepository.sumAmountDueByTenantId(tenantId)).thenReturn(50000L);
        when(invoiceRepository.sumOverdueAmountByTenantId(eq(tenantId), any())).thenReturn(10000L);
        
        InvoiceStatsResponse result = invoiceService.getInvoiceStatsWithPeriod(null, dateTo);
        
        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getFrom()).isNotNull();
        assertThat(result.getPeriod().getTo()).isEqualTo(dateTo);
    }

    @Test
    void createInvoice_WhenInvoiceIsNull_ShouldThrowException() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(10000L);
        
        assertThatThrownBy(() -> invoiceService.createInvoice(null, List.of(item)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void createInvoice_WhenItemsIsNull_ShouldThrowException() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setSubtotal(10000L);
        
        assertThatThrownBy(() -> invoiceService.createInvoice(invoice, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must have at least one item");
    }

    @Test
    void createInvoice_WhenItemsIsEmpty_ShouldThrowException() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setSubtotal(10000L);
        
        assertThatThrownBy(() -> invoiceService.createInvoice(invoice, List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must have at least one item");
    }

    @Test
    void createInvoiceWithTax_WhenInvoiceIsNull_ShouldThrowException() {
        InvoiceItem item = new InvoiceItem();
        item.setAmount(10000L);
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithTax(null, List.of(item), "US"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void createInvoiceWithTax_WhenItemsIsNull_ShouldThrowException() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setSubtotal(10000L);
        
        assertThatThrownBy(() -> invoiceService.createInvoiceWithTax(invoice, null, "US"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must have at least one item");
    }

    @Test
    void finalizeInvoice_WhenNotDraft_ShouldThrowException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setDeletedAt(null);
        
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        
        assertThatThrownBy(() -> invoiceService.finalizeInvoice(invoiceId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only DRAFT invoices can be finalized");
    }

    @Test
    void voidInvoice_WhenPaid_ShouldThrowException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setDeletedAt(null);
        
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        
        assertThatThrownBy(() -> invoiceService.voidInvoice(invoiceId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Paid invoices cannot be voided");
    }

    @Test
    void markInvoiceAsPaid_WhenTotalIsNull_ShouldSetZero() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setTotal(null);
        invoice.setDeletedAt(null);
        
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        
        Invoice result = invoiceService.markInvoiceAsPaid(invoiceId);
        
        assertThat(result.getAmountPaid()).isZero();
        assertThat(result.getAmountDue()).isZero();
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }
}

