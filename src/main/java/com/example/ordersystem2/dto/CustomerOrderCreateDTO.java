package com.example.ordersystem2.dto;

import com.example.ordersystem2.model.CustomerFulfillmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CustomerOrderCreateDTO {

    @NotNull(message = "품목을 선택해주세요.")
    private Long orderTypeId;

    @NotNull(message = "수량을 입력해주세요.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 80, message = "이름은 80자 이하로 입력해주세요.")
    private String customerName;

    @NotBlank(message = "연락처를 입력해주세요.")
    @Size(max = 40, message = "연락처는 40자 이하로 입력해주세요.")
    private String customerPhone;

    @NotNull(message = "수령 방식을 선택해주세요.")
    private CustomerFulfillmentType fulfillmentType;

    @Size(max = 240, message = "주소는 240자 이하로 입력해주세요.")
    private String deliveryAddress;

    @Size(max = 240, message = "요청사항은 240자 이하로 입력해주세요.")
    private String requestMemo;

    public Long getOrderTypeId() {
        return orderTypeId;
    }

    public void setOrderTypeId(Long orderTypeId) {
        this.orderTypeId = orderTypeId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public CustomerFulfillmentType getFulfillmentType() {
        return fulfillmentType;
    }

    public void setFulfillmentType(CustomerFulfillmentType fulfillmentType) {
        this.fulfillmentType = fulfillmentType;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getRequestMemo() {
        return requestMemo;
    }

    public void setRequestMemo(String requestMemo) {
        this.requestMemo = requestMemo;
    }
}
