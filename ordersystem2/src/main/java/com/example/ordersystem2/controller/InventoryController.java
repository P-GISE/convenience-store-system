// src/main/java/com/example/ordersystem2/controller/InventoryController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.dto.InventoryCreateDTO;
import com.example.ordersystem2.dto.InventoryEditDTO;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.service.InventoryService;
import com.example.ordersystem2.service.OrderTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderTypeService orderTypeService;

    /**
     * 재고 확인 페이지를 표시합니다.
     */
    @GetMapping
    public String showInventory(Model model) {
        List<Inventory> inventories = inventoryService.getAllInventories();
        model.addAttribute("inventories", inventories);
        return "inventory";
    }

    /**
     * 재고 추가 페이지를 표시합니다.
     */
    @GetMapping("/add")
    public String showAddInventoryForm(Model model) {
        model.addAttribute("inventoryCreateDTO", new InventoryCreateDTO());
        List<OrderType> orderTypes = orderTypeService.getAllOrderTypes();
        model.addAttribute("orderTypes", orderTypes);
        return "inventory_add";
    }

    /**
     * 재고를 추가합니다.
     */
    @PostMapping("/add")
    public String addInventory(@Valid @ModelAttribute("inventoryCreateDTO") InventoryCreateDTO inventoryCreateDTO,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            List<OrderType> orderTypes = orderTypeService.getAllOrderTypes();
            model.addAttribute("orderTypes", orderTypes);
            return "inventory_add";
        }

        try {
            inventoryService.addInventory(inventoryCreateDTO);
            return "redirect:/inventory?success";
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
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        try {
            Inventory inventory = inventoryService.getInventoryById(id);
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
                                  @Valid InventoryEditDTO inventoryEditDTO,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
            return "inventory_edit";
        }

        try {
            inventoryService.updateInventory(id, inventoryEditDTO);
            return "redirect:/inventory?success";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "재고 수정 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
            return "inventory_edit";
        }
    }

    /**
     * 재고를 삭제합니다.
     */
    @GetMapping("/delete/{id}")
    public String deleteInventory(@PathVariable("id") Long id, Model model) {
        try {
            inventoryService.deleteInventory(id);
            return "redirect:/inventory?success";
        } catch (InventoryNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error"; // 오류 페이지로 이동
        } catch (Exception e) {
            model.addAttribute("errorMessage", "재고 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "error"; // 오류 페이지로 이동
        }
    }
}
