// src/main/java/com/example/ordersystem2/controller/InventoryController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.dto.InventoryCreateDTO;
import com.example.ordersystem2.dto.InventoryEditDTO;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.service.AccessControlService;
import com.example.ordersystem2.service.InventoryService;
import com.example.ordersystem2.service.OrderTypeService;
import com.example.ordersystem2.service.StockTransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderTypeService orderTypeService;

    @Autowired
    private StockTransactionService stockTransactionService;

    @Autowired
    private AccessControlService accessControlService;

    /**
     * 재고 확인 페이지를 표시합니다.
     * ACTIVE 상태의 재고만 조회합니다.
     */
    @GetMapping
    public String showInventory(HttpSession session, Model model) {
        if (!accessControlService.isLoggedIn(session)) {
            return accessControlService.redirectFor(session);
        }

        List<Inventory> inventories = inventoryService.getInventoriesByStatus(InventoryStatus.ACTIVE);
        model.addAttribute("inventories", inventories);
        model.addAttribute("isHeadquarters", accessControlService.isHeadquarters(session));
        return "inventory";
    }

    /**
     * 재고 추가 페이지를 표시합니다.
     */
    @GetMapping("/add")
    public String showAddInventoryForm(HttpSession session, Model model) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        model.addAttribute("inventoryCreateDTO", new InventoryCreateDTO());
        List<OrderType> orderTypes = orderTypeService.getAllOrderTypes();
        model.addAttribute("orderTypes", orderTypes);
        return "inventory_add";
    }

    /**
     * 재고를 추가하거나 업데이트합니다.
     */
    @PostMapping("/add")
    public String addInventory(@Valid @ModelAttribute("inventoryCreateDTO") InventoryCreateDTO inventoryCreateDTO,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        if (bindingResult.hasErrors()) {
            List<OrderType> orderTypes = orderTypeService.getAllOrderTypes();
            model.addAttribute("orderTypes", orderTypes);
            return "inventory_add";
        }

        try {
            inventoryService.addOrUpdateInventory(inventoryCreateDTO);
            redirectAttributes.addFlashAttribute("success", "재고가 성공적으로 추가 또는 업데이트되었습니다.");
            return "redirect:/inventory";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "재고 추가 중 오류가 발생했습니다: " + e.getMessage());
            List<OrderType> orderTypes = orderTypeService.getAllOrderTypes();
            model.addAttribute("orderTypes", orderTypes);
            return "inventory_add";
        }
    }

    /**
     * 재고 수정 페이지를 표시합니다.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        try {
            Inventory inventory = inventoryService.getInventoryById(id);
            InventoryEditDTO inventoryEditDTO = new InventoryEditDTO();
            inventoryEditDTO.setOrderTypeId(inventory.getOrderType().getId());
            inventoryEditDTO.setQuantity(inventory.getQuantity());
            model.addAttribute("inventoryEditDTO", inventoryEditDTO);
            model.addAttribute("inventory", inventory);
            model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
            return "inventory_edit";
        } catch (InventoryNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error"; // 오류 페이지로 이동
        }
    }

    /**
     * 재고를 수정합니다.
     */
    @PostMapping("/edit/{id}")
    public String updateInventory(@PathVariable("id") Long id,
                                  @Valid @ModelAttribute("inventoryEditDTO") InventoryEditDTO inventoryEditDTO,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("inventory", inventoryService.getInventoryById(id));
            } catch (InventoryNotFoundException e) {
                model.addAttribute("errorMessage", e.getMessage());
                return "error";
            }
            model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
            return "inventory_edit";
        }

        try {
            inventoryService.updateInventory(id, inventoryEditDTO);
            redirectAttributes.addFlashAttribute("success", "재고가 성공적으로 수정되었습니다.");
            return "redirect:/inventory";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "재고 수정 중 오류가 발생했습니다: " + e.getMessage());
            try {
                model.addAttribute("inventory", inventoryService.getInventoryById(id));
            } catch (InventoryNotFoundException ignored) {
                return "error";
            }
            model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
            return "inventory_edit";
        }
    }

    /**
     * 폐기처리 페이지를 표시합니다.
     */
    @GetMapping("/disposed")
    public String showDisposedInventories(HttpSession session, Model model) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        List<StockTransaction> disposedTransactions = inventoryService.getDisposedTransactions();
        model.addAttribute("disposedTransactions", disposedTransactions);
        return "inventory_dispose"; // templates/inventory_dispose.html
    }

    /**
     * 폐기 버튼을 눌러 발주를 폐기 처리합니다.
     */
    @PostMapping("/dispose/{id}")
    public String disposeInventory(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        try {
            // StockTransactionService를 통해 트랜잭션 조회
            StockTransaction transaction = stockTransactionService.getTransactionById(id);
            inventoryService.disposeInventory(transaction);
            redirectAttributes.addFlashAttribute("success", "발주가 성공적으로 폐기되었습니다.");
        } catch (InventoryNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "폐기 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/inventory/disposed";
    }

    // /delete/{id} 엔드포인트 제거
}
