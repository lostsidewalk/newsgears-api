package com.lostsidewalk.buffy.app.model;

import lombok.Data;

@Data
public class CheckoutItem {
    private int  quantity;
    private double price;
    private long productId;
    private int userId;
}
