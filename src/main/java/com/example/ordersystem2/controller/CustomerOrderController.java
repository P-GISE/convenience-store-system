package com.example.ordersystem2.controller;

import com.example.ordersystem2.dto.CustomerOrderCreateDTO;
import com.example.ordersystem2.exception.InsufficientInventoryException;
import com.example.ordersystem2.model.CustomerFulfillmentType;
import com.example.ordersystem2.model.CustomerOrderStatus;
import com.example.ordersystem2.service.AccessControlService;
import com.example.ordersystem2.service.CustomerOrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final AccessControlService accessControlService;

    public CustomerOrderController(CustomerOrderService customerOrderService,
                                   AccessControlService accessControlService) {
        this.customerOrderService = customerOrderService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/shop")
    public String showShop(@RequestParam(value = "phone", required = false) String phone, Model model) {
        model.addAttribute("catalogItems", customerOrderService.getAvailableCatalogItems());
        model.addAttribute("customerOrderCreate", new CustomerOrderCreateDTO());
        model.addAttribute("fulfillmentTypes", CustomerFulfillmentType.values());
        model.addAttribute("lookupPhone", phone);
        model.addAttribute("lookupOrders", customerOrderService.getOrdersByPhone(phone));
        return "customer_shop";
    }

    @PostMapping("/shop")
    public String createCustomerOrder(@Valid @ModelAttribute("customerOrderCreate") CustomerOrderCreateDTO request,
                                      BindingResult bindingResult,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            prepareShopModel(model);
            return "customer_shop";
        }

        try {
            customerOrderService.createCustomerOrder(request);
            return "redirect:/shop?success=created";
        } catch (InsufficientInventoryException | IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            prepareShopModel(model);
            return "customer_shop";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "주문 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            prepareShopModel(model);
            return "customer_shop";
        }
    }

    @GetMapping("/customer-orders")
    public String showCustomerOrders(HttpSession session, Model model) {
        if (!accessControlService.isStoreOwner(session)) {
            return accessControlService.redirectFor(session);
        }

        model.addAttribute("customerOrders", customerOrderService.getAllOrders());
        model.addAttribute("statuses", CustomerOrderStatus.values());
        return "customer_orders";
    }

    @PostMapping("/customer-orders/{id}/status")
    public String updateCustomerOrderStatus(@PathVariable("id") Long id,
                                            @RequestParam("status") CustomerOrderStatus status,
                                            HttpSession session) {
        if (!accessControlService.isStoreOwner(session)) {
            return accessControlService.redirectFor(session);
        }

        try {
            customerOrderService.updateStatus(id, status);
            return "redirect:/customer-orders?success=status_updated";
        } catch (IllegalStateException e) {
            return "redirect:/customer-orders?error=invalid_status_transition";
        } catch (Exception e) {
            return "redirect:/customer-orders?error=unexpected_error";
        }
    }

    private void prepareShopModel(Model model) {
        model.addAttribute("catalogItems", customerOrderService.getAvailableCatalogItems());
        model.addAttribute("fulfillmentTypes", CustomerFulfillmentType.values());
        model.addAttribute("lookupOrders", customerOrderService.getOrdersByPhone(null));
    }
}
