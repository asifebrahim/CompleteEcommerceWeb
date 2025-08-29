package com.example.EcommerceFresh.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public Order createOrder(Double amount) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_" + System.currentTimeMillis());
        
        return razorpayClient.orders.create(orderRequest);
    }

    public boolean verifyPayment(String paymentId, String orderId, String signature) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
            
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            
            return com.razorpay.Utils.verifyPaymentSignature(options, keySecret);
        } catch (Exception e) {
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
